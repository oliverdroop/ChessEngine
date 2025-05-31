package chess.api.dto;

import jakarta.validation.constraints.NotNull;

public class AiMoveRequestDto extends FENRequestDto{

    @NotNull
    private int depth;

    public AiMoveRequestDto(){}

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
