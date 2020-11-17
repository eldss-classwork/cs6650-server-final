import exceptions.EmptyPathException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Performs tasks common to every Servlet response set-up.
 */
class ServletMethodSetup {

    /**
     * Performs common set-up tasks for any Servlet response. Checks for an empty URL, sets common
     * response headers, and provides an array of the parts of the request path if available.
     * @param request A servlet request
     * @param response A servlet response
     * @return true if the set-up went smoothly and the response can continue. false if an empty URL
     * is found.
     */
    static String[] setUp(HttpServletRequest request, HttpServletResponse response)
            throws EmptyPathException, IOException {

        // Check path
        final String urlString = request.getPathInfo();
        if (isEmptyPath(urlString, response)) {
            throw new EmptyPathException();
        }

        // Set-up common headers
        response.setContentType("application/json;charset=UTF-8");

        // Process path and return
        return urlString.split("/");
    }

    /**
     * Checks if the given path is empty.
     * @param urlPathString The path string
     * @param response A servlet response
     * @return true if the string is empty or null, otherwise false
     */
    private static boolean isEmptyPath(String urlPathString, HttpServletResponse response)
            throws IOException {
        if (urlPathString == null || urlPathString.isEmpty()) {
            String errorMsgJSON = "\"status\": \"request failed: unknown path\"";
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(errorMsgJSON);
            return true;
        }
        return false;
    }
}
