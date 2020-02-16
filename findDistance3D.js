function findDistance3D(v1, v2) {
	var x1 = v1.x;
	var y1 = v1.y;
	var z1 = v1.z;
	var x2 = v2.x;
	var y2 = v2.y;
	var z2 = v2.z;
	var dsxy = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	var dsz = z2 - z1;
	var out = 0;
	if (dsz != 0){
		out = Math.sqrt(Math.pow(dsxy, 2) + Math.pow(dsz, 2));
	}else{
		out = dsxy;
	}
	return out;
}