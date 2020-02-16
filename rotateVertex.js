function rotateVertex(vertex, angle, Rvector){
	var theta = angle;
	var c = Math.cos(theta);
	var s = Math.sin(theta);
	var t = 1 - (Math.cos(theta));
	var Rvect = Rvector;
	var x = Rvect.x;
	var y = Rvect.y;
	var z = Rvect.z;
	var Rmatrix = [
		[(t * Math.pow(x, 2)) + c, (t * x * y) - (s * z), (t * x * z) + (s * y), 0],
		[(t * x * y) + (s * z), (t * Math.pow(y, 2)) + c, (t * y * z) - (s * x), 0],
		[(t * x * z) - (s * y), (t * y * z) + (s * x), (t * Math.pow(z, 2)) + c, 0],
		[0, 0, 0, 1]
	];
	var Vmatrix1 = [];
	Vmatrix1[0] = vertex.x;
	Vmatrix1[1] = vertex.y;
	Vmatrix1[2] = vertex.z;
	Vmatrix1[3] = 1;
	//
	var Vmatrix2 = [];
	Vmatrix2[0] = (Rmatrix[0][0] * Vmatrix1[0]) + (Rmatrix[0][1] * Vmatrix1[1]) + (Rmatrix[0][2] * Vmatrix1[2]) + (Rmatrix[0][3] * Vmatrix1[3]);
	Vmatrix2[1] = (Rmatrix[1][0] * Vmatrix1[0]) + (Rmatrix[1][1] * Vmatrix1[1]) + (Rmatrix[1][2] * Vmatrix1[2]) + (Rmatrix[1][3] * Vmatrix1[3]);
	Vmatrix2[2] = (Rmatrix[2][0] * Vmatrix1[0]) + (Rmatrix[2][1] * Vmatrix1[1]) + (Rmatrix[2][2] * Vmatrix1[2]) + (Rmatrix[2][3] * Vmatrix1[3]);
	Vmatrix2[3] = (Rmatrix[3][0] * Vmatrix1[0]) + (Rmatrix[3][1] * Vmatrix1[1]) + (Rmatrix[3][2] * Vmatrix1[2]) + (Rmatrix[3][3] * Vmatrix1[3]);
	//
	vertex.x = Vmatrix2[0];
	vertex.y = Vmatrix2[1];
	vertex.z = Vmatrix2[2];
}