package chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveEvaluator {
	private Move move;
	
	private static Map<String, Double> weightMap = new HashMap<>();
	
	static {
		weightMap.put("considerPieceTaken", 0.1);
		weightMap.put("considerVulnerability", 0.1);
		weightMap.put("considerIsBackedUp", 0.1);
		weightMap.put("considerBacksUp", 0.1);
	}
	
	public void evaluate() {
		double start = 1;
		double result = considerPieceTaken(start);
		result = considerVulnerability(result);
		result = considerIsBackedUp(result);
		//result = considerBacksUp(result);
		move.setEvaluation(result);
	}
	
	public double considerPieceTaken(double input) {
		if (move.getPieceTaken() == null) {
			return input;
		}
		int pieceValue = move.getPieceTaken().getValue();
		return input + (pieceValue * weightMap.get("considerPieceTaken"));
	}
	
	public double considerVulnerability(double input) {
		int newThreats = 0;
		int existingThreats = 0;
		List<Piece> allPieces = move.getBoard().getPieces();
		for(Piece opposingPiece : move.getBoard().getTeamPieces(move.getPiece().getOpposingTeam(), allPieces)) {
			if (opposingPiece.threatens(move.getStartSquare(), allPieces)) {
				existingThreats ++;
			}
			if (opposingPiece.threatens(move.getEndSquare(), allPieces)) {
				newThreats ++;
			}
		}
		if (newThreats == existingThreats) {
			return input;
		}
		int threatDiff = existingThreats - newThreats;
		int threatDiffMagnitude = Math.abs(threatDiff);
		while(threatDiffMagnitude > 0) {
			input = input * (1 - (Math.signum(threatDiff) * weightMap.get("considerVulnerability")));
			threatDiffMagnitude --;
		}
		return input;
	}
	
	public double considerIsBackedUp(double input) {
		int existingBackups = 0;
		int newBackups = 0;
		List<Piece> allPieces = new ArrayList<>();
		move.getBoard().getPieces().forEach(p -> allPieces.add(p));
		allPieces.remove(move.piece);
		for(Piece friendlyPiece : move.getBoard().getTeamPieces(move.getPiece().getTeam(), allPieces)) {
			if (friendlyPiece != move.getPiece()) {
				if (friendlyPiece.threatens(move.getStartSquare(), allPieces)) {
					existingBackups ++;
				}
				if (friendlyPiece.threatens(move.getEndSquare(), allPieces)) {
					newBackups ++;
				}
			}
		}
		if (newBackups == existingBackups) {
			return input;
		}
		int backupDiff = existingBackups - newBackups;
		int backupDiffMagnitude = Math.abs(backupDiff);
		while(backupDiffMagnitude > 0) {
			input = input * (1 + (Math.signum(backupDiff) * weightMap.get("considerIsBackedUp")));
			backupDiffMagnitude --;
		}
		return input;
	}
	
	public double considerBacksUp(double input) {
		int existingReliants = 0;
		int newReliants = 0;
		List<Piece> allPieces = new ArrayList<>();
		move.getBoard().getPieces().forEach(p -> allPieces.add(p));
		allPieces.remove(move.piece);
		for(Piece friendlyPiece : move.getBoard().getTeamPieces(move.getPiece().getTeam(), allPieces)) {
			if (friendlyPiece != move.getPiece() && friendlyPiece.getType() != PieceType.KING) {
				allPieces.remove(friendlyPiece);
				if (move.getPiece().getType() != PieceType.KING) {
					if (move.getPiece().threatens(friendlyPiece.getSquare(), allPieces)) {
						existingReliants ++;
					}
				}
				else {
					if (new Move(move.getPiece(), move.getStartSquare(), friendlyPiece.getSquare()).adjacent()) {
						existingReliants ++;
					}
				}
				allPieces.remove(move.getPiece());
				Piece movedPiece = new PieceBuilder()
						.withBoard(move.getBoard())
						.withHasMoved(true)
						.withTeam(move.getPiece().getTeam())
						.withType(move.getPiece().getType())
						.withX(move.getEndSquare().getX())
						.withY(move.getEndSquare().getY()).build();
				allPieces.add(movedPiece);
				if (movedPiece.getType() != PieceType.KING) {
					if (movedPiece.threatens(friendlyPiece.getSquare(), allPieces)) {
						newReliants ++;
					}
				}
				else {
					if (new Move(movedPiece, movedPiece.getSquare(), friendlyPiece.getSquare()).adjacent()) {
						newReliants ++;
					}
				}
			}
		}
		if (newReliants == existingReliants) {
			return input;
		}
		int backupDiff = existingReliants - newReliants;
		int backupDiffMagnitude = Math.abs(backupDiff);
		while(backupDiffMagnitude > 0) {
			input = input * (1 + (Math.signum(backupDiff) * weightMap.get("considerBacksUp")));
			backupDiffMagnitude --;
		}
		return input;
	}

	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
