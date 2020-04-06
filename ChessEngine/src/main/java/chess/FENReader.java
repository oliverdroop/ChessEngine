package chess;

public class FENReader {
	public Board read(String fen) {
		Board board = new Board();
		String[] fields = fen.split(" ");
		String[] ranks = fields[0].split("/");
		for(int y = 0; y < 8; y++) {
			String rank = ranks[7 - y];
			int x = 0;
			for(int i = 0; i < rank.length(); i++) {
				char c = rank.charAt(rank.length() - i - 1);
				if ((int)c > 64) {
					Piece piece = createPiece(c);
					piece.board = board;
					piece.x = x;
					piece.y = y;
					piece.hasMoved = false;
					board.pieces.add(piece);
					x ++;
				}
				else {
					x += Integer.parseInt(String.valueOf(c));
				}
			}
		}
		
		board.turnTeam = fields[1].equals("w") ? Team.WHITE : Team.BLACK;
		
		if (!fields[2].contains("K")) {
			board.getPiece(0, 0, board.pieces).hasMoved = true;
		}
		if (!fields[2].contains("Q")) {
			board.getPiece(7, 0, board.pieces).hasMoved = true;
		}
		if (!fields[2].contains("k")) {
			board.getPiece(0, 7, board.pieces).hasMoved = true;
		}
		if (!fields[2].contains("q")) {
			board.getPiece(7, 7, board.pieces).hasMoved = true;
		}
		
		for(Piece piece : board.pieces) {
			if (piece.type == PieceType.PAWN && piece.y != 1 + (piece.team.ordinal() * 5)) {
				piece.hasMoved = true;
			}
		}
		
		if (fields[3].length() == 2) {
			int x = (int)fields[3].charAt(0) - 97;
			int y = Integer.parseInt(String.valueOf(fields[3].charAt(1)));
			y += (1) - (2 * (1 - board.turnTeam.ordinal()));
			board.enPassantable = board.getPiece(x, y, board.pieces);
		}
		else {
			board.enPassantable = null;
		}
		
		board.halfmoveClock = Integer.parseInt(fields[4]);
		board.fullmoveNumber = Integer.parseInt(fields[5]);
		return board;
	}
	
	public Piece createPiece(char c) {
		PieceBuilder builder = new PieceBuilder();
		if (c == 'K' || c == 'k') {
			builder = builder.withType(PieceType.KING);
		}
		if (c == 'Q' || c == 'q') {
			builder = builder.withType(PieceType.QUEEN);
		}
		if (c == 'R' || c == 'r') {
			builder = builder.withType(PieceType.ROOK);
		}
		if (c == 'B' || c == 'b') {
			builder = builder.withType(PieceType.BISHOP);
		}
		if (c == 'N' || c == 'n') {
			builder = builder.withType(PieceType.KNIGHT);
		}
		if (c == 'P' || c == 'p') {
			builder = builder.withType(PieceType.PAWN);
		}
		
		if ((int)c < 91) {
			builder = builder.withTeam(Team.WHITE);
		}
		else {
			builder = builder.withTeam(Team.BLACK);
		}
		return builder.build();
	}
}
