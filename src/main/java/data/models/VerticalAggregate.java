package data.models;

/**
 * Stores information from GET requests for the total vertical achieved by a skier at a given resort
 * and day. Two endpoints use this class, one based on only resort, the other both resort and day.
 * The skier's information is not stored, as that is not required by the API. Only resort name and
 * total vertical are provided to the client.
 */
public class VerticalAggregate implements JSONable {

    String resortID;
    int vertical;

    public VerticalAggregate(String resortID, int vertical) {
        this.resortID = resortID;
        this.vertical = vertical;
    }

    public String getResortID() {
        return resortID;
    }

    public int getVertical() {
        return vertical;
    }

    @Override
    public String toString() {
        return "VerticalAggregate{" +
                "resortID='" + resortID + '\'' +
                ", vertical=" + vertical +
                '}';
    }

    @Override
    public String fieldsToJSON() {
        return "{\"resortID\": \"" + resortID + "\", \"totalVert\": " + vertical + "}";
    }
}
