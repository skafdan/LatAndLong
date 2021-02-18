/**
 * Feature class - Json object
 * @author Dan Skaf
 */
import java.util.*;
import org.json.simple.*;

@SuppressWarnings({"unchecked","unsafe","serial"})
public class Feature extends JSONObject{
    public Feature(){
        this.put("type","Feature");
        JSONObject properties = new JSONObject();
        this.put("properties",properties);
    }
    public void addPoint(List<Double> point){
        Geometry gpoint = new Geometry("Point",point);
        this.put("geometry",gpoint);
    }
}