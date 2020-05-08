package chess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveEvaluator {
	private Move move;
	
	private static Map<String, Double> weightMap = new HashMap<>();
	
	static {
		weightMap.put("considerPieceTaken", 2.0);
		weightMap.put("considerVulnerability", 1.9);
		weightMap.put("considerDependence", 0.75);
		weightMap.put("considerDependants", 0.75);
		weightMap.put("considerPotency", 1.0);
		weightMap.put("considerCentrality", 0.5);
	}
	
	public MoveEvaluator() {
		// TODO Auto-generated constructor stub
	}
	
	public MoveEvaluator(double taken, double vulnerability, double dependence, double dependants, double potency, double centrality) {
		weightMap.put("considerPieceTaken", taken);
		weightMap.put("considerVulnerability", vulnerability);
		weightMap.put("considerDependence", dependence);
		weightMap.put("considerDependants", dependants);
		weightMap.put("considerPotency", potency);
		weightMap.put("considerCentrality", centrality);
	}

	public void evaluate(int halfmovesAhead) {
		double result = 1;
		result = considerPieceTaken(result);
		result = considerPotency(result);
		result = considerVulnerability(result);
		result = considerDependence(result);
		result = considerDependants(result);
		result = considerCentrality(result);
		if (halfmovesAhead > 0) {
			halfmovesAhead --;
			result = considerFuture(result, halfmovesAhead);
		}
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
		int threatFactor = existingThreats - newThreats;
		return input + Math.pow(threatFactor, weightMap.get("considerVulnerability"));
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
		int	potencyFactor = newTargets - existingTargets;
		return input + Math.pow(potencyFactor, weightMap.get("considerPotency"));
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
		int	dependenceFactor = newBackups - existingBackups;
		return input + Math.pow(dependenceFactor, weightMap.get("considerDependence"));
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
		int	dependenceFactor = newDependants - existingDependants;
		return input + Math.pow(dependenceFactor, weightMap.get("considerDependants"));
	}
	
	public double considerCentrality(double input) {
		double d1 = CoordinateHolder.findDistance(move.getStartSquare().getX(), move.getStartSquare().getY(), 3.5, 3.5);
		double d2 = CoordinateHolder.findDistance(move.getEndSquare().getX(), move.getEndSquare().getY(), 3.5, 3.5);
		double centralityFactor = d1 - d2;
		return input + Math.pow(centralityFactor, weightMap.get("considerCentrality"));
	}

	public double considerFuture(double input, int halfmovesAhead) {
		List<Move> futureMoves = new ArrayList<>();
		move.getResultantBoard().getAvailableMoves().forEach(m -> futureMoves.add(m));
		if (futureMoves.size() > 0) {
			for(Move futureMove : futureMoves) {
				setMove(futureMove);
				evaluate(halfmovesAhead);
			}
			futureMoves.sort(null);
			
			return input - futureMoves.get(futureMoves.size() - 1).getEvaluation();
		}
		else {
			return input;
		}
	}
	
	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}
}
