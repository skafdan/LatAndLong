/**
 * GeoJson - parses input from stdin and writes to GeoJson file.
 * @author Dan Skaf 
 * libraries:
 *  json-simple
 *  Gson
 * Created 2-Feb-2021
 */
import java.util.*;
import java.io.*;
import org.json.simple.*;
import java.math.*;
import java.util.regex.*;
import com.google.gson.*;

@SuppressWarnings({"unchecked","unsafe","serial","deprecation"})
public class GeoJsonApp{
    /**
     * Main function. Creates FeatureCollection and passes input from stdin to 
     * parsing functions.Each valid input is put into a feature which is added 
     * to the feature collection. Finally the JsonObject is printed.  
     * @param args String[] unsed command-line arguments.
     * @return void
     */
    public static void main(String[] args){
        FeatureCollection fc = new FeatureCollection();
        Scanner sc = new Scanner(System.in);
        while(sc.hasNext()){
            String input = sc.nextLine();
            try {
                List<Double> point = parseInput(input);
                fc.addFeature(createFeature(point));
            } catch(Exception e){
                if(e instanceof NullPointerException || 
                   e instanceof NumberFormatException ||
                   e instanceof ArrayIndexOutOfBoundsException ||
                   e instanceof StringIndexOutOfBoundsException){
                    System.out.println("Unable to process: " + input);
                }else {
                    e.printStackTrace();
                }
            }
        }
        toFile("geoJson.json",fc);
    }
    /**
     * Creates a feature with a the valid point. 
     * @param point List<Double> parsed coordinate pair
     * @return feature jsonObject feature.
     */
    public static Feature createFeature(List<Double> point){
       Feature feature;
       feature = new Feature();
       feature.addPoint(point);
       return feature;
    }
    /**
     * Parses input string to valid pair. 
     * @param String str. Input from stdin.
     * @return point List<Double> parsed coordinates.
     */
    public static List<Double> parseInput(String str){
        List<Double> points = new ArrayList<Double>();
        Double lat = -190d;
        Double lng = -190d;
        //removes extra spaces and replaces apostrophes from macos.
        str = str.replaceAll("  "," ");
        str = str.replaceAll("′","'");
        str = str.replaceAll("″","\"");
        //Regex match for degrees minutes seconds
        if(str.matches("[0-9]+\\s?[°d*\\s]\\s?[0-9]+\\s?[m' ]\\s?[0-9]+(\\." +
        "[0-9]+)?\\s?[s\" ]\\s?([nNsSeEwW])?([a-zA-Z]+)?\\s?[, ]?[\\s]?[0-9]+" +
        "\\s?[°d* ]\\s?[0-9]+\\s?[m' ]\\s?[0-9]+(\\.[0-9]+)?\\s?[s\" ]\\s?" +
        "([nNsSeEwW])?([a-zA-Z]+)?")){
            //Simplifies cardinal directions to single letter.
            str = str.replaceAll("[nN]orth","N");
            str = str.replaceAll("[eE]ast","E");
            str = str.replaceAll("[sS]outh","S");
            str = str.replaceAll("[wW]est","W");
            //Removes extra symbols.
            str = str.replaceAll("[°d*m's\"]"," ");
            str = str.replaceAll("  "," ");
            str = str.replaceAll(" ",",");
            str = str.replaceAll(",+",",");
            //Converts to sf.
            str=dmsToSF(str);
            //Regex match for degrees minutes.
        }else if(str.matches("[0-9]+\\s?[°d*\\s]\\s?[0-9]+(\\.[0-9]+)?\\s?"+
        "[m' ]\\s?([nNsSeEwW])?([a-zA-Z]+)?\\s?[, ]?\\s?[0-9]+\\s?[°d* ]\\s?"+
        "[0-9]+(\\.[0-9]+)\\s?[m' ]\\s?([nNsSeEwW])?([a-zA-Z]+)?")){
            str = str.replaceAll("[eE]ast","E");
            str = str.replaceAll("[sS]outh","S");
            str = str.replaceAll("[wW]est","W");
            str = str.replaceAll("[°d*m']"," ");
            str = str.replaceAll("  "," ");
            str = str.replaceAll(" ",",");
            str = str.replaceAll(",+",",");
            str=dmToSF(str);
        }
        //Regex match for standard form.
        if(str.matches(
            "[-+]?[0-9]+(\\.[0-9]+)?\\s?[,\\s]\\s?[-+]?[0-9]+(\\.[0-9]+)?")){
            str = str.replaceAll(" ", ",");
            str = str.replaceAll(",+",",");
            //splits the x and y
            String[] xy = str.split(",");
            //if no extra symbols assume lat long
            if(xy.length == 2){
               lat = Double.parseDouble(xy[0]);
               lng = Double.parseDouble(xy[1]);
               //Check valid lat long
               if(!validLatLng(lat,lng)){
                   return null;
               }
               //Rounds the output to 6 deciamal places
               Double scale = Math.pow(10,6);
               lat = Math.round(lat * scale)/scale;
               lng = Math.round(lng * scale)/scale;points.add(lng);
               points.add(lat);
            }
            //Regex match for standard form plus cardinal directions and +-.
        } else if (str.matches("[-+]?[0-9]+(.[0-9]+)?\\s?([a-zA-Z]+)?[, ]"+
        "\\s?[-+]?[0-9]+(.[0-9]+)?\\s?([a-zA-Z]+)?") 
            && cardDirections(str = cleanCards(str))){
            str = str.replaceAll(" ", ",");
            str = str.replaceAll(",+",",");
            String[] xy = str.split(",");
            xy = processCards(xy);
            if(xy.length == 3){
                if(xy[1].matches("[NS]")){
                    lat = Double.parseDouble(xy[0]);
                    lng = Double.parseDouble(xy[2]);
                }else if (xy[2].matches("[EW]")){
                    lat = Double.parseDouble(xy[0]);
                    lng = Double.parseDouble(xy[1]);
                }else if (xy[1].matches("[EW]")){
                    lat = Double.parseDouble(xy[2]);
                    lng = Double.parseDouble(xy[0]);
                }else if (xy[2].matches("[NS]")){
                    lat = Double.parseDouble(xy[1]);
                    lng = Double.parseDouble(xy[0]);
                }
            } else {
                for(int i = 0; i < xy.length; i++){
                    if(xy[i].matches("[NS]")){
                       lat = Double.parseDouble(xy[i-1]);
                    } else if (xy[i].matches("[EW]")){
                        lng = Double.parseDouble(xy[i-1]);
                    }
                }
            }
            for(int i = 0; i < xy.length; i++){
                if(xy[i].matches("S")){
                    lat *= -1;
                } else if(xy[i].matches("W")){
                    lng *= -1;
                }
            }
            if(!validLatLng(lat,lng)){
               return null; 
            }
            Double scale = Math.pow(10,6);
            lat = Math.round(lat * scale)/scale;
            lng = Math.round(lng * scale)/scale;
            points.add(lng);
            points.add(lat);
        }
        if(points.size() <= 1){
            return null;
        }
        System.out.println(points.toString());
        return points;

    }
    /**
     * Processes the cardinal directions.Returns null if NS or NS, EW or EW.
     * @param xy String[] pair of coordinates.
     * @return xy String[] processed pair.
     */ 
    public static String[] processCards(String[] xy){
        for(int i = 0; i < xy.length; i++){
            String str = Character.toString(xy[i].charAt(0));
            if(str.matches("[a-zA-z]")){
                xy[i] = Character.toString(xy[i].charAt(0)).toUpperCase();
            }
        }

        if(xy.length == 4){
            if(xy[1].matches("[NS]") && xy[3].matches("[NS]")){
                return null;
            } else if (xy[1].matches("[EW]") && xy[3].matches("[EW]")){
                return null;
            }
        }
        return xy;
    }
    /**
     * Cleans coordinates. Inserts space between direction and number.
     * @param direction inputed cardinal direction.
     * @return String cleaned direction.
     */
    public static String cleanCards(String direction){
        StringBuffer str; 
        String[] directionAsArray = direction.split(",| ");
        for(int i = 0; i < directionAsArray.length; i++){
            str = new StringBuffer(directionAsArray[i]);
            String dir = directionAsArray[i];
            Pattern p = Pattern.compile("\\p{Alpha}");
            Matcher m = p.matcher(dir);
            if(m.find()){
                str.insert(m.start()," ");
                directionAsArray[i] = str.toString();
            }
        }
        direction = "";
        for(int i = 0; i < directionAsArray.length; i++){
            direction += directionAsArray[i] + " ";
        }
        return direction;
    }
    /**
     * Checks if coordinate has a valid cardinal direction.
     * @param direction inputed string.
     * @return Boolean true or false if valid.
     */
    public static Boolean cardDirections(String direction){
        List<String> directions = Arrays.asList("s","S","south","South",
            "n","N","north","North","e","E","East","east",
            "w","W","west","West");
        for(String dir: directions){
            if(direction.contains(dir)){
                return true;
            }
        }
        return false;
    }
    /**
     * Checks that the two inputed cardinal directions are not  both NS or EW. 
     * @param x String - x coordinate string
     * @param y String - y coordinate string
     * @return Boolean true if duplicate, false if valid.
     */
    public static Boolean duplicateBearings(String x, String y){
        if(x.matches("[sSnN]") && y.matches("[sSnN]")){
            return true;
        }else if(x.matches("[eEwW]") && y.matches("[eEwW]")){
            return true;
        }else {
            return false;
        }
    }
    /**
     * checks values for lat and lng.  -90 < lat < 90. -180 < lng < 180.
     * @param lat Double - latitude.
     * @param lng Double - longitude.
     * @return Boolean true if valid, false if not.
     */
    public static Boolean validLatLng(double lat, double lng){
        if(lat <= 90 && lat >= -90){
            if(lng <= 180 && lng >= -180){
                return true;
            }
        }
        return false;
    }
    /**
     * Converts dms string to standard form 
     * @param input String - input from stdin
     * @return String - converted SF.
     */
    public static String dmsToSF(String input){
        Double x = 190d;
        Double y = 190d;
        String output = "";
        //Splits the x and y coordinate.
        String[] inputArray = input.split(",");
        List<Double> components = new ArrayList<Double>();
        //convert every number to float and add to components. 
        //If its a SW direction add a -1.
        for(String s:inputArray){
            try{
                components.add(Double.parseDouble(s));
            }catch(Exception e){
                if(e instanceof NumberFormatException){
                    if(s.matches("[SW]")){
                        components.add(-1d);
                    }else {
                        components.add(1d);
                    }
                }
            }
        }
        //DD*MM'SS.SS" N DD*MM'SS.SS" E
        if(inputArray.length == 8){//convert to SF + with direction(-+)
            if(duplicateBearings(inputArray[3],inputArray[7])){
                return null;
            }
            x = (components.get(0) + components.get(1)/60 
                +components.get(2)/3600 ) * components.get(3);
            y = (components.get(4) + components.get(5)/60 
                +components.get(6)/3600 ) * components.get(7);
        //Only one cardinal direction
        }else if (inputArray.length == 7){
            if(inputArray[3].matches("[SE]")){
                x = (components.get(0) + components.get(1)/60 
                    + components.get(2)/3600) * components.get(3);
                y = (components.get(4) + components.get(5)/60
                    + components.get(6)/3600);
            }else if(inputArray[6].matches("[SE]")){
                x = (components.get(0) + components.get(1)/60
                    + components.get(2)/3600);
                y = (components.get(3) + components.get(4)/60
                    + components.get(5)/3600) * components.get(6);
            }
        //No cardinal directions
        }else if (inputArray.length == 6){
            x = components.get(0) + components.get(1)/60 
                + components.get(2)/3600;
            y = components.get(3) + components.get(4)/60 
                + components.get(5)/3600;
        }
        //Rounding
        Double scale = Math.pow(10, 6);
        x = Math.round(x * scale) / scale;
        y = Math.round(y * scale) / scale;
        //Rearrange order of xy bassed on NS EW
        if(inputArray.length == 8){
            if(inputArray[3].matches("[NS]")){
                output = Double.toString(x) + "," + Double.toString(y);
            }else if(inputArray[7].matches("[NS]")){
                output = Double.toString(y) + "," + Double.toString(x);
            }
        }else if (inputArray.length == 7){
            if(inputArray[3].matches("[NS]") || inputArray[6].matches("[EW]")){
                output = Double.toString(x) + "," + Double.toString(y);
            }else if (inputArray[6].matches("[NS]") 
                || inputArray[3].matches("[EW]")){
                output = Double.toString(y) + "," + Double.toString(x);
            }
        }else {
            output = Double.toString(x) + "," + Double.toString(y);
        }
        return output;
    }
    /**
     * Converts Degrees minutes to SF.
     * @param input String - input from stdin
     * @return - converted input to SF
     */
    public static String dmToSF(String input){
        Double x = 190d;
        Double y = 190d;
        String output = "";
        //Splits the x and y coordinate.
        String[] inputArray = input.split(",");
        List<Double> components = new ArrayList<Double>();
        //convert every number to float and add to components.
        //If its a SW direction add a -1.
        for(String s:inputArray){
            try{
                components.add(Double.parseDouble(s));
            }catch(Exception e){
                if(e instanceof NumberFormatException){
                    if(s.matches("[SW]")){
                        components.add(-1d);
                    }else {
                        components.add(1d);
                    }
                }
            }
        }
        //DD*MM.MM' N DD*MM.MM' E
        if(inputArray.length == 6){//Convert to SF + with direction (-+)
            if(duplicateBearings(inputArray[2],inputArray[5])){
                return null;
            }
            x = (components.get(0) + components.get(1)/60) * components.get(2);
            y = (components.get(3) + components.get(4)/60) * components.get(5);
        //Only one cardinal direction
        }else if(inputArray.length == 5) {
            if(inputArray[2].matches("[NS]")){
                x = (components.get(0) + components.get(1)/60) * components.get(2);
                y = (components.get(3) + components.get(4)/60);
            }else if (inputArray[4].matches("[NS]")){
                x = (components.get(0) + components.get(1)/60);
                y = (components.get(2) + components.get(3)/60) * components.get(4);
            }
        //No cardinal directions.
        }else if(inputArray.length == 4){
            x = components.get(0) + components.get(1)/60;
            y = components.get(2) + components.get(3)/60;
        }
        //Rounding 
        Double scale = Math.pow(10,6);
        x = Math.round(x * scale) / scale;
        y = Math.round(y * scale) / scale;
        //Rearrange order of xy based on NS EW
        if(inputArray.length == 6){
            if(inputArray[2].matches("[NS]")){
                output = Double.toString(x) + "," + Double.toString(y);
            }else if(inputArray[5].matches("[NS]")){
                output = Double.toString(y) + "," + Double.toString(x);
            }
        }else if (inputArray.length == 5){
            if(inputArray[2].matches("[NS]") || inputArray[5].matches("EW")){
                output = Double.toString(x) + "," + Double.toString(y);
            }else if(inputArray[5].matches("[NS]") 
                || inputArray[2].matches("[EW]")){
                    output = Double.toString(y) + "," + Double.toString(x);
            }
        }else {
            output = Double.toString(x) + "," + Double.toString(y); 
        }
        return output;
    }    

    /**
     * writes the JsonObject to a file.
     * @param name String - File name.
     * @param obj JSONObject - JsonObject to print
     */
    public static void toFile(String name, JSONObject obj){
        try(FileWriter file = new FileWriter(name)){
            //Gson deserializes the Json toString method 
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(obj.toJSONString());
            file.write(gson.toJson(je));
            file.flush(); 
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}