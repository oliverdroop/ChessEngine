function findAngle2D(p1, p2){
	A = Math.atan((p2.x - p1.x) / (p1.y - p2.y));
	if ((p2.x - p1.x) >= 0 && (p2.y - p1.y) > 0) {
		A += Math.PI;
	}
	if ((p2.x - p1.x) < 0 && (p2.y - p1.y) > 0) {
		A += Math.PI;
	}
	if ((p2.x - p1.x) >= 0 && (p2.y - p1.y) <= 0) {
		//do nothing
	}
	if ((p2.x - p1.x) < 0 && (p2.y - p1.y) <= 0) {
		A += 2 * Math.PI;
	}
	return (A);
}