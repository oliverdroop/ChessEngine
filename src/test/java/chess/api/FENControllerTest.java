package chess.api;

import chess.api.dto.AiMoveRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class FENControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private MockMvc mockMvc;

    @Test
    void testGetAiMove() throws Exception {
        mockMvc.perform(
            post("/chess")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(buildRequest(FENWriter.STARTING_POSITION)))
        ).andExpect(status().is2xxSuccessful());
    }

    @Test
    void testGetAiMove_withNonsenseFEN() throws Exception {
        var resolvedException = mockMvc.perform(
            post("/chess")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(buildRequest("This is not FEN"))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResolvedException();

        assertThat(resolvedException).isNotNull();
        assertThat(resolvedException.getMessage()).contains("You must supply valid Forsyth-Edwards Notation in the 'fen' field");
    }

    @Test
    void testGetAiMove_withValidMoveHistory() throws Exception {
        mockMvc.perform(
                post("/chess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildRequest("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", List.of("e2e4")))))
            .andExpect(status().isOk());
    }

    @Test
    void testGetAiMove_withNonsenseMoveHistory() throws Exception {
        var resolvedException = mockMvc.perform(
                post("/chess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildRequest(FENWriter.STARTING_POSITION, List.of("e2e4", "nonsense")))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResolvedException();

        assertThat(resolvedException).isNotNull();
        assertThat(resolvedException.getMessage()).contains("moveHistory is invalid");
    }

    @Test
    void testGetAiMove_withInaccurateMoveHistory() throws Exception {
        var resolvedException = mockMvc.perform(
                post("/chess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildRequest(FENWriter.STARTING_POSITION, List.of("e2e4", "e7e5")))))
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
                post("/chess")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(buildRequest(fen, moveHistory))))
            .andExpect(status().isOk());
    }

    private AiMoveRequestDto buildRequest(String fen) {
        final AiMoveRequestDto request = new AiMoveRequestDto();
        request.setFen(fen);
        request.setDepth(3);
        return request;
    }

    private AiMoveRequestDto buildRequest(String fen, List<String> moveHistory) {
        final AiMoveRequestDto request = buildRequest(fen);
        request.setMoveHistory(moveHistory);
        return request;
    }
}
