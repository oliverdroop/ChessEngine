package numbertester;

import java.util.ArrayList;
import java.util.List;

public class SquareFinder {
    public List<Integer> findSquares(int upperLimit){
        List<Integer> squares = new ArrayList<>();
        int factor = 0;
        int result = 0;
        while(result < upperLimit){
            result = factor * factor;
            squares.add(result);
            System.out.println("Found square: ".concat(String.valueOf(result)));
            factor++;
        }
        return squares;
    }
}
