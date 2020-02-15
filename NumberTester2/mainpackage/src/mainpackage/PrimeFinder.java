package mainpackage;

import java.util.ArrayList;
import java.util.List;

public class PrimeFinder {
    public List<Integer> findPrimes(int upperLimit){
        List<Integer> primes = new ArrayList<>();
        for(int i = 1; i <= upperLimit; i++){
            if (findFactors(i).size() == 0){
                primes.add(i);
            }
        }
        return primes;
    }
    
    public List<Integer> findFactors(int num){
        List<Integer> factors = new ArrayList<>();
        if (num <= 2){
            return factors;
        }
        for(int i = 2; i <= num / 2; i++){
            if (num % i == 0){
                factors.add(i);
            }
        }
        return factors;
    }
}
