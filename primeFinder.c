#include <stdio.h>
#include <time.h>
#include <math.h>
#include <inttypes.h>

int isPrime(int test){
	if (test == 1){
		return 1;
	}
	for(int i = 2; i <= test / 2; i++){
		if (test % i == 0){
			return 0;
		}
	}
	return 1;
}

int lucasLehmer(int p){
	int64_t s = 4;
	int64_t m = pow(2, p) - 1;
	int64_t count = p - 2;
	while(count > 0) {
		s = ((s * s) - 2) % m;
		count--;
	}
	if (s == 0) {
		printf("%d is a Mersenne prime generated from prime %d\n", m, p);
		return 1;
	}
	return 0;
}

int main() {
	clock_t start = clock(), diff;
	int primeTest;
	int upperLimit = 1000;
	int count = 0;
	int num = 1;
	while(num < upperLimit){
		primeTest = isPrime(num);
		if (primeTest == 1){
			if (lucasLehmer(num) == 1){
				count++;
			}
		}
		num++;
	}
	printf("Found %d Mersenne primes in the region 1 to %d", count, upperLimit);
	diff = clock() - start;
	int msec = diff * 1000 / CLOCKS_PER_SEC;
	printf("Time taken %d seconds %d milliseconds", msec/1000, msec%1000);
	return 0;
}