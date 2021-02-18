/**
 * Feature collection Json object.
 * @author Dan Skaf
 */
import java.util.*;
import org.json.simple.*;
@SuppressWarnings({"unchecked","unsafe","serial"})
public class FeatureCollection extends JSONObject{
    JSONArray features = new JSONArray();
    public FeatureCollection(){
        this.put("type", "FeatureCollection");
        this.put("features",features);
    }

    public void addFeature(JSONObject feature){
        features.add(feature);
    }
    
}