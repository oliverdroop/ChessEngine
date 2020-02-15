package mainpackage;

import java.util.ArrayList;
import java.util.List;

public class PrimeFinder {
    
    public List<Integer> findPrimes(int upperLimit){
        List<Integer> primes = new ArrayList<>();
        int num = 1;
        while(num <= upperLimit){
            if (isPrime(num)){
                primes.add(num);
                String msg = ("Prime found: ".concat(String.valueOf(num)));
                System.out.println(msg);
            }
            num++;
        }
        return primes;
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
