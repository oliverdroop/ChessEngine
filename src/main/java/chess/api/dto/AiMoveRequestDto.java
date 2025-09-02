package chess.api.dto;

import chess.api.validation.AiMoveRequestValidation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

import static chess.api.validation.AiMoveRequestValidator.ALGEBRAIC_NOTATION_REGEX;

@AiMoveRequestValidation
public class AiMoveRequestDto extends FENRequestDto{

    private int depth;

    private List<@NotNull @Pattern(
        regexp = ALGEBRAIC_NOTATION_REGEX,
        message = "moveHistory is invalid"
    ) String> moveHistory;

    public AiMoveRequestDto(){}

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<String> getMoveHistory() {
        return moveHistory;
    }

    public void setMoveHistory(List<String> moveHistory) {
        this.moveHistory = moveHistory;
    }
}
