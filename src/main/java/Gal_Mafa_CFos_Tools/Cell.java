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
    private Object3DInt cell;
    private HashMap<String, Double> params;
    

    public Cell() {
        this.params = new HashMap<>();
    }
    
    public void setGal(Object3DInt cell) {
        this.cell = cell;
    }
    
    public Object3DInt getGal() {
        return(this.cell);
    }
       
    public void setParams(double label, ImageHandler imhGal, ImageHandler imhMafa, ImageHandler imhCfos, double galBg, double mafaBg, double cfosBg, double minMafaInt, double minCfosInt) {
        double volUnit = new MeasureVolume(this.cell).getVolumeUnit();
        double volPix =  new MeasureVolume(this.cell).getVolumePix();
        double galIntSum  = new MeasureIntensity(this.cell, imhGal).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - galBg * volPix;
        double galIntMean  = new MeasureIntensity(this.cell, imhGal).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - galBg;
        double mafaIntSum = new MeasureIntensity(this.cell, imhMafa).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - mafaBg * volPix;
        double mafaIntMean  = new MeasureIntensity(this.cell, imhMafa).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - mafaBg;
        double mafaPos = (mafaIntMean > minMafaInt)? 1 : 0;
        double cfosIntSum = new MeasureIntensity(this.cell, imhCfos).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - cfosBg * volPix;
        double cfosIntMean  = new MeasureIntensity(this.cell, imhCfos).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - cfosBg;
        double cfosPos = (cfosIntMean > minCfosInt)? 1 : 0;
        
        params.put("label", label);
        params.put("vol", volUnit);
        params.put("galIntSum", galIntSum);
        params.put("galIntMean", galIntMean);
        params.put("mafaIntSum", mafaIntSum);
        params.put("mafaIntMean", mafaIntMean);
        params.put("mafaPos", mafaPos);
        params.put("cfosIntSum", cfosIntSum);
        params.put("cfosIntMean", cfosIntMean);
        params.put("cfosPos", cfosPos);
    }
    
    public HashMap<String, Double> getParams() {
        return this.params;
    }
}