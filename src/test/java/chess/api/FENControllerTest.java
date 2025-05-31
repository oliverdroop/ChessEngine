package chess.api;

import chess.api.dto.AiMoveRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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

    private AiMoveRequestDto buildRequest(String fen) {
        final AiMoveRequestDto request = new AiMoveRequestDto();
        request.setFen(fen);
        request.setDepth(3);
        return request;
    }
}
