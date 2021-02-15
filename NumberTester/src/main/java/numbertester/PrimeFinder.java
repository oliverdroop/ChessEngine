package numbertester;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class PrimeFinder {
    
	private static final Logger LOGGER = LoggerFactory.getLogger(PrimeFinder.class);
	
    public void countPrimes(int upperLimit){
    	long startTime = System.currentTimeMillis();
        int num = 1;
        int count = 0;
        while(num <= upperLimit){
            if (isPrime(num)){
                //LOGGER.info("{} is prime", num);
            	//System.out.println(String.format("%d is prime", num));
            	count++;
            }
            num++;
        }
        LOGGER.info("Found {} primes in the region 1 to {}", count, upperLimit);
        LOGGER.info("It took {} milliseconds to find all primes up to {}", System.currentTimeMillis() - startTime, upperLimit);
    }
    
    public void findMersennePrimes() {
    	LOGGER.info("Starting Mersenne prime search");
    	long startTime = System.currentTimeMillis();
		long split = startTime;
		int upperLimit = 5000;
    	for(int i = 1; i < upperLimit; i++) {
    		if (!isPrime(i)) {
    			continue;
    		}
    		long diff = System.currentTimeMillis() - split;
    		LOGGER.debug("It took {} milliseconds to find prime {}", String.valueOf(diff), String.valueOf(i));
    		long startLL = System.currentTimeMillis();
    		if (lucasLehmer(i)) {
    			LOGGER.info("Lucas Lehmer test of {} retruned true in {} milliseconds", String.valueOf(i), System.currentTimeMillis() - startLL);
    		}
    		split = System.currentTimeMillis();
    	}
    	LOGGER.info("Finished Mersenne prime search up to {} in {} milliseconds", upperLimit, System.currentTimeMillis() - startTime);
    }
    
    public boolean lucasLehmer(int p){
    	LOGGER.debug("Performing Lucas Lehmer test of {}", String.valueOf(p));
    	BigInteger s = new BigInteger("4");
    	BigInteger two = new BigInteger("2");
    	BigInteger m = two.pow(p).subtract(BigInteger.ONE);
    	int count = p - 2;
    	while(count > 0) {
    		s = s.multiply(s).subtract(two).mod(m);
    		LOGGER.debug("s is {}", s.toString());
    		count--;
    	}
    	if (s.compareTo(BigInteger.ZERO) == 0) {
    		LOGGER.info("{} is a Mersenne prime", m.toString());
    		return true;
    	}
    	return false;
    }
    
    public boolean isPrime(int num){
        if (num > 0 && num <= 2){
            return true;
        }
        for(int i = 2; i <= num / 2; i++){
            if (num % i == 0){
                return false;
            }
        }
        return true;
    }
}
