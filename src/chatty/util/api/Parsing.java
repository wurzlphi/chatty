
package chatty.util.api;

import chatty.Room;
import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Some more simple parsing.
 * 
 * @author tduva
 */
public class Parsing {
    
    private static final Logger LOGGER = Logger.getLogger(Parsing.class.getName());
    
    /**
     * Parse the list of games that was returned by the game search.
     * 
     * @param json
     * @return 
     */
    public static Set<StreamCategory> parseCategorySearch(String json) {
        Set<StreamCategory> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            Object data = root.get("data");
            
            if (!(data instanceof JSONArray)) {
                LOGGER.warning("Error parsing game search: Should be array");
                return null;
            }
            Iterator it = ((JSONArray)data).iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof JSONObject) {
                    JSONObject categoryData = (JSONObject) obj;
                    String id = JSONUtil.getString(categoryData, "id");
                    String name = JSONUtil.getString(categoryData, "name");
                    if (!StringUtil.isNullOrEmpty(id, name)) {
                        result.add(new StreamCategory(id, name));
                    }
                }
            }
            return result;
            
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing game search.");
            return null;
        } catch (NullPointerException ex) {
            LOGGER.warning("Error parsing game search: Unexpected null");
            return null;
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing game search: Unexpected type");
            return null;
        }
    }
    
    public static long followGetTime(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            long time = DateTime.parseDatetime((String)root.get("created_at"));
            return time;
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /**
     * Parses the JSON returned from the TwitchAPI that contains the token
     * info into a TokenInfo object.
     * 
     * @param json
     * @return The TokenInfo or null if an error occured.
     */
    public static TokenInfo parseVerifyToken(String json) {
        if (json == null) {
            LOGGER.warning("Error parsing verify token result (null)");
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject token = (JSONObject) parser.parse(json);
            
            String username = (String)token.get("login");
            String id = (String)token.get("user_id");
            String client_id = JSONUtil.getString(token, "client_id");
            JSONArray scopes = (JSONArray)token.get("scopes");
            long expiresIn = JSONUtil.getLong(token, "expires_in", -1);
            return new TokenInfo(username, id, client_id, scopes, expiresIn);
        }
        catch (Exception e) {
            return null;
        }
    }

}
