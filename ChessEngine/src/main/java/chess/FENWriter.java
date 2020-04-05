package chess;

import java.util.List;

public class FENWriter {
	public String write(Board board){
			String fen = "";
			//board pieces
			for(int y = 7; y >= 0; y--) {
				int gapSize = 0;
				for(int x = 7; x >= 0; x--) {
					Piece piece = board.getPiece(x, y, board.pieces);
					if (piece != null) {
						if (gapSize > 0) {
							fen += gapSize;
							gapSize = 0;
						}
						fen += piece.getFEN();
					}
					else {
						gapSize ++;
					}
				}
				if (gapSize > 0) {
					fen += gapSize;
				}
				if (y != 0) {
					fen += '/';
				}
			}
			fen += ' ';
			//current turn team
			fen += (char)((byte)board.turnTeam.toString().charAt(0) + 32);
			fen += ' ';
			//castling availability
			String castling = "";
			Piece kingW = board.getPiece(3, 0, board.pieces);
			if (!kingW.hasMoved) {
				Piece rookWK = board.getPiece(0, 0, board.pieces);
				if (rookWK != null && !rookWK.hasMoved) {
					castling += 'K';
				}
				Piece rookWQ = board.getPiece(7, 0, board.pieces);
				if (rookWQ != null && !rookWQ.hasMoved) {
					castling += 'Q';
				}
			}
			Piece kingB = board.getPiece(3, 7, board.pieces);
			if (!kingB.hasMoved) {
				Piece rookBK = board.getPiece(0, 0, board.pieces);
				if (rookBK != null && !rookBK.hasMoved) {
					castling += 'k';
				}
				Piece rookBQ = board.getPiece(7, 0, board.pieces);
				if (rookBQ != null && !rookBQ.hasMoved) {
					castling += 'q';
				}
			}
			fen += (castling.length() > 0) ? castling : '-';
			fen += ' ';
			//en Passant piece
			String enPassant = "";
			if (board.enPassantable != null) {
				char x = (char)(board.enPassantable.x + 97);
				int yBehind = board.enPassantable.y + ((-1) + (2 * board.enPassantable.team.ordinal()));
				char y = String.valueOf(yBehind).charAt(0);
			}
			fen += (enPassant.length() > 0) ? enPassant : '-';
			fen += ' ';
			fen += board.halfmoveClock;
			fen += ' ';
			fen += board.fullmoveNumber;
			return fen;
	}
}
