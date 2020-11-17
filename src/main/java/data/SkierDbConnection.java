package data;

import data.models.JSONable;
import data.models.VerticalAggregate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides methods to access the database for the skiers servlet.
 */
public class SkierDbConnection {

    /**
     * Adds a single lift ride to the database.
     *
     * @param resortID the resort name
     * @param dayID    the day of the year
     * @param skierID  the skier's ID
     * @param time     the time in minutes after opening
     * @param liftID   the lift number
     * @throws SQLException if there is a problem executing the query
     */
    public void postLiftRide(String resortID, int dayID, int skierID, int time, int liftID)
            throws SQLException {
        String SQL_QUERY = String.format(
                "INSERT INTO LiftRides(skierID, liftNum, resortID, `day`, `time`) VALUES (%d, %d, '%s', %d, %d);",
                skierID, liftID, resortID, dayID, time);
        try (Connection conn = DataSource.getConnection();
             PreparedStatement pst = conn.prepareStatement(SQL_QUERY)) {
            pst.executeUpdate();
        }
    }

    /**
     * Returns the resort name and total vertical for a given skier on the given day, at the given
     * resort.
     *
     * @param skierID the skier ID
     * @param day     the day of the year
     * @param resort  the resort
     * @return An object storing the resort and total vertical
     * @throws SQLException if there is a problem with the query
     */
    public JSONable getSkierVertByResortAndDay(int skierID, int day, String resort)
            throws SQLException {
        String SQL_QUERY = String.format(
                "SELECT lr.resortID, SUM(l.vertical) AS vert\n" +
                        "FROM LiftRides AS lr, Lifts AS l\n" +
                        "WHERE\n" +
                        "    lr.resortID=l.resortID\n" +
                        "    AND lr.liftNum=l.liftNum\n" +
                        "    AND lr.skierID=%d\n" +
                        "    AND lr.day=%d\n" +
                        "    AND lr.resortID='%s'\n" +
                        "GROUP BY lr.skierID, lr.resortID",
                skierID,
                day,
                resort
        );
        return getVerticalHelper(SQL_QUERY);
    }

    /**
     * Returns the resort name and total vertical for a given skier at the given resort, on any day.
     *
     * @param skierID the skier ID
     * @param resort  the resort
     * @return An object storing the resort and total vertical
     * @throws SQLException if there is a problem with the query
     */
    public JSONable getSkierVertByResort(int skierID, String resort) throws SQLException {
        String SQL_QUERY = String.format(
                "SELECT lr.resortID, SUM(l.vertical) AS vert\n" +
                        "FROM LiftRides AS lr, Lifts AS l\n" +
                        "WHERE\n" +
                        "    lr.resortID=l.resortID\n" +
                        "    AND lr.liftNum=l.liftNum\n" +
                        "    AND lr.skierID=%d\n" +
                        "    AND lr.resortID='%s'\n" +
                        "GROUP BY lr.skierID, lr.resortID",
                skierID,
                resort
        );
        return getVerticalHelper(SQL_QUERY);
    }

    /**
     * Searches the database for aggregated vertical data based on the provided query.
     *
     * @param SQL A SQL query that gets a single line including a resort name and the total vertical
     *            for a skier based on some criteria. The aggregate total vertical must be aliased as
     *            "vert", the resort must not be aliased.
     * @return A JSONable VerticalAggregate object with the information, or null
     * @throws SQLException if there is a problem with the query
     */
    private JSONable getVerticalHelper(String SQL) throws SQLException {
        JSONable vertObj = null;
        try (Connection conn = DataSource.getConnection();
             PreparedStatement pst = conn.prepareStatement(SQL);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                String res = rs.getString("resortID");
                int vert = rs.getInt("vert");
                vertObj = new VerticalAggregate(res, vert);
            }
        }
        return vertObj;
    }
}
