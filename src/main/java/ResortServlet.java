import exceptions.EmptyPathException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ResortServlet")
public class ResortServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {
        String[] pathParts;
        try {
            pathParts = ServletMethodSetup.setUp(request, response);
        } catch (EmptyPathException e) {
            // Error response taken care of in setup
            return;
        }

        if (!isUrlValidGET(pathParts)) {
            // Invalid path
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "unknown path provided");
        } else {
            response.setStatus(HttpServletResponse.SC_OK);

            // Echo back the path and anything in the query
            try (PrintWriter out = response.getWriter()) {
                out.println("{\"status\": \"request successful\"}");
            }
        }
    }

    /**
     * Checks if a POST request path is valid.
     * @param pathParts An array of path parts
     * @return true if valid, otherwise false
     */
    private boolean isUrlValidGET(String[] pathParts) {
        // urlPath  = "/day/top10vert"
        // pathParts = ["", "day", "top10vert"]
        final String resource1 = "day";
        final String resource2 = "top10vert";
        // Ignores path params past valid part
        if (pathParts.length >= 3) {
            return pathParts[1].equals(resource1) && pathParts[2].equals(resource2);
        }
        return false;
    }
}
