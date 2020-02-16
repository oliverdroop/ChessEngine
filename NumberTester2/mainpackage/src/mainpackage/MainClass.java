package mainpackage;

import java.util.List;


public class MainClass {
    public static void main(String[] args){
        BaseNExpresser baseNExpresser = new BaseNExpresser();
        int start = 0;
        int finish = 1024;
        int base = 2;
        for(int i = start; i < finish; i++){
            String baseNInteger = baseNExpresser.express(i, base);
            int base10Integer = baseNExpresser.decode(baseNInteger, base);
            System.out.println(i + " >> " + baseNInteger + " >> " + String.valueOf(base10Integer));
        }
    }
    
    public void makeNumberSequenceImages(){

        int upperLimit = 10000;
        Visualizer v = new Visualizer();
        
        PrimeFinder primeFinder = new PrimeFinder();
        List<Integer> primes = primeFinder.findPrimes(upperLimit);
        v.drawBlock(primes, primeFinder.getClass());
        v.drawSquareSpiral(primes, primeFinder.getClass());
        
        FibonacciFinder fibonacciFinder = new FibonacciFinder();
        List<Integer> fibonacciSequence = fibonacciFinder.findFibonacciSequence(upperLimit);
        v.drawBlock(fibonacciSequence, fibonacciFinder.getClass());
        v.drawSquareSpiral(fibonacciSequence, fibonacciFinder.getClass());
        
        SquareFinder squareFinder = new SquareFinder();
        List<Integer> squares = squareFinder.findSquares(upperLimit);
        v.drawBlock(squares, squareFinder.getClass());
        v.drawSquareSpiral(squares, squareFinder.getClass());
        
        TriangularFinder triangularFinder = new TriangularFinder();
        List<Integer> triangulars = triangularFinder.findTriangulars(upperLimit);
        v.drawBlock(triangulars, triangularFinder.getClass());
        v.drawSquareSpiral(triangulars, triangularFinder.getClass());
    }
}
