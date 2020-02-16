function findDistance2D(p0, p1){
	var out = Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2));
	return out;
}