package com.oliverdroop.chess.api.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliverdroop.chess.api.*;
import com.oliverdroop.chess.api.configuration.LongsPieceConfiguration;
import com.oliverdroop.chess.api.configuration.PieceConfiguration;
import com.oliverdroop.chess.api.dto.AvailableMovesRequestDto;
import com.oliverdroop.chess.api.dto.AiMoveRequestDto;
import com.oliverdroop.chess.api.dto.AiMoveResponseDto;
import com.oliverdroop.chess.api.pieces.Piece;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.oliverdroop.chess.api.ai.openings.OpeningBook.getOpeningResponse;
import static com.oliverdroop.chess.api.ai.DepthFirstPositionEvaluator.deriveGameEndType;
import static com.oliverdroop.chess.api.ai.ConcurrentPositionEvaluator.getBestMoveRecursively;

@Service
@Validated
public class FENController implements RequestHandler<APIGatewayV2HTTPEvent, AiMoveResponseDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FENController.class);

    private static final Class<? extends PieceConfiguration> CONFIGURATION_CLASS = LongsPieceConfiguration.class;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public AiMoveResponseDto handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        LOGGER.info("Handling AWS Lambda request: {}", request);
        try {
            AiMoveRequestDto aiMoveRequestDto = OBJECT_MAPPER.readValue(request.getBody(), AiMoveRequestDto.class);
            return getAiMove(aiMoveRequestDto).getBody();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to process body as AiMoveRequestDto");
        }
    }

//    @PostMapping("/chess")
    public ResponseEntity<AiMoveResponseDto> getAiMove(AiMoveRequestDto aiMoveRequestDto) {
        LOGGER.info("Received AI move request: {}", aiMoveRequestDto);
        final AiMoveResponseDto response = new AiMoveResponseDto();
        try {
            LOGGER.debug("FEN: {}", aiMoveRequestDto.getFen());
            LOGGER.debug("Depth: {}", aiMoveRequestDto.getDepth());
            final PieceConfiguration inputConfiguration = getInputConfiguration(aiMoveRequestDto);

            PieceConfiguration outputConfiguration = getOpeningResponse(inputConfiguration);
            if (outputConfiguration == null) {
                outputConfiguration = getBestMoveRecursively(inputConfiguration, aiMoveRequestDto.getDepth());
            } else {
                Thread.sleep(250); // Wait a bit to simulate some thinking time
            }

            if (outputConfiguration != null) {
                final String outputFEN = FENWriter.write(outputConfiguration);
                if (outputConfiguration.getOnwardConfigurations().isEmpty() || outputConfiguration.isDraw()) {
                    setAndLogGameEnd(response, outputConfiguration);
                }
                response.setFen(outputFEN);
                response.setAlgebraicNotation(outputConfiguration.getAlgebraicNotation(inputConfiguration));
                response.setCheck(outputConfiguration.isCheck());
                LOGGER.info("Response FEN: {}", outputFEN);
            } else {
                setAndLogGameEnd(response, inputConfiguration);
            }
            return ResponseEntity.ok().body(response);
        } catch (Throwable e) {
            final String errorMessage = String.format("Unable to get AI move: %s", e);
            LOGGER.error(errorMessage, e);
            response.setError(errorMessage);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/chess/available-moves")
    public ResponseEntity<List<String>> getAvailableMoves(@RequestBody @Valid AvailableMovesRequestDto availableMovesRequestDto) {
        LOGGER.debug("Received available moves request: {}", availableMovesRequestDto);
        try {
            LOGGER.debug("FEN: {}", availableMovesRequestDto.getFen());
            LOGGER.debug("From: {}", availableMovesRequestDto.getFrom());
            final PieceConfiguration inputConfiguration = FENReader.read(
                availableMovesRequestDto.getFen(), CONFIGURATION_CLASS);
            final int pieceBitFlag = inputConfiguration.getPieceAtPosition(Position.getPosition(availableMovesRequestDto.getFrom()));
            LOGGER.info("Getting available moves for the {} {} at {}",
                    Side.values()[Piece.getSide(pieceBitFlag)], Piece.getPieceType(pieceBitFlag),
                    Position.getCoordinateString(Position.getPosition(pieceBitFlag)));

            final List<String> algebraicNotations = inputConfiguration.getOnwardConfigurationsForPiece(pieceBitFlag)
                    .stream()
                    .map(pc -> pc.getAlgebraicNotation(inputConfiguration))
                    .collect(Collectors.toList());
            LOGGER.info("Response: {}", algebraicNotations);
            return ResponseEntity.ok(algebraicNotations);
        } catch (IllegalArgumentException e) {
            final String errorMessage = String.format("Unable to get available moves from %s: No piece at position", availableMovesRequestDto.getFrom());
            LOGGER.warn(errorMessage);
            return ResponseEntity.noContent().build();
        } catch (Throwable e) {
            final String errorMessage = String.format("Unable to get available moves: %s", e.getMessage());
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(errorMessage));
        }
    }

    private PieceConfiguration getInputConfiguration(AiMoveRequestDto aiMoveRequestDto) {
        final PieceConfiguration inputConfiguration = FENReader.read(aiMoveRequestDto.getFen(), CONFIGURATION_CLASS);
        final List<String> moveHistory = aiMoveRequestDto.getMoveHistory();
        if (moveHistory != null) {
            final List<Short> historicMoveList = moveHistory.stream().map(MoveDescriber::getMoveFromAlgebraicNotation).toList();
            short[] historicMoves = new short[historicMoveList.size()];
            for(int index = 0; index < historicMoves.length; index++) {
                historicMoves[index] = historicMoveList.get(index);
            }
            inputConfiguration.setHistoricMoves(historicMoves);
        }
        return inputConfiguration;
    }

    private void setAndLogGameEnd(AiMoveResponseDto response, PieceConfiguration pieceConfiguration) {
        final GameEndType gameEndType = deriveGameEndType(pieceConfiguration);
        response.setGameResult(gameEndType.toString());
        LOGGER.info("{}: {}", gameEndType, pieceConfiguration);
    }
}
