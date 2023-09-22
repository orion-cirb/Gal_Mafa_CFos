package Gal_Mafa_CFos_Tools;

import java.util.HashMap;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;


/**
 * @author ORION-CIRB
 */
public class Cell {
    private Object3DInt gal;
    private HashMap<String, Double> params;
    

    public Cell() {
        this.gal = gal;
        this.params = new HashMap<>();
    }
    
    public void setGal(Object3DInt gal) {
        this.gal = gal;
    }
    
    public Object3DInt getGal() {
        return(gal);
    }
    
    
    public HashMap<String, Double> getParams() {
        return params;
    }
    
    
    public void setParams(double label, ImageHandler imhGal, ImageHandler imhMafa, ImageHandler imhCfos, double galBg, double mafaBg, double cfosBg) {
        params.put("label", label);
        double galVol = new MeasureVolume(this.gal).getVolumeUnit();
        double galInt  = new MeasureIntensity(this.gal, imhGal).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
        double galIntCor = galInt - galBg * new MeasureVolume(this.gal).getVolumePix();
        params.put("galVol", galVol);
        params.put("galIntCor", galIntCor);
        double mafaInt = new MeasureIntensity(this.gal, imhMafa).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
        double mafaIntCor = mafaInt - mafaBg * new MeasureVolume(this.gal).getVolumePix();
        params.put("mafaIntCor", mafaIntCor);
        
        double cfosInt = new MeasureIntensity(this.gal, imhCfos).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
        double cfosIntCor = cfosInt - cfosBg * new MeasureVolume(this.gal).getVolumePix();
        params.put("cfosIntCor", cfosIntCor);  
    }
    
}