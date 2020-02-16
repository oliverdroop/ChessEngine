function findNormal3D(v0, v1, v2){
	var vec1 = {x:v1.x - v0.x, y:v1.y - v0.y, z:v1.z - v0.z};
	var vec2 = {x:v2.x - v0.x, y:v2.y - v0.y, z:v2.z - v0.z};
	var crossProduct = {x:((vec1.y * vec2.z) - (vec1.z * vec2.y)), y:((vec1.z * vec2.x) - (vec1.x * vec2.z)), z:((vec1.x * vec2.y) - (vec1.y * vec2.x))};
	var vx0 = {x:0, y:0, z:0};
	var fac = 1 / (findDistance3D(vx0, crossProduct));
	crossProduct.x = crossProduct.x * fac;
	crossProduct.y = crossProduct.y * fac;
	crossProduct.z = crossProduct.z * fac;
	return crossProduct;
}