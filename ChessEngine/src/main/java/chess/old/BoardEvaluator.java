package chess.old;

import java.util.ArrayList;
import java.util.List;

public class BoardEvaluator {
	private Board board;
	
	private double pieceValueWeight = 1;
	private double potencyWeight = 0.05;
	private double backupWeight = 0.01;
	private double positionWeight = 0.01;
	
	public void evaluate() {
		double input = 0;
		input += calculatePieceValueDifference() * pieceValueWeight;
//		input += calculatePotencyDifference() * potencyWeight;
//		input += calculateBackupDifference() * backupWeight;
//		input += calculateThreatenedSquareDifference() * positionWeight;
		board.setEvaluation(input);
	}
	
	public int calculatePieceValueDifference() {
		int piecesValue = 0;
		for (Piece p : board.getTeamPieces(board.getTurnTeam(), board.getPieces())) {
			if (p.getType() != PieceType.KING) {
				piecesValue += p.getValue();
			}
		}
		for(Piece p : board.getTeamPieces(board.getOpposingTeam(), board.getPieces())) {
			if (p.getType() != PieceType.KING) {
				piecesValue -= p.getValue();
			}
		}
		return piecesValue;
	}
	
	public int calculatePotencyDifference() {
		int potencyDiff = 0;
		for (Piece p : board.getTeamPieces(board.getTurnTeam(), board.getPieces())) {
			for(Piece op : board.getTeamPieces(board.getOpposingTeam(), board.getPieces())) {
				if (p.threatens(op.getSquare(), board.getPieces())) {
					potencyDiff += op.getValue();
				}
			}
		}
		for (Piece op : board.getTeamPieces(board.getOpposingTeam(), board.getPieces())) {
			for(Piece p : board.getTeamPieces(board.getTurnTeam(), board.getPieces())) {
				if (op.threatens(p.getSquare(), board.getPieces())) {
					potencyDiff -= p.getValue();
				}
			}
		}
		return potencyDiff;
	}
	
	public int calculateBackupDifference() {
		int backupDiff = 0;
		for (Piece p : board.getTeamPieces(board.getTurnTeam(), board.getPieces())) {
			if (p.getType() != PieceType.KING) {
				List<Piece> otherPieces = new ArrayList<>();
				for(Piece p2 : board.getPieces()) {
					if (p2 != p) {
						otherPieces.add(p);
					}
				}
				for(Piece p2 : board.getTeamPieces(board.getTurnTeam(), otherPieces)) {
					if (p2.threatens(p.getSquare(), otherPieces)) {
						backupDiff += p.getValue();
					}
				}
			}			
		}
		
		for (Piece p : board.getTeamPieces(board.getOpposingTeam(), board.getPieces())) {
			if (p.getType() != PieceType.KING) {
				List<Piece> otherPieces = new ArrayList<>();
				for(Piece p2 : board.getPieces()) {
					if (p2 != p) {
						otherPieces.add(p);
					}
				}
				for(Piece p2 : board.getTeamPieces(board.getOpposingTeam(), otherPieces)) {
					if (p2.threatens(p.getSquare(), otherPieces)) {
						backupDiff -= p.getValue();
					}
				}
			}			
		}
		return backupDiff;
	}
	
	public int calculateThreatenedSquareDifference() {
		int threatenedSquareDiff = 0;
		for(Square s : board.getSquares()) {
			boolean squareThreatened = false;
			for(Piece p : board.getTeamPieces(board.getTurnTeam(), board.getPieces())){
				if (!squareThreatened && p.threatens(s, board.getPieces())) {
					squareThreatened = true;
				}
				else {
					continue;
				}
			}
			if (squareThreatened) {
				threatenedSquareDiff ++;
			}
			
			squareThreatened = false;
			for(Piece p : board.getTeamPieces(board.getOpposingTeam(), board.getPieces())){
				if (!squareThreatened && p.threatens(s, board.getPieces())) {
					squareThreatened = true;
				}
				else {
					continue;
				}
			}
			if (squareThreatened) {
				threatenedSquareDiff --;
			}
		}
		return threatenedSquareDiff;
	}

	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}
	
}
