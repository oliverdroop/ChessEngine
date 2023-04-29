package chess.api.controller;

import chess.api.FENReader;
import chess.api.FENWriter;
import chess.api.PieceConfiguration;
import chess.api.PositionEvaluator;
import chess.api.dto.FENRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
            LOGGER.warn(errorMessage);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
}
