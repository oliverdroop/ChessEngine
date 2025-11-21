package com.oliverdroop.chess.api;

import com.oliverdroop.chess.api.configuration.PieceConfiguration;
import com.oliverdroop.chess.api.pieces.Piece;

import java.util.*;

import static com.oliverdroop.chess.api.configuration.PieceConfiguration.*;

public class FENWriter {

	public static final String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static final Map<Integer, String> CASTLE_POSITION_CODES = Map.of(6, "K", 2, "Q", 62, "k", 58, "q");

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
        fenBuilder.append(
            Arrays.stream(pieceConfiguration.getCastlePositions())
                .boxed()
                .map(CASTLE_POSITION_CODES::get)
                .sorted()
                .reduce(String::concat)
                .orElse("-"));
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
