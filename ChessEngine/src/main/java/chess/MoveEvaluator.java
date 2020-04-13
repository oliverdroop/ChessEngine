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
		weightMap.put("considerDependence", 0.1);
		weightMap.put("considerDependants", 0.1);
		weightMap.put("considerPotency", 0.1);
	}
	
	public void evaluate() {
		double start = 1;
		double result = considerPieceTaken(start);
		result = considerVulnerability(result);
		result = considerDependence(result);
		result = considerDependants(result);
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
				existingThreats += move.getPiece().getValue();
			}
			if (opposingPiece.threatens(move.getEndSquare(), allPieces)) {
				newThreats += move.getPiece().getValue();
			}
		}
		int threatFactor = 1;
		if (newThreats != 0) {
			threatFactor = existingThreats / newThreats;
		}
		return input * threatFactor * weightMap.get("considerVulnerability");
	}
	
	public double considerPotency(double input) {
		int newTargets = 0;
		int existingTargets = 0;
		List<Piece> allPieces = move.getBoard().getPieces();
		for(Piece opposingPiece : move.getBoard().getTeamPieces(move.getPiece().getOpposingTeam(), allPieces)) {
			if (move.getPiece().threatens(opposingPiece.getSquare(), allPieces)) {
				existingTargets += opposingPiece.getValue();
			}
			Board newBoard = move.getResultantBoard();
			Square newOpposingSquare = newBoard.getSquare(opposingPiece.getSquare().getX(), opposingPiece.getSquare().getY());
			Piece movedPiece = newBoard.getPiece(move.getEndSquare().getX(), move.getEndSquare().getY(), newBoard.getPieces());
			if (movedPiece.threatens(newOpposingSquare, newBoard.getPieces())) {
				newTargets += opposingPiece.getValue();
			}
		}
		int potencyFactor = 1;
		if (existingTargets != 0) {
			potencyFactor = newTargets / existingTargets;
		}
		return input * potencyFactor * weightMap.get("considerPotency");
	}
	
	public double considerDependence(double input) {
		int existingBackups = 0;
		int newBackups = 0;
		List<Piece> allPieces = new ArrayList<>();
		move.getBoard().getPieces().forEach(p -> allPieces.add(p));
		allPieces.remove(move.piece);
		if (move.getPiece().getType() != PieceType.KING) {
			for(Piece friendlyPiece : move.getBoard().getTeamPieces(move.getPiece().getTeam(), allPieces)) {
				if (friendlyPiece != move.getPiece()) {
					if (friendlyPiece.threatens(move.getStartSquare(), allPieces)) {
						existingBackups += move.getPiece().getValue();
					}
					if (friendlyPiece.threatens(move.getEndSquare(), allPieces)) {
						newBackups += move.getPiece().getValue();
					}
				}
			}
		}
		int dependenceFactor = 1;
		if(existingBackups != 0) {
			dependenceFactor = newBackups / existingBackups;
		}
		return input * dependenceFactor * weightMap.get("considerDependence");
	}
	
	public double considerDependants(double input) {
		int existingDependants = 0;
		int newDependants = 0;
		for(Piece friendlyPiece : move.getBoard().getTeamPieces(move.getPiece().getTeam(), move.getBoard().getPieces())) {
			if (friendlyPiece.getType() != PieceType.KING) {
				List<Piece> allPieces = new ArrayList<>();
				for(Piece piece : move.getBoard().getPieces()) {
					if (piece != friendlyPiece) {
						allPieces.add(piece);
					}
				}
				if (move.getPiece().threatens(friendlyPiece.getSquare(), allPieces)) {
					existingDependants += friendlyPiece.getValue();
				}
				allPieces.remove(move.getPiece());
				Piece movedPiece = new PieceBuilder()
						.withBoard(move.getPiece().getBoard())
						.withTeam(move.getPiece().getTeam())
						.withType(move.getPiece().getType())
						.withHasMoved(true)
						.withX(move.getEndSquare().getX())
						.withY(move.getEndSquare().getY()).build();
				allPieces.add(movedPiece);
				if (movedPiece.threatens(friendlyPiece.getSquare(), allPieces)) {
					newDependants += friendlyPiece.getValue();
				}
			}
		}
		int dependenceFactor = 1;
		if (existingDependants != 0) {
			dependenceFactor = newDependants / existingDependants;
		}
		return input * dependenceFactor * weightMap.get("considerDependants");
	}

	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
