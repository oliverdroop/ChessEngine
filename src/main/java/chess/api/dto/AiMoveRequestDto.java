package chess.api.dto;

public class AiMoveRequestDto {
    private int depth;

    private String fen;

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }
}
