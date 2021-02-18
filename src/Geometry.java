/**
 * Geometry object.
 * @author Dan Skaf
 */
import java.util.*;
import org.json.simple.*;

@SuppressWarnings({"unchecked","unsafe","serial"})
public class Geometry extends JSONObject{
    String type;
    JSONArray points = new JSONArray();
    public Geometry(String type,List<Double> point){
        this.put("type","Point");
        this.points.addAll(point);
        this.put("coordinates",this.points);
    }
}