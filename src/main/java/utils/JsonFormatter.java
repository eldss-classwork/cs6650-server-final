package utils;

import data.models.JSONable;
import java.util.List;

/**
 * JsonFormatter provides methods that turn data into JSON formatted strings.
 */
public class JsonFormatter {

    /**
     * Takes a list of JSONable objects and turns it into a JSON list as a String.
     *
     * @param list a list of JSONable objects
     * @return a JSON array as a String
     */
    public static String buildArray(List<JSONable> list) {
        StringBuilder builder = new StringBuilder();

        // Starting braces
        builder.append("[");

        // Objects
        JSONable obj;
        String json;
        for (int i = 0; i < list.size() - 1; i++) {
            // Get all but last object, these have a comma at the end
            obj = list.get(i);
            json = obj.fieldsToJSON();
            builder.append(json);
            builder.append(",");
        }
        // Add last object w/o comma
        obj = list.get(list.size() - 1);
        json = obj.fieldsToJSON();
        builder.append(json);

        // Ending braces
        builder.append("]");

        return builder.toString();
    }

    /**
     * Sends a simple JSON formatted error message. Message looks like {"message": "error message"}
     *
     * @param message a message to send
     * @return A JSON formatted error message
     */
    public static String buildError(String message) {
        return "{\"message\": \"" + message + "\"}";
    }
}
