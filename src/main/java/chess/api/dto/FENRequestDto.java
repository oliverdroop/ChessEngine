package chess.api.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FENRequestDto {

    @NotNull
    @Pattern(
        regexp = "^(([bknpqrBKNPQR1-8]{1,8})/){7}[bknpqrBKNPQR1-8]{1,8} [bw] (K?Q?k?q?|-) (([a-h][36])|-) [0-9]{1,3} [0-9]{1,4}$",
        message = "You must supply valid Forsyth-Edwards Notation in the 'fen' field")
    private String fen;

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
