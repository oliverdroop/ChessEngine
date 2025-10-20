package chess.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetResource() throws Exception {
        final String expectedFile = new ClassPathResource("/public/grandMuster.html")
            .getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(
                get("/grandMuster.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(expectedFile));
    }

    @Test
    void testGetResource_with404() throws Exception {
        mockMvc.perform(
                get("/login"))
            .andExpect(status().isNotFound());
    }
}
