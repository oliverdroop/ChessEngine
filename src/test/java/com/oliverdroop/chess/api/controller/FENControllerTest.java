package com.oliverdroop.chess.api.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.oliverdroop.chess.api.FENWriter;
import com.oliverdroop.chess.api.dto.AiMoveRequestDto;
import com.oliverdroop.chess.api.dto.AiMoveResponseDto;
import com.oliverdroop.chess.api.dto.AvailableMovesRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.oliverdroop.chess.api.GameEndType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FENControllerTest {

//    @Autowired private FENController fenController;
//
//    @Test
//    void testGetAiMove() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest(
//                FENWriter.STARTING_POSITION, 3),
//            mock(Context.class));
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.OK);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getFen)
//            .isNotNull();
//    }
//
//    @Test
//    void testGetAiMove_withNonsenseFEN() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest("nonsense", 3),
//            mock(Context.class));
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getError)
//            .isEqualTo("You must supply valid Forsyth-Edwards Notation in the 'fen' field");
//    }
//
//    @Test
//    void testGetAiMove_withValidMoveHistory() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", List.of("e2e4")),
//            mock(Context.class));
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getError)
//            .isEqualTo("You must supply valid Forsyth-Edwards Notation in the 'fen' field");
//    }
//
//    @Test
//    void testGetAiMove_withNonsenseMoveHistory() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest(FENWriter.STARTING_POSITION, List.of("e2e4", "nonsense")),
//            mock(Context.class));
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getError)
//            .isEqualTo("moveHistory is invalid");
//    }
//
//    @Test
//    void testGetAiMove_withInaccurateMoveHistory() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest(FENWriter.STARTING_POSITION, List.of("e2e4", "e7e5")),
//            mock(Context.class));
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getError)
//            .isEqualTo("Move history does not result in FEN");
//    }
//
//    @Test
//    void testGetAiMove_withEnPassant() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest(
//                "rn4nr/p1k1bp2/1pp1p2P/3pP3/3P1B2/2P3NP/PQ5P/1N2KB1R b K - 0 15",
//                List.of("e2e4","c7c6","f2f4","d8b6","c2c3","d7d5","e4e5","g7g5","f4xg5","c8h3","g2xh3","e7e6","d2d4","e8d7","g1e2","d7c7","c1f4","b6xb2","b1d2","b2xa1","d1xa1","f8a3","d2b1","a3e7","a1b2","b7b6","e2g3","h7h5","g5xh6")),
//            mock(Context.class));
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.OK);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getFen)
//            .isNotNull();
//    }
//
//    @Test
//    void testGetAiMove_withAiVictory() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest("k7/2P5/K7/8/8/8/8/8 w - - 0 50", 3),
//            mock(Context.class)
//        );
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.OK);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getGameResult)
//            .isEqualTo(WHITE_VICTORY.toString());
//    }
//
//    @Test
//    void testGetAiMove_withCallerVictory() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest("k1R5/8/K7/8/8/8/8/8 b - - 0 50", 3),
//            mock(Context.class)
//        );
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.OK);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getGameResult)
//            .isEqualTo(WHITE_VICTORY.toString());
//    }
//
//    @Test
//    void testGetAiMove_withAiDraw() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest("7K/7P/8/8/8/8/8/k7 w - - 99 50", 3),
//            mock(Context.class)
//        );
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.OK);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getGameResult)
//            .isEqualTo(DRAW.toString());
//    }
//
//    @Test
//    void testGetAiMove_withAiStalemate() {
//        ResponseEntity<AiMoveResponseDto> response = fenController.handleRequest(
//            buildAiMoveRequest("2Q5/kB1N4/8/8/8/8/8/KR6 b - - 0 50", 3),
//            mock(Context.class)
//        );
//        assertThat(response)
//            .extracting(ResponseEntity::getStatusCode)
//            .isEqualTo(HttpStatus.OK);
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getFen)
//            .isNull();
//        assertThat(response)
//            .extracting(ResponseEntity::getBody)
//            .extracting(AiMoveResponseDto::getGameResult)
//            .isEqualTo(STALEMATE.toString());
//    }
//
////    @Test
////    void testGetAvailableMoves() throws Exception {
////        mockMvc.perform(
////                post(AVAILABLE_MOVES_ENDPOINT)
////                    .contentType(MediaType.APPLICATION_JSON)
////                    .content(OBJECT_MAPPER.writeValueAsString(
////                        buildAvailableMovesRequest(FENWriter.STARTING_POSITION, "e2"))))
////            .andExpect(status().isOk())
////            .andExpect(content().string("[\"e2e3\",\"e2e4\"]"));
////    }
////
////    @Test
////    void testGetAvailableMoves_fromEmptySquare() throws Exception {
////        mockMvc.perform(
////                post(AVAILABLE_MOVES_ENDPOINT)
////                    .contentType(MediaType.APPLICATION_JSON)
////                    .content(OBJECT_MAPPER.writeValueAsString(
////                        buildAvailableMovesRequest(FENWriter.STARTING_POSITION, "e3"))))
////            .andExpect(status().isNoContent());
////    }
//
//    private AiMoveRequestDto buildAiMoveRequest(String fen, int depth) {
//        final AiMoveRequestDto request = new AiMoveRequestDto();
//        request.setFen(fen);
//        request.setDepth(depth);
//        return request;
//    }
//
//    private AiMoveRequestDto buildAiMoveRequest(String fen, List<String> moveHistory) {
//        final AiMoveRequestDto request = buildAiMoveRequest(fen, 3);
//        request.setMoveHistory(moveHistory);
//        return request;
//    }
//
//    private AvailableMovesRequestDto buildAvailableMovesRequest(String fen, String fromSquare) {
//        final AvailableMovesRequestDto request = new AvailableMovesRequestDto();
//        request.setFen(fen);
//        request.setFrom(fromSquare);
//        return request;
//    }
}
