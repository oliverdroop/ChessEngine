#include <stdio.h>
#include <time.h>
#include <math.h>
#include <inttypes.h>
#include "gmp-impl.h"

int lucasLehmer(unsigned long int p){
	//initialize the number 0
	mpz_t zero;
	mpz_init_set_ui(zero, 0);
	//initialize the number 1
	mpz_t one;
	mpz_init_set_ui(one, 1);
	//initialize the number 2
	mpz_t two;
	mpz_init_set_ui(two, 2);
	
	//initialize the number s = 4
	mpz_t s;
	mpz_init_set_ui(s, 4);
	
	//initialize and calculate the number m
	mpz_t m;
	mpz_init(m);
	mpz_pow_ui(m, two, p);
	mpz_sub(m, m, one);
	
	//initialize the number count
	mpz_t count;
	mpz_init_set_ui(count, p);
	mpz_sub(count, count, two);
	
	int countZComp = mpz_cmp(count, zero);
	while(countZComp > 0) {
		mpz_mul(s, s, s);
		mpz_sub(s, s, two);
		mpz_mod(s, s, m);
		
		mpz_sub(count, count, one);
		countZComp = mpz_cmp(count, zero);
	}
	int zeroComp = mpz_cmp(s, zero);
	int rtrn = 0;
	if (zeroComp == 0) {
		gmp_printf("%Zd is a Mersenne prime generated from (2 ^ ", m);
		printf("%d) - 1\n", p);
		rtrn = 1;
	}
	mpz_clear(zero);
	mpz_clear(one);
	mpz_clear(two);
	mpz_clear(s);
	mpz_clear(m);
	mpz_clear(count);
	return rtrn;
}

int main(int argc, char **argv) {
	clock_t start = clock(), diff;
	int primeTest;
	unsigned long int upperLimit = 1000;
	
	if (argc > 1) {
		sscanf(argv[1], "%d", &upperLimit);
	}
	printf("Starting primeFinder with upper limit %d\n", upperLimit);
	int count = 0;
	mpz_t num;
	mpz_init_set_ui(num, 1);
	int numComp = mpz_cmp_ui(num, upperLimit);
	while(numComp < 0){
		unsigned long int nTest = mpz_get_ui(num);
		if (lucasLehmer(nTest) == 1){
			count++;
		}
		mpz_nextprime(num, num);
		numComp = mpz_cmp_ui(num, upperLimit);
	}
	printf("Found %d Mersenne primes in the region 1 to %d\n", count, upperLimit);
	diff = clock() - start;
	printf("Time taken %d milliseconds", diff);
	return 0;
}