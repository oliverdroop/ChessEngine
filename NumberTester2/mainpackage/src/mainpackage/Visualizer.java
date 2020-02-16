package mainpackage;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public class Visualizer {
    
    private static final Logger LOGGER = Logger.getLogger(Visualizer.class);
    
    private String path = "D:\\Documents and Settings\\Oliver\\My Documents\\My Pictures\\Computer Art\\Number Images\\";
    
    public void drawSquareSpiral(List<Integer> numberList, Class sequencerClass){
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        int x = (img2.getWidth() / 2) - 1;
        int y = (img2.getHeight() / 2) - 1;
        int count = 1;
        double angle = 0;
        List<Point> coveredPoints = new ArrayList<>();
        while(count <= img2.getWidth() * img2.getHeight()){
            if (numberList.contains(count)){
                img2.setRGB(x, y, 0xFFFFFF);
            }
            coveredPoints.add(new Point(x, y));
            angle += (Math.PI / 2);
            int nx = x + (int)Math.round(Math.sin(angle));
            int ny = y - (int)Math.round(Math.cos(angle));
            Point p2 = new Point(nx, ny);
            if (coveredPoints.contains(p2)){
                angle -= (Math.PI / 2);
                nx = x + (int)Math.round(Math.sin(angle));
                ny = y - (int)Math.round(Math.cos(angle));
            }
            if (angle >= 2 * Math.PI){
                angle -= 2 * Math.PI;
            }
            x = nx;
            y = ny;
            count ++;
        }
        Graphics g = img.getGraphics();
        g.drawImage(img2, 0, 0, null);
        savePicture(img, path + sequencerClass.getSimpleName() + "SquareSpiral" + ".bmp");
        LOGGER.info("Saved picture");
    }
    
    public void drawBlock(List<Integer> numberList, Class sequencerClass){
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for(int y = 0; y < img2.getHeight(); y++){
            for(int x= 0; x < img2.getWidth(); x++){
                int pixelNum = (y * img2.getWidth() + x) + 1;
                if (numberList.contains(pixelNum)){
                    img2.setRGB(x, y, 0xFFFFFF);
                }
            }
        }
        Graphics g = img.getGraphics();
        g.drawImage(img2, 0, 0, null);
        savePicture(img, path + sequencerClass.getSimpleName() + "Block" + ".bmp");
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
