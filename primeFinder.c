#include <stdio.h>
#include <time.h>
#include <math.h>
#include <inttypes.h>
#include "gmp-impl.h"

int isPrime(unsigned long int test){
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
	mpz_t twoToP;
	mpz_init(twoToP);
	mpz_pow_ui(twoToP, two, p);
	mpz_t m;
	mpz_init(m);
	mpz_sub(m, twoToP, one);
	
	//initialize the number count
	mpz_t pEq;
	mpz_init_set_ui(pEq, p);
	mpz_t count;
	mpz_init(count);
	mpz_sub(count, pEq, two);
	
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
		gmp_printf("%Zd is a Mersenne prime generated from prime ", m);
		printf("%d\n", p);
		rtrn = 1;
	}
	mpz_clear(zero);
	mpz_clear(one);
	mpz_clear(two);
	mpz_clear(s);
	mpz_clear(twoToP);
	mpz_clear(m);
	mpz_clear(pEq);
	mpz_clear(count);
	return rtrn;
}

int main(int argc, char **argv) {
	clock_t start = clock(), diff;
	int primeTest;
	int upperLimit;
	sscanf(argv[1], "%d", &upperLimit);
	int count = 0;
	unsigned long int num = 1;
	while(num < upperLimit){
		primeTest = isPrime(num);
		if (primeTest == 1){
			if (lucasLehmer(num) == 1){
				count++;
			}
		}
		num++;
	}
	printf("Found %d Mersenne primes in the region 1 to %d\n", count, upperLimit);
	diff = clock() - start;
	printf("Time taken %d milliseconds", diff);
	return 0;
}