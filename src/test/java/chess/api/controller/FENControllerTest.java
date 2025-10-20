package chess.api.controller;

import chess.api.FENWriter;
import chess.api.dto.AiMoveRequestDto;
import chess.api.dto.AvailableMovesRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static chess.api.GameEndType.STALEMATE;
import static chess.api.GameEndType.WHITE_VICTORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class FENControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AI_MOVE_ENDPOINT = "/chess";
    private static final String AVAILABLE_MOVES_ENDPOINT = "/chess/available-moves";

    @Autowired private MockMvc mockMvc;

    @Test
    void testGetAiMove() throws Exception {
        mockMvc.perform(
            post(AI_MOVE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(FENWriter.STARTING_POSITION)))
        ).andExpect(status().is2xxSuccessful());
    }

    @Test
    void testGetAiMove_withNonsenseFEN() throws Exception {
        var resolvedException = mockMvc.perform(
            post(AI_MOVE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest("This is not FEN"))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResolvedException();

        assertThat(resolvedException).isNotNull();
        assertThat(resolvedException.getMessage()).contains("You must supply valid Forsyth-Edwards Notation in the 'fen' field");
    }

    @Test
    void testGetAiMove_withValidMoveHistory() throws Exception {
        mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", List.of("e2e4")))))
            .andExpect(status().isOk());
    }

    @Test
    void testGetAiMove_withNonsenseMoveHistory() throws Exception {
        var resolvedException = mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(FENWriter.STARTING_POSITION, List.of("e2e4", "nonsense")))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResolvedException();

        assertThat(resolvedException).isNotNull();
        assertThat(resolvedException.getMessage()).contains("moveHistory is invalid");
    }

    @Test
    void testGetAiMove_withInaccurateMoveHistory() throws Exception {
        var resolvedException = mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(FENWriter.STARTING_POSITION, List.of("e2e4", "e7e5")))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResolvedException();

        assertThat(resolvedException).isNotNull();
        assertThat(resolvedException.getMessage()).contains("Move history does not result in FEN");
    }

    @Test
    void testGetAiMove_withEnPassant() throws Exception {
        String fen = "rn4nr/p1k1bp2/1pp1p2P/3pP3/3P1B2/2P3NP/PQ5P/1N2KB1R b K - 0 15";
        List<String> moveHistory = List.of("e2e4","c7c6","f2f4","d8b6","c2c3","d7d5","e4e5","g7g5","f4xg5","c8h3","g2xh3","e7e6","d2d4","e8d7","g1e2","d7c7","c1f4","b6xb2","b1d2","b2xa1","d1xa1","f8a3","d2b1","a3e7","a1b2","b7b6","e2g3","h7h5","g5xh6");
        mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(fen, moveHistory))))
            .andExpect(status().isOk());
    }

    @Test
    void testGetAiMove_withAiVictory() throws Exception {
        String fen = "k7/2P5/K7/8/8/8/8/8 w - - 0 50";
        mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(fen))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(WHITE_VICTORY.toString())));
    }

    @Test
    void testGetAiMove_withCallerVictory() throws Exception {
        String fen = "k1R5/8/K7/8/8/8/8/8 b - - 0 50";
        mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(fen))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(WHITE_VICTORY.toString())));
    }

    @Test
    void testGetAiMove_withAiStalemate() throws Exception {
        String fen = "7K/7P/8/8/8/8/8/k7 w - - 99 50";
        mockMvc.perform(
                post(AI_MOVE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildAiMoveRequest(fen))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(STALEMATE.toString())));
    }
    
    @Test
    void testGetAvailableMoves() throws Exception {
        mockMvc.perform(
                post(AVAILABLE_MOVES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(
                        buildAvailableMovesRequest(FENWriter.STARTING_POSITION, "e2"))))
            .andExpect(status().isOk())
            .andExpect(content().string("[\"e2e3\",\"e2e4\"]"));
    }

    @Test
    void testGetAvailableMoves_fromEmptySquare() throws Exception {
        mockMvc.perform(
                post(AVAILABLE_MOVES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(
                        buildAvailableMovesRequest(FENWriter.STARTING_POSITION, "e3"))))
            .andExpect(status().isNoContent());
    }

    private AiMoveRequestDto buildAiMoveRequest(String fen) {
        final AiMoveRequestDto request = new AiMoveRequestDto();
        request.setFen(fen);
        request.setDepth(3);
        return request;
    }

    private AiMoveRequestDto buildAiMoveRequest(String fen, List<String> moveHistory) {
        final AiMoveRequestDto request = buildAiMoveRequest(fen);
        request.setMoveHistory(moveHistory);
        return request;
    }

    private AvailableMovesRequestDto buildAvailableMovesRequest(String fen, String fromSquare) {
        final AvailableMovesRequestDto request = new AvailableMovesRequestDto();
        request.setFen(fen);
        request.setFrom(fromSquare);
        return request;
    }
}
