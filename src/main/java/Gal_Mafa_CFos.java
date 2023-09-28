import Gal_Mafa_CFos_Tools.Cell;
import Gal_Mafa_CFos_Tools.Process;
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
    
    private Gal_Mafa_CFos_Tools.Process proc = new Process();
       
    public void run(String arg) {
        try {
            if (!proc.tools.checkInstalledModules("mcib3d.geom2.Object3DInt", "mcib3d") || !proc.tools.checkStarDistModels(proc.stardistModel)) {
                return;
            }
            
            String imageDir = IJ.getDirectory("Choose images directory")+File.separator;
            if (imageDir == null) {
                return;
            }
            
            // Find images with fileExt extension
            String fileExt = proc.tools.findImageType(new File(imageDir));
            ArrayList<String> imageFiles = proc.tools.findImages(imageDir, fileExt);
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
            proc.cal = proc.tools.findImageCalib(meta);
            
            // Find channel names
            String[] channelNames = proc.tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Generate dialog box
            String[] channels = proc.dialog(imageDir, channelNames);
            if (channels == null) {
                IJ.showStatus("Plugin canceled");
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
            results.write("Image name\tImage vol (µm3)\tGal cell label\tGal vol (µm3)\tGal bg\tGal integrated int bg corrected\tCFos bg\t"
                + "Gal integrated int bg corrected in Cfos channel\tMafa bg\tGal integrated int bg corrected in Mafa channel\n");
            results.flush();
            
            for (String f: imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                proc.tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                
                // Analyze Gal channel
                proc.tools.print("- Analyzing Gal channel -");
                int indexCh = ArrayUtils.indexOf(channelNames, channels[0]);
                ImagePlus imgGal = BF.openImagePlus(options)[indexCh];
                double galBg = proc.tools.findBackground(imgGal, null, "median");
                Objects3DIntPopulation galPop = proc.tools.stardistObjectsPop(imgGal, 1, false, 0, proc.stardistModel, proc.stardistProbThresh, 
                        proc.stardistOverlapThresh, false);
                proc.tools.popFilterOneZ(galPop);
                proc.tools.popFilterSize(galPop, proc.minGalVol, Double.MAX_VALUE);
                proc.tools.print(galPop.getNbObjects() + "Galanin cells found");
                
                // Analyze Mafa channel
                proc.tools.print("- Analyzing Mafa channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[1]);
                ImagePlus imgMafa = BF.openImagePlus(options)[indexCh];
                double mafaBg = proc.tools.findBackground(imgMafa, null, "median");
                
                // Analyze C-fos channel
                proc.tools.print("- Analyzing c-Fos channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[2]);
                ImagePlus imgCfos = BF.openImagePlus(options)[indexCh];
                double cfosBg = proc.tools.findBackground(imgCfos, null, "median");
                
                // Compute parameters
                proc.tools.print("- Compute parameters for Galanin cells -");
                ArrayList<Cell> cells = new ArrayList<>();
                proc.computeParams(cells, galPop, imgGal, imgMafa, imgCfos, galBg, mafaBg, cfosBg);
                
                // Write results
                proc.tools.print("- Writing and drawing results -");
                double imgVol = imgGal.getWidth() * imgGal.getHeight() * imgGal.getNSlices() * proc.pixVol;
                for (Cell cell: cells) {
                    HashMap<String, Double> params = cell.getParams(); 
                    results.write(rootName+"\t"+imgVol+"\t"+params.get("label")+"\t"+params.get("galVol")+"\t"+galBg+"\t"+params.get("galIntCor")+
                            "\t"+cfosBg+"\t"+params.get("cfosIntCor")+"\t"+mafaBg+"\t"+params.get("mafaIntCor")+"\n");                                
                    results.flush();
                }
                
                // Draw results
                proc.drawResults(cells, imgGal, outDirResults+rootName+"_Objects.tif");
                
                proc.tools.flush_close(imgGal);
                proc.tools.flush_close(imgMafa);
                proc.tools.flush_close(imgCfos);
            }
            results.close();
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(Gal_Mafa_CFos.class.getName()).log(Level.SEVERE, null, ex);
        }
        proc.tools.print("All done!");
    }
}
