package mainpackage;

import java.util.ArrayList;

public class MainClass {
    public static void main(String[] args){
        //List<Integer> primes = new PrimeFinder().findPrimes(1000000);
        Visualizer v = new Visualizer();
        v.drawSpiral(new ArrayList<Integer>());
    }
}
