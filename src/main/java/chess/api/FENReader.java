package chess.api;

import chess.api.pieces.*;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static chess.api.PieceConfiguration.*;

public class FENReader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FENReader.class);

	private static final Map<Character, Integer> pieceMappings = new ImmutableMap.Builder<Character, Integer>()
			.put('K', KING_OCCUPIED)
			.put('k', KING_OCCUPIED)
			.put('Q', QUEEN_OCCUPIED)
			.put('q', QUEEN_OCCUPIED)
			.put('R', ROOK_OCCUPIED)
			.put('r', ROOK_OCCUPIED)
			.put('N', KNIGHT_OCCUPIED)
			.put('n', KNIGHT_OCCUPIED)
			.put('B', BISHOP_OCCUPIED)
			.put('b', BISHOP_OCCUPIED)
			.put('P', PAWN_OCCUPIED)
			.put('p', PAWN_OCCUPIED).build();
	
	public static PieceConfiguration read(String fen) {
		PieceConfiguration pieceConfiguration = new PieceConfiguration();
		String[] fields = fen.split(" ");
		String[] ranks = fields[0].split("/");
		for(int y = 0; y < 8; y++) {
			String rank = ranks[7 - y];
			int x = 0;
			for(int i = 0; i < rank.length(); i++) {
				char c = rank.charAt(i);
				if ((int)c > 64) {
					pieceConfiguration.addPiece(createPiece(c, x, y));
					x ++;
				}
				else {
					x += Integer.parseInt(String.valueOf(c));
				}
			}
		}
		LOGGER.debug("Pieces set successfully");

		pieceConfiguration.setTurnSide(fields[1].equals("w") ? 0 : 1);
		LOGGER.debug("Turn team set successfully");
		
		if (fields[2].contains("K")) {
			pieceConfiguration.addCastlePosition(6);
		}
		if (fields[2].contains("Q")) {
			pieceConfiguration.addCastlePosition(2);
		}
		if (fields[2].contains("k")) {
			pieceConfiguration.addCastlePosition(62);
		}
		if (fields[2].contains("q")) {
			pieceConfiguration.addCastlePosition(58);
		}
		LOGGER.debug("Castling availability set successfully");
		
		if (fields[3].length() == 2) {
			pieceConfiguration.setEnPassantSquare(Position.getPositionFromCoordinateString(fields[3]));
		}
		else {
			pieceConfiguration.setEnPassantSquare(-1);
		}
		LOGGER.debug("EnPassantable set successfully");

		pieceConfiguration.setHalfMoveClock(Integer.parseInt(fields[4]));
		LOGGER.debug("Halfmove clock set successfully");
		pieceConfiguration.setFullMoveNumber(Integer.parseInt(fields[5]));
		LOGGER.debug("Fullmove number set successfully");
		return pieceConfiguration;
	}
	
	public static int createPiece(char c, int x, int y) {
		int position = Position.getPosition(x, y);
		int sideFlag = c < 97 ? WHITE_OCCUPIED : BLACK_OCCUPIED;
		int pieceTypeFlag = pieceMappings.get(c);
		return position + pieceTypeFlag + sideFlag;
	}
}
