package mainpackage;

import java.util.List;

public class MainClass {
    public static void main(String[] args){
        List<Integer> primes = new PrimeFinder().findPrimes(1000000);
    }
}
