package chess.api.controller;

import chess.api.*;
import chess.api.dto.AvailableMovesRequestDto;
import chess.api.dto.FENRequestDto;
import chess.api.pieces.Piece;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
public class FENController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FENController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chess")
    public ResponseEntity<String> getFENResponse(@RequestBody String body) {
        try {
            LOGGER.info("Received: {}", body);
            FENRequestDto fenRequestDto = objectMapper.readValue(body, FENRequestDto.class);
            LOGGER.info("FEN: {}", fenRequestDto.getFen());
            LOGGER.info("Depth: {}", fenRequestDto.getDepth());
            PieceConfiguration inputConfiguration = FENReader.read(fenRequestDto.getFen());

            PieceConfiguration outputConfiguration = PositionEvaluator.getBestMoveRecursively(inputConfiguration, fenRequestDto.getDepth());
            String outputFEN = FENWriter.write(outputConfiguration);
            LOGGER.info("Response: {}", outputFEN);
            return ResponseEntity.ok().body(FENWriter.write(outputConfiguration));
        } catch (Exception e) {
            String errorMessage = String.format("Unable to process FEN: %s", e);
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    @PostMapping("/chess/available-moves")
    public ResponseEntity<List<String>> getAvailableMoves(@RequestBody String body) {
        try {
            LOGGER.info("Received: {}", body);
            AvailableMovesRequestDto availableMovesRequestDto = objectMapper.readValue(body, AvailableMovesRequestDto.class);
            LOGGER.info("FEN: {}", availableMovesRequestDto.getFen());
            LOGGER.info("From: {}", availableMovesRequestDto.getFrom());
            PieceConfiguration inputConfiguration = FENReader.read(availableMovesRequestDto.getFen());
            int pieceBitFlag = inputConfiguration.getPieceAtPosition(Position.getPosition(availableMovesRequestDto.getFrom()));
            LOGGER.info("The piece for which to get available moves is a {} {}", Piece.getSide(pieceBitFlag), Piece.getPieceType(pieceBitFlag));
            List<String> algebraicNotations = inputConfiguration.getPossiblePieceConfigurationsForPiece(pieceBitFlag, true)
                    .stream()
                    .map(PieceConfiguration::getAlgebraicNotation)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(algebraicNotations);
        } catch (Exception e) {
            String errorMessage = String.format("Unable to get available moves: %s", e.getMessage());
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(errorMessage));
        }
    }
}
