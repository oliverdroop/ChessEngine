function findIsBetween2D(centre, left, right, input) {
	var output = false;
	var aleft = findAngle2D(centre, left);
	var aright = findAngle2D(centre, right);
	if (aleft > aright) {
		aleft -= 2 * Math.PI;
	}
	var ainput = findAngle2D(centre, input);
	if (ainput >= aleft && ainput <= aright) {
		output = true;
	} else {
		ainput -= 2 * Math.PI;
		if (ainput >= aleft && ainput <= aright) {
			output = true;
		}
	}
	return output;
}