package chess;

public class CoordinateHolder {
	protected int x;
	protected int y;
	
	public boolean blocks(Move move){
		if (move.diagonal() || move.orthogonal()){
			double angle1 = findAngle(move.startSquare, move.endSquare);
			double angle2 = findAngle(move.startSquare, this);
			double angle3 = findAngle(this, move.endSquare);
			double dist1 = findDistance(move.startSquare, move.endSquare);
			double dist2 = findDistance(move.startSquare, this);
			if (angle1 == angle2 && angle2 == angle3 && dist1 > dist2){
				return true;
			}
		}
		return false;
	}
	
	public static double findAngle(CoordinateHolder p1, CoordinateHolder p2) {
		double a = 0;
		if (p1.y - p2.y != 0){
			if (p2.x == p1.x && p2.y > p1.y) {
				a = Math.PI;
			}
			else if (p2.x == p1.x && p2.y < p1.y) {
				a = 0;
			}
			else {
				a = Math.atan((p2.x - p1.x) / (p1.y - p2.y));
			}
		}else{
			a = Math.PI / (2 * Math.signum(p2.x - p1.x));
		}
		if (p2.x - p1.x >= 0 && p2.y - p1.y > 0) {
			a += Math.PI;
		}
		if (p2.x - p1.x < 0 && p2.y - p1.y > 0) {
	        a += Math.PI;
	    }
		if (p2.x - p1.x < 0 && p2.y - p1.y <= 0) {
	        a += Math.PI * 2;
	    }
		return a;
	}

	public static double findDistance(CoordinateHolder p0, CoordinateHolder p1){
		return Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2));
	}
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
}
