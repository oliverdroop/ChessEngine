package chess.api;

import chess.api.pieces.*;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class FENReader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FENReader.class);

	private static final Map<Character, Class> pieceMappings = new ImmutableMap.Builder<Character, Class>()
			.put('K', King.class)
			.put('k', King.class)
			.put('Q', Queen.class)
			.put('q', Queen.class)
			.put('R', Rook.class)
			.put('r', Rook.class)
			.put('N', Knight.class)
			.put('n', Knight.class)
			.put('B', Bishop.class)
			.put('b', Bishop.class)
			.put('P', Pawn.class)
			.put('p', Pawn.class).build();
	
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

		pieceConfiguration.setTurnSide(fields[1].equals("w") ? Side.WHITE : Side.BLACK);
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
			pieceConfiguration.setEnPassantSquare(null);
		}
		LOGGER.debug("EnPassantable set successfully");

		pieceConfiguration.setHalfMoveClock(Integer.parseInt(fields[4]));
		LOGGER.debug("Halfmove clock set successfully");
		pieceConfiguration.setFullMoveNumber(Integer.parseInt(fields[5]));
		LOGGER.debug("Fullmove number set successfully");
		return pieceConfiguration;
	}
	
	public static Piece createPiece(char c, int x, int y) {
		int position = Position.getPosition(x, y);
		Side side = (int) c < 97 ? Side.WHITE : Side.BLACK;
		Class<Piece> clazz = pieceMappings.get(c);
		try {
			return clazz.getDeclaredConstructor(Side.class, int.class).newInstance(side, position);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			LOGGER.error("Caught exception creating piece: {}", e.getMessage());
			return null;
		}
	}
}
