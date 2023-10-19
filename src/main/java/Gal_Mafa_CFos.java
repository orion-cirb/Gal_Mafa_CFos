import Gal_Mafa_CFos_Tools.Cell;
import Gal_Mafa_CFos_Tools.Tools;
import ij.*;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;


/**
* Detect Gal, Mafa and CFos cells and compute their colocalization
* Give volume and intensity of each Gal Mafa+/CFos+ cell
* @author ORION-CIRB
*/
public class Gal_Mafa_CFos implements PlugIn {
    
    private Gal_Mafa_CFos_Tools.Tools tools = new Tools();
       
    public void run(String arg) {
        try {
            if (!tools.checkInstalledModules() || !tools.checkStarDistModels()) {
                return;
            }
            
            String imageDir = IJ.getDirectory("Choose images directory")+File.separator;
            if (imageDir == null) {
                return;
            }
            
            // Find images with fileExt extension
            String fileExt = tools.findImageType(new File(imageDir));
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);
            if (imageFiles.isEmpty()) {
                IJ.showMessage("Error", "No images found with " + fileExt + " extension");
                return;
            }
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);
            
            // Find channel names
            String[] channelNames = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Generate dialog box
            String[] channels = tools.dialog(imageDir, channelNames);
            if (channels == null) {
                IJ.showMessage("Error", "Plugin canceled");
                return;
            }
            
            // Create output folder
            String outDirResults = imageDir + File.separator + "Results_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            // Write headers results for results files
            FileWriter fwResults = new FileWriter(outDirResults + "results.csv", false);
            BufferedWriter results = new BufferedWriter(fwResults);
            results.write("Image name\tImage vol (µm3)\tGal cell label\tGal cell vol (µm3)\tGal bg\tGal cell bg-corr integrated int\t"
                    + "Gal cell bg-corr mean int\tMafa bg\tGal cell bg-corr integrated int in Mafa ch\tGal cell bg-corr mean int in Mafa ch\t"
                    + "CFos bg\tGal cell bg-corr integrated int in CFos ch\tGal cell bg-corr mean int in CFos ch\n");
            results.flush();
            
            for (String f: imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                
                // Analyze Gal channel
                tools.print("- Analyzing Galanin channel -");
                int indexCh = ArrayUtils.indexOf(channelNames, channels[0]);
                ImagePlus imgGal = BF.openImagePlus(options)[indexCh];
                double galBg = tools.findBackground(imgGal);
                // Detect Gal cells
                tools.print("- Detecting Galanin cells -");
                Objects3DIntPopulation galPop = tools.stardistDetection(imgGal);
                tools.print(galPop.getNbObjects() + "Galanin cells found");
                
                // Analyze Mafa channel
                tools.print("- Analyzing Mafa channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[1]);
                ImagePlus imgMafa = BF.openImagePlus(options)[indexCh];
                double mafaBg = tools.findBackground(imgMafa);
                
                // Analyze CFos channel
                tools.print("- Analyzing c-Fos channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[2]);
                ImagePlus imgCfos = BF.openImagePlus(options)[indexCh];
                double cfosBg = tools.findBackground(imgCfos);
                
                // Compute parameters
                tools.print("- Computing Galanin cells parameters -");
                ArrayList<Cell> cells = tools.computeParams(galPop, imgGal, imgMafa, imgCfos, galBg, mafaBg, cfosBg);
                
                // Write results
                tools.print("- Writing results -");
                double imgVol = imgGal.getWidth() * imgGal.getHeight() * imgGal.getNSlices() * tools.pixVol;
                for (Cell cell: cells) {
                    HashMap<String, Double> params = cell.getParams(); 
                    results.write(rootName+"\t"+imgVol+"\t"+params.get("label")+"\t"+params.get("vol")+"\t"+galBg+"\t"+params.get("galIntSum")+"\t"+
                            params.get("galIntMean")+"\t"+mafaBg+"\t"+params.get("mafaIntSum")+"\t"+params.get("mafaIntMean")+"\t"+
                            cfosBg+"\t"+params.get("cfosIntSum")+"\t"+params.get("cfosIntMean")+"\n");                                
                    results.flush();
                }
                
                // Draw results
                tools.print("- Drawing results -");
                tools.drawResults(galPop, imgGal, outDirResults+rootName+".tif");
                
                tools.closeImage(imgGal);
                tools.closeImage(imgMafa);
                tools.closeImage(imgCfos);
            }
            results.close();
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(Gal_Mafa_CFos.class.getName()).log(Level.SEVERE, null, ex);
        }
        tools.print("All done!");
    }
}
