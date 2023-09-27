package Gal_Mafa_CFos_Tools;

import Orion.Toolbox.Tools;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.image3d.ImageHandler;


/**
 * @author ORION-CIRB
 */
public class Process {
    
    public Orion.Toolbox.Tools tools = new Tools();
    public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    private final String helpUrl = "https://github.com/orion-cirb/Gal_Mafa_CFos.git";
    
    String[] chNames = {"Gal", "Mafa", "CFos"};
    public Calibration cal = new Calibration();
    public double pixVol;
     
    // Gal Mafa and CFos detection
    public File stardistModelsPath = new File(IJ.getDirectory("imagej")+File.separator+"models");
    public String stardistModel = "StandardFluo.zip"; 
    public final double stardistPercentileBottom = 0.2;
    public final double stardistPercentileTop = 99.8;
    public final double stardistProbThresh = 0.75;
    public final double stardistOverlapThresh = 0.2;
    public Object syncObject = new Object();
    public String stardistOutput = "Label Image";
    public double minGalVol = 100;
    
    
    
   
    
    /**
     * Generate dialog box
     */
    public String[] dialog(String imagesDir, String[] channels) {
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 40, 0);
        gd.addImage(icon);
        
        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        int index = 0;
        for (String chName: chNames) {
            gd.addChoice(chName+": ", channels, channels[index]);
            index++;
        }
        
        gd.addMessage("Cells detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min Gal volume (µm3): ", minGalVol, 2);
        
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY calibration (µm): ", cal.pixelHeight, 3);
        gd.addNumericField("Z calibration (µm): ", cal.pixelDepth, 3);
        gd.addHelp(helpUrl);
        gd.showDialog();
        
        String[] chChoices = new String[chNames.length];
        for (int n = 0; n < chChoices.length; n++) 
            chChoices[n] = gd.getNextChoice();
        
        minGalVol = gd.getNextNumber();
        cal.pixelHeight = cal.pixelWidth = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        pixVol = cal.pixelHeight*cal.pixelWidth*cal.pixelDepth;
        
        if (gd.wasCanceled())
            chChoices = null;
        return(chChoices);
    }
    
    
    /**
     * Compute parameters of each cell in population
     * @param cells
     * @param galPop
     * @param imgGal
     * @param imgMafa
     * @param imgCfos
     * @param galBg
     * @param mafaBg
     */
    public void computeParams(ArrayList<Cell> cells, Objects3DIntPopulation galPop, ImagePlus imgGal, ImagePlus imgMafa, ImagePlus imgCfos, double galBg, double mafaBg, double cfosBg) {
        ImageHandler imhGal = ImageHandler.wrap(imgGal);
        ImageHandler imhMafa = ImageHandler.wrap(imgMafa);
        ImageHandler imhCfos = ImageHandler.wrap(imgCfos);
        for (Object3DInt galCell : galPop.getObjects3DInt()) {
            Cell cell = new Cell();
            cell.setGal(galCell);
            cells.add(cell);
        }
        for (Cell cell: cells) {
            float label = cell.getGal().getLabel();
            cell.setParams(label, imhGal, imhMafa, imhCfos, galBg, mafaBg, cfosBg);
        }
    }
    
    
    /**
     * Draw results
     */
    public void drawResults(ArrayList<Cell> cells, ImagePlus imgGal, String name) {
        ImageHandler imhGal = ImageHandler.wrap(imgGal).createSameDimensions();
        
        // Draw Gal pop in green
        for (Cell cell: cells)  {
            int label = cell.getParams().get("label").intValue();
            Object3DInt cellGal = cell.getGal();
            cellGal.drawObject(imhGal, label);
            tools.labelObjectLeftTop(cellGal, imhGal.getImagePlus(), 14);
        }

        ImagePlus[] imgColors = {null, imhGal.getImagePlus(), null, imgGal};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(cal);
        
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(name); 
        imhGal.closeImagePlus();
    }
    
}
