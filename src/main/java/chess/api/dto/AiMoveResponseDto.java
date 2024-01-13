package chess.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiMoveResponseDto {

    @JsonProperty private String fen;
    @JsonProperty private String error;
    @JsonProperty private String gameResult;
    @JsonProperty private boolean isCheck;

    public void setFen(String fen) {
        this.fen = fen;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }
}
