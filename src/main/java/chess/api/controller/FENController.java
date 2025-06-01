package chess.api.controller;

import chess.api.*;
import chess.api.dto.AvailableMovesRequestDto;
import chess.api.dto.AiMoveRequestDto;
import chess.api.dto.AiMoveResponseDto;
import chess.api.pieces.Piece;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static chess.api.ai.openings.OpeningBook.getOpeningResponse;
import static chess.api.ai.PositionEvaluator.deriveGameEndType;
import static chess.api.ai.ConcurrentPositionEvaluator.getBestMoveRecursively;

@RestController
public class FENController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FENController.class);

    @PostMapping("/chess")
    public ResponseEntity<AiMoveResponseDto> getAiMove(@RequestBody @Valid AiMoveRequestDto aiMoveRequestDto) {
        LOGGER.info("Received AI move request: {}", aiMoveRequestDto);
        final AiMoveResponseDto response = new AiMoveResponseDto();
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            LOGGER.debug("FEN: {}", aiMoveRequestDto.getFen());
            LOGGER.debug("Depth: {}", aiMoveRequestDto.getDepth());
            final PieceConfiguration inputConfiguration = FENReader.read(aiMoveRequestDto.getFen());

            PieceConfiguration outputConfiguration = getOpeningResponse(inputConfiguration);
            if (outputConfiguration == null) {
                outputConfiguration = getBestMoveRecursively(inputConfiguration, aiMoveRequestDto.getDepth());
            } else {
                Thread.sleep(500); // Wait a bit to simulate some thinking time
            }

            if (outputConfiguration != null) {
                String outputFEN = FENWriter.write(outputConfiguration);
                if (FENReader.read(outputFEN).getPossiblePieceConfigurations().isEmpty()) {
                    response.setGameResult(deriveGameEndType(outputConfiguration).toString());
                }
                response.setFen(outputFEN);
                response.setAlgebraicNotation(outputConfiguration.getAlgebraicNotation(inputConfiguration));
                response.setCheck(outputConfiguration.isCheck());
            } else {
                response.setGameResult(deriveGameEndType(inputConfiguration).toString());
            }
            LOGGER.info("Response: {}", objectMapper.writeValueAsString(response));
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
        LOGGER.info("Received available moves request: {}", availableMovesRequestDto);
        try {
            LOGGER.debug("FEN: {}", availableMovesRequestDto.getFen());
            LOGGER.debug("From: {}", availableMovesRequestDto.getFrom());
            final PieceConfiguration inputConfiguration = FENReader.read(availableMovesRequestDto.getFen());
            final int pieceBitFlag = inputConfiguration.getPieceAtPosition(Position.getPosition(availableMovesRequestDto.getFrom()));
            LOGGER.info("Getting available moves for the {} {} at {}",
                    Side.values()[Piece.getSide(pieceBitFlag)], Piece.getPieceType(pieceBitFlag),
                    Position.getCoordinateString(Position.getPosition(pieceBitFlag)));

            final List<String> algebraicNotations = inputConfiguration.getPossiblePieceConfigurationsForPiece(pieceBitFlag)
                    .stream()
                    .map(pc -> pc.getAlgebraicNotation(inputConfiguration))
                    .collect(Collectors.toList());
            LOGGER.info("Response: {}", algebraicNotations);
            return ResponseEntity.ok(algebraicNotations);
        } catch (Throwable e) {
            final String errorMessage = String.format("Unable to get available moves: %s", e.getMessage());
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(errorMessage));
        }
    }
}
