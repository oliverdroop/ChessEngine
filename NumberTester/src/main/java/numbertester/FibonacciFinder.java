package numbertester;

import java.util.ArrayList;
import java.util.List;

public class FibonacciFinder {
    public List<Integer> findFibonacciSequence(int upperLimit){
        List<Integer> fibonacciList = new ArrayList<>();
        int i = 1;
        int iPrev = 0;
        int iNew = 0;
        while (i < upperLimit){
            fibonacciList.add(i);
            System.out.println("Found fibonacci number: ".concat(String.valueOf(i)));
            iNew = i + iPrev;
            iPrev = i;
            i = iNew;            
        }
        return fibonacciList;
    }
}
