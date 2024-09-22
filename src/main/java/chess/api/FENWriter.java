package chess.api;

import chess.api.pieces.Piece;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;

import static chess.api.PieceConfiguration.*;

public class FENWriter {

	public static final String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	private static final char[] CASTLE_POSITION_CODES = new char[]{'K', 'Q', 'k', 'q'};
	private static final int[] CASTLE_POSITIONS = new int[]{6, 2, 62, 58};
	public static String write(PieceConfiguration pieceConfiguration){
		StringBuilder fenBuilder = new StringBuilder();
		//board pieces
		for(int y = 7; y >= 0; y--) {
			int gapSize = 0;
			for(int x = 0; x < 8; x++) {
				int pieceBitFlag = pieceConfiguration.getPieceAtPosition(Position.getPosition(x, y));
				if ((pieceBitFlag & ALL_PIECE_AND_COLOUR_FLAGS_COMBINED) > 0) {
					if (gapSize > 0) {
						fenBuilder.append(gapSize);
						gapSize = 0;
					}
					fenBuilder.append(Piece.getFENCode(pieceBitFlag));
				} else {
					gapSize ++;
				}
			}
			if (gapSize > 0) {
				fenBuilder.append(gapSize);
			}
			if (y != 0) {
				fenBuilder.append('/');
			}
		}
		fenBuilder.append(' ');
		//current turn team
		fenBuilder.append(pieceConfiguration.getTurnSide() == 0 ? 'w' : 'b');
		fenBuilder.append(' ');
		//castling availability
		boolean castling = false;
		int[] castlePositions = pieceConfiguration.getCastlePositions();
		for(int i = 0; i < 4; i++) {
			int castlePosition = CASTLE_POSITIONS[i];
			if(Arrays.stream(castlePositions).anyMatch(cp -> cp == castlePosition)) {
				char castlePositionCode = CASTLE_POSITION_CODES[i];
				fenBuilder.append(castlePositionCode);
				castling = true;
			}
		}
		if (!castling) {
			fenBuilder.append('-');
		}
		fenBuilder.append(' ');
		//en Passant piece
		if (pieceConfiguration.getEnPassantSquare() >= 0) {
			fenBuilder.append(Position.getCoordinateString(pieceConfiguration.getEnPassantSquare()));
		} else {
			fenBuilder.append('-');
		}
		fenBuilder.append(' ');
		fenBuilder.append(pieceConfiguration.getHalfMoveClock());
		fenBuilder.append(' ');
		fenBuilder.append(pieceConfiguration.getFullMoveNumber());
		return fenBuilder.toString();
	}
}
