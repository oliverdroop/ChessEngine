function findIsWithin2D(p, poly){
	var out = true;
	var p0 = {x: poly.v0.screenX, y: poly.v0.screenY};
	var p1 = {x: poly.v1.screenX, y: poly.v1.screenY};
	var p2 = {x: poly.v2.screenX, y: poly.v2.screenY};
	if (findIsBetween2D(p, p0, p2, p1) == false){
		out = false;
	}
	if (findIsBetween2D(p, p1, p0, p2) == false){
		out = false;
	}
	if (findIsBetween2D(p, p2, p1, p0) == false){
		out = false;
	}
	return out;
}