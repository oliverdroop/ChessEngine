package mainpackage;

public class MainClass {
    public static void main(String[] args){
        new PrimeFinder().findPrimes(100).forEach(n -> System.out.println(n));
    }
}
