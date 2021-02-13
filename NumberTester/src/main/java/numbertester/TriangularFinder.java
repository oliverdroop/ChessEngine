package numbertester;

import java.util.ArrayList;
import java.util.List;

public class TriangularFinder {
    public List<Integer> findTriangulars(int upperLimit){
        List<Integer> triangulars = new ArrayList<>();
        int result = 0;
        int difference = 0;
        while (result < upperLimit){
            difference ++;
            result += difference;
            triangulars.add(result);
            System.out.println("Triangular found: ".concat(String.valueOf(result)));
        }
        return triangulars;
    }
}
