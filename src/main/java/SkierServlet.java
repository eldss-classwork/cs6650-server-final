import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import data.SkierDbConnection;
import data.models.JSONable;
import exceptions.EmptyPathException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.json.JSONException;
import org.json.JSONObject;
import utils.JsonFormatter;
import utils.RabbitMQChannelFactory;

@WebServlet(name = "SkierServlet")
public class SkierServlet extends HttpServlet {

    private final SkierDbConnection dbConn = new SkierDbConnection();
    // RabbitMQ objects
    private final static String QUEUE_NAME = "DB_POST_DURABLE";
    private Connection rmqConn;
    private ObjectPool<Channel> channelPool;

    /**
     * Used to set up RabbitMQ. Does not use the config object.
     */
    public void init(ServletConfig config) throws ServletException  {
        super.init(config);

        // Connection setup
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.90.78.126");
        //factory.setHost("localhost");  // for local testing

        try {
            rmqConn = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            throw new ServletException("couldn't connect to RabbitMQ");
        }

        // Channel pool setup
        channelPool= new GenericObjectPool<>(new RabbitMQChannelFactory(rmqConn, QUEUE_NAME));
    }

    /**
     * Cleans up RabbitMQ leftovers.
     */
    public void destroy() {
        try {
            rmqConn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channelPool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle POST requests.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Process path
        String[] pathParts;
        try {
            pathParts = ServletMethodSetup.setUp(request, response);
        } catch (EmptyPathException e) {
            // Error response taken care of in setup
            return;
        }

        // Get the body JSON
        String requestData = request.getReader().lines().collect(Collectors.joining());
        JSONObject requestJson = new JSONObject(requestData);

        // Validate path
        if (!isUrlValidPOST(pathParts, requestJson)) {
            // Invalid path
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(JsonFormatter.buildError("invalid request"));
            return;
        }

        // Collect the json arguments
        final String[] keys = new String[]{
                "resortID",
                "dayID",
                "skierID",
                "time",
                "liftID"
        };
        // Already validated
        String resort = requestJson.getString(keys[0]);
        // Don't need to convert to ints because these will be sent
        // to a queue anyways.
        String day = requestJson.getString(keys[1]);
        String skier = requestJson.getString(keys[2]);
        String time = requestJson.getString(keys[3]);
        String lift = requestJson.getString(keys[4]);

        // Do the request and write response
        PrintWriter out = response.getWriter();
        try {
            // Get and declare a channel
            Channel channel = channelPool.borrowObject();

            // Keep the message as simple as possible, just the arguments.
            // Consumers will know the schema here.
            String message = String.format(
                    "%s,%s,%s,%s,%s", resort, day, skier, time, lift);
            // Send it
            channel.basicPublish(
                    "",
                    QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes());

            // Return the channel to the pool
            channelPool.returnObject(channel);

            // Send a successful response
            response.setStatus(HttpServletResponse.SC_CREATED);

        } catch (Exception e) {
            // Error with RabbitMQ
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(JsonFormatter.buildError("problem pushing to worker queue: "
                    + e.getMessage()));  // For development
        }
    }

    /**
     * Handle GET requests.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Process path
        String[] pathParts;
        try {
            pathParts = ServletMethodSetup.setUp(request, response);
        } catch (EmptyPathException e) {
            // Error response taken care of in setup
            return;
        }

        // Get query string
        String queryStr = request.getQueryString();
        Map<String, String> queries = queryStringToMap(queryStr);

        // Validate path
        if (!isUrlValidGET(pathParts, queries)) {
            // Invalid path
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(JsonFormatter.buildError("invalid request"));
            return;
        }

        // Process the request
        PrintWriter out = response.getWriter();
        try {
            String pathIdVertWithDays = "days";
            String pathIdVertNoDays = "vertical";
            int pathIdIndex = 2;

            // /skiers/{resortID}/days/{dayID}/skiers/{skierID}
            if (pathParts[pathIdIndex].equals(pathIdVertWithDays)) {
                // Get the data
                String resort = pathParts[1];
                int day = Integer.parseInt(pathParts[3]);
                int skierID = Integer.parseInt(pathParts[5]);
                JSONable vertObj = dbConn.getSkierVertByResortAndDay(skierID, day, resort);

                // Return the data
                response.setStatus(HttpServletResponse.SC_OK);
                if (vertObj == null) {
                    out.println("{}");
                } else {
                    out.println(vertObj.fieldsToJSON());
                }

                // /skiers/{skierID}/vertical?resort={resortID}
            } else if (pathParts[pathIdIndex].equals(pathIdVertNoDays)) {
                // Get the data
                String resort = queries.get("resort");
                int skierID = Integer.parseInt(pathParts[1]);
                JSONable vertObj = dbConn.getSkierVertByResort(skierID, resort);

                // Return the data
                response.setStatus(HttpServletResponse.SC_OK);
                if (vertObj == null) {
                    out.println("{}");
                } else {
                    out.println(vertObj.fieldsToJSON());
                }

            } else {
                // Something unexpected happened
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(JsonFormatter.buildError("unexpected path provided"));
            }

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(JsonFormatter.buildError("problem executing SQL "
                    + e.getMessage()));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(JsonFormatter.buildError("problem parsing path parameters "
                    + e.getMessage()));
        }
    }

    // Validation methods below
    // Info on paths found at
    // https://app.swaggerhub.com/apis/cloud-perf/SkiDataAPI/1.13#/

    /**
     * Checks if a GET request path is valid.
     *
     * @param pathParts An array of path parts
     * @return true if valid, otherwise false
     */
    private boolean isUrlValidGET(String[] pathParts, Map<String, String> queries) {
        // Example pathParts
        // urlPath  = "/1/days/1/skier/123"
        // pathParts = [, 1, days, 1, skier, 123]

        // Path length constants
        int verticalByResortLen = 3;
        int verticalByDay = 6;

        // Need to adjust the ending index in case the path has a trailing "/'
        // and therefore an empty string as the last array element.
        int len = pathParts.length;
        if (pathParts[len - 1].isEmpty()) {
            --len;
        }

        // Check each path
        try {
            if (len == verticalByResortLen) {
                // Ensure skierID is a number
                String skierID = pathParts[1];
                Integer.parseInt(skierID);

                // Ensure end of path is "vertical"
                boolean pathCondition = pathParts[len - 1].equals("vertical");

                // Ensure query given with "resort"
                boolean queryCondition = queries.containsKey("resort");

                return pathCondition && queryCondition;

            } else if (len == verticalByDay) {
                // Ensure day and skierID are ints
                String day = pathParts[3];
                String skierID = pathParts[5];
                Integer.parseInt(day);
                Integer.parseInt(skierID);

                // Ensure path identifiers are in the right place
                return pathParts[2].equals("days") && pathParts[4].equals("skiers");

            } else {
                // Something unexpected
                return false;
            }
        } catch (NumberFormatException e) {
            // Something wasn't a valid number
            return false;
        }
    }

    /**
     * Checks if a POST request path is valid.
     *
     * @param pathParts An array of path parts
     * @return true if valid, otherwise false
     */
    private boolean isUrlValidPOST(String[] pathParts, JSONObject body) {
        // urlPath  = "/liftrides"
        // pathParts = ["", "liftrides"]
        // Only one POST path

        // Check path is correct
        boolean pathIsValid = false;
        final String resource = "liftrides";
        if (pathParts.length >= 2) {
            pathIsValid = pathParts[1].equals(resource);
        }

        // Check body is correct
        Set<String> keys = body.keySet();
        final String[] requiredKeys = new String[]{
                "resortID",
                "dayID",
                "skierID",
                "time",
                "liftID"
        };

        // Check keys
        boolean bodyIsValid = true;
        for (String key : requiredKeys) {
            if (!keys.contains(key)) {
                bodyIsValid = false;
                break;
            }
        }

        // Check key int values are actually ints
        try {
            // All keys except first are ints
            for (int i = 1; i < requiredKeys.length; i++) {
                body.getInt(requiredKeys[i]);
            }
        } catch (JSONException e) {
            bodyIsValid = false;
        }

        return pathIsValid && bodyIsValid;
    }

    /**
     * Turns a query string into a map of key, value pairs. All keys and values are left as Strings.
     *
     * @param queryStr the string returned by request.getQueryString()
     * @return A map of key value pairs provided by the query
     */
    private Map<String, String> queryStringToMap(String queryStr) {
        if (queryStr == null) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        String[] queries = queryStr.split("&");
        for (String query : queries) {
            String[] keyVal = query.split("=");
            result.put(keyVal[0], keyVal[1]);
        }
        return result;
    }
}
