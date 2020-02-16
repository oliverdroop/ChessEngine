package mainpackage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public class Visualizer {
    private static final Logger LOGGER = Logger.getLogger(Visualizer.class);
    String path = "D:\\Documents and Settings\\Oliver\\My Documents\\My Pictures\\Computer Art\\Number Images\\";
    public void drawSpiral(List<Integer> numberList){
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.drawLine(0, 0, 19, 19);
        savePicture(img, path + "myImage.bmp");
        LOGGER.info("Saved picture");
    }
    
    public void savePicture(BufferedImage img, String path){
        try{
            FileWriter fw = new FileWriter(path);
            File outputFile = new File(path);
            ImageIO.write(img, "bmp", outputFile);
                
        }
        catch(IOException ioe){
            LOGGER.info(ioe.getMessage());
        }
    }
}
