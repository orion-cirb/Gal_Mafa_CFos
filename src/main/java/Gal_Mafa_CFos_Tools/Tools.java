package Gal_Mafa_CFos_Tools;

import Gal_Mafa_CFos_Tools.StardistOrion.StarDist2D;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.ImageIcon;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;


/**
 * @author ORION-CIRB
 */
public class Tools {
    
    public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    private final String helpUrl = "https://github.com/orion-cirb/Gal_Mafa_CFos.git";
    
    public Calibration cal = new Calibration();
    public double pixVol;
    String[] chNames = {"Galanin", "Mafa", "cFos"};
     
    public File stardistModelsPath = new File(IJ.getDirectory("imagej")+File.separator+"models");
    public String stardistModel = "DSB2018.zip"; 
    public final double stardistPercentileBottom = 0.2;
    public final double stardistPercentileTop = 99.8;
    public final double stardistProbThresh = 0.75;
    public final double stardistOverlapThresh = 0.2;
    
    public double minGalVol = 30;
    public double maxGalVol = 500;
    
    public double minCfosInt = 6;
    public double minMafaInt = 20;
    
    

    /**
     * Display a message in the ImageJ console and status bar
     */
    public void print(String log) {
        System.out.println(log);
        IJ.showStatus(log);
    }
    
    
    /**
     * Flush and close an image
     */
    public void closeImage(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Check that needed modules are installed
     */
    public boolean checkInstalledModules() {
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("mcib3d.geom2.Object3DInt");
        } catch (ClassNotFoundException e) {
            IJ.showMessage("Error", "3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /**
     * Check that required StarDist model is present in Fiji models folder
     */
    public boolean checkStarDistModels() {
        FilenameFilter filter = (dir, name) -> name.endsWith(".zip");
        File[] modelList = stardistModelsPath.listFiles(filter);
        int index = ArrayUtils.indexOf(modelList, new File(stardistModelsPath+File.separator+stardistModel));
        if (index == -1) {
            IJ.showMessage("Error", stardistModel + " StarDist model not found, please add it in Fiji models folder");
            return false;
        }
        return true;
    }
    
    
    /**
     * Find images extension
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
               case "nd" :
                   ext = fileExt;
                   break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "ics" :
                    ext = fileExt;
                    break;
                case "ics2" :
                    ext = fileExt;
                    break;
                case "lsm" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
                case "tiff" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
    
    
    /**
     * Find images in folder
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No image found in " + imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt) && !f.startsWith("."))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
       
    
    /**
     * Find image calibration
     */
    public void findImageCalib(IMetadata meta) {
        cal = new Calibration();
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue(); 
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth + ", Z calibration = " + cal.pixelDepth);
    }
    
    
      /**
     * Find channels name and None to end of list
     * @param imageName
     * @param meta
     * @param reader
     * @return 
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public String[] findChannels (String imageName, IMetadata meta, ImageProcessorReader reader) throws DependencyException, ServiceException, FormatException, IOException {
        int chs = reader.getSizeC();
        String[] channels = new String[chs];
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = (meta.getChannelName(0, n).toString().equals("")) ? Integer.toString(n) : meta.getChannelName(0, n).toString();
                break;
            case "nd2" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = (meta.getChannelName(0, n).toString().equals("")) ? Integer.toString(n) : meta.getChannelName(0, n).toString();
                break;
            case "lif" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n).toString();
                break;
            case "czi" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = (meta.getChannelFluor(0, n).toString().equals("")) ? Integer.toString(n) : meta.getChannelFluor(0, n).toString();
                break;
            case "ics" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = meta.getChannelEmissionWavelength(0, n).value().toString();
                break;    
            case "ics2" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = meta.getChannelEmissionWavelength(0, n).value().toString();
                break; 
            default :
                for (int n = 0; n < chs; n++)
                    channels[n] = Integer.toString(n);
        }
        return(channels);     
    }
    
    
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
        
        gd.addMessage("Gal cells detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min cell vol (µm3): ", minGalVol, 2);
        gd.addNumericField("Max cell vol (µm3): ", maxGalVol, 2);
        
        gd.addMessage("Gal cells cFos/Mafa positivity", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min cFos+ cell int: ", minCfosInt, 2);
        gd.addNumericField("Min Mafa+ cell int: ", minMafaInt, 2);
        
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY calibration (µm): ", cal.pixelHeight, 3);
        gd.addNumericField("Z calibration (µm): ", cal.pixelDepth, 3);
        gd.addHelp(helpUrl);
        gd.showDialog();
        
        String[] chChoices = new String[chNames.length];
        for (int n = 0; n < chChoices.length; n++) 
            chChoices[n] = gd.getNextChoice();
        
        minGalVol = gd.getNextNumber();
        maxGalVol = gd.getNextNumber();
        
        minCfosInt = gd.getNextNumber();
        minMafaInt = gd.getNextNumber();
        
        cal.pixelHeight = cal.pixelWidth = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        pixVol = cal.pixelHeight*cal.pixelWidth*cal.pixelDepth;
        
        if (gd.wasCanceled())
            chChoices = null;
        return(chChoices);
    }
    
    
    /**
     * Find image background intensity:
     * Z projection over min intensity + read median intensity
     */
    public double findBackground(ImagePlus img) {
      ImagePlus imgProj = doZProjection(img, ZProjector.MIN_METHOD);
      double bg = imgProj.getProcessor().getStatistics().median;
      System.out.println("Background (median intensity of the min projection) = " + bg);
      closeImage(imgProj);
      return(bg);
    }
    
    
    /**
     * Z-project a stack
     */
    public ImagePlus doZProjection(ImagePlus img, int param) {
        ZProjector zproject = new ZProjector();
        zproject.setMethod(param);
        zproject.setStartSlice(1);
        zproject.setStopSlice(img.getNSlices());
        zproject.setImage(img);
        zproject.doProjection();
       return(zproject.getProjection());
    }
    
    
    /**
     * Apply StarDist 2D slice by slice
     * Label detections in 3D
     * @return objects population
     * @throws java.io.IOException
     */
    public Objects3DIntPopulation stardistDetection(ImagePlus img) throws IOException {
        ImagePlus imgIn = img.resize((int)(img.getWidth()*0.5), (int)(img.getHeight()*0.5), 1, "average");

        // Run StarDist
        File starDistModelFile = new File(stardistModelsPath+File.separator+stardistModel);
        StarDist2D star = new StarDist2D(new Object(), starDistModelFile);
        star.loadInput(imgIn);
        star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistProbThresh, stardistOverlapThresh, "Label Image");
        star.run();

        // Label detections in 3D   
        ImagePlus imgLabels = star.associateLabels().resize(img.getWidth(), img.getHeight(), 1, "none");
        imgLabels.setCalibration(cal);
        Objects3DIntPopulation pop = new Objects3DIntPopulation(ImageHandler.wrap(imgLabels));
        
        popFilterOneZ(pop);
        popFilterSize(pop, minGalVol, maxGalVol);
        pop.resetLabels();

        closeImage(imgIn);
        closeImage(imgLabels);
        return(pop);
    }
    
    
    /**
     * Remove objects present in only one slice from population
     */
    public void popFilterOneZ(Objects3DIntPopulation pop) {
        pop.getObjects3DInt().removeIf(p -> (p.getObject3DPlanes().size() == 1));
    }
    
    
    /**
     * Remove objects with size < min and size > max from population
     */
    public void popFilterSize(Objects3DIntPopulation pop, double min, double max) {
        pop.getObjects3DInt().removeIf(p -> (new MeasureVolume(p).getVolumeUnit()/p.getObject3DPlanes().size() < min) || (new MeasureVolume(p).getVolumeUnit()/p.getObject3DPlanes().size() > max));
    }
    
    
    /**
     * Compute parameters of each object in population
     */
    public ArrayList<Cell> computeParams(Objects3DIntPopulation pop, ImagePlus imgGal, ImagePlus imgMafa, ImagePlus imgCfos, double galBg, double mafaBg, double cfosBg) {
        ArrayList<Cell> cells = new ArrayList<>();
        ImageHandler imhGal = ImageHandler.wrap(imgGal);
        ImageHandler imhMafa = ImageHandler.wrap(imgMafa);
        ImageHandler imhCfos = ImageHandler.wrap(imgCfos);
        
        for (Object3DInt obj: pop.getObjects3DInt()) {
            Cell cell = new Cell();
            cell.setGal(obj);
            cell.setParams(cell.getGal().getLabel(), imhGal, imhMafa, imhCfos, galBg, mafaBg, cfosBg, minMafaInt, minCfosInt);
            cells.add(cell);
        }
        return(cells);
    }
    
    
    /**
     * Draw results
     */
    public void drawResults(Objects3DIntPopulation pop, ImagePlus imgGal, String name) {
        // Draw cells population in green
        ImageHandler imhGal = ImageHandler.wrap(imgGal).createSameDimensions();
        pop.drawInImage(imhGal);
        
        ImagePlus[] imgColors = {null, imhGal.getImagePlus(), null, imgGal};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(cal);
        
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(name); 
        imhGal.closeImagePlus();
    }
    
}
