package chess.api.dto;

public class AvailableMovesRequestDto {
    private String fen;

    private String from;


    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
