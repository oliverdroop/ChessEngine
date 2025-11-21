package com.oliverdroop.chess.api;

import com.oliverdroop.chess.api.configuration.PieceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.oliverdroop.chess.api.configuration.PieceConfiguration.*;
import static java.lang.String.format;

public class FENReader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FENReader.class);

	private static final Map<Character, Integer> PIECE_MAPPINGS = new HashMap<>();

	static {
		PIECE_MAPPINGS.put('K', KING_OCCUPIED);
		PIECE_MAPPINGS.put('k', KING_OCCUPIED);
		PIECE_MAPPINGS.put('Q', QUEEN_OCCUPIED);
		PIECE_MAPPINGS.put('q', QUEEN_OCCUPIED);
		PIECE_MAPPINGS.put('R', ROOK_OCCUPIED);
		PIECE_MAPPINGS.put('r', ROOK_OCCUPIED);
		PIECE_MAPPINGS.put('N', KNIGHT_OCCUPIED);
		PIECE_MAPPINGS.put('n', KNIGHT_OCCUPIED);
		PIECE_MAPPINGS.put('B', BISHOP_OCCUPIED);
		PIECE_MAPPINGS.put('b', BISHOP_OCCUPIED);
		PIECE_MAPPINGS.put('P', PAWN_OCCUPIED);
		PIECE_MAPPINGS.put('p', PAWN_OCCUPIED);
	}

    public static PieceConfiguration read(String fen, Class<? extends PieceConfiguration> clazz) {
        PieceConfiguration pieceConfiguration;
        try {
            pieceConfiguration = clazz.getConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            final String message = format("Could not create new instance of %s", clazz.getSimpleName());
            LOGGER.error(message);
            throw new RuntimeException(message, e);
        }
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
        if (FENWriter.STARTING_POSITION.equals(fen)) {
            pieceConfiguration.setHistoricMoves(new short[]{});
        }
        return pieceConfiguration;
    }
	
	public static int createPiece(char c, int x, int y) {
		final int position = Position.getPosition(x, y);
		int sideFlag = c < 97 ? WHITE_OCCUPIED : BLACK_OCCUPIED;
		final int pieceTypeFlag = PIECE_MAPPINGS.get(c);
		return position | pieceTypeFlag | sideFlag;
	}
}
