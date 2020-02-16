package mainpackage;


public class BaseNExpresser {
    
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public String express(int input, int base){
        String output = "";
        int[] parts = getParts(input, base);
        output = characterOf(parts[1]);
        while(parts[0] >= base){
            parts = getParts(parts[0], base);
            output = characterOf(parts[1]).concat(output);
        }
        if ( parts[0] > 0){
            output = characterOf(parts[0]).concat(output);
        }
        return output;
    }
    public int[] getParts(int input, int divisor){
        int[] output = new int[2];
        output[0] = input / divisor;
        output[1] = input % divisor;
        return output;
    }
    public String characterOf(int input){
        return Character.toString(characters.charAt(input));
    }
    public int decode(String input, int base){
        int output = 0;
        while(input.length() > 0){
            output += characters.indexOf(input.charAt(0)) * Math.pow(base, input.length() - 1);
            input = input.substring(1);
        }
        return output;
    }
}
