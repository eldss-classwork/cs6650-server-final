package data.models;

public interface JSONable {

    /**
     * Converts the fields in an object to JSON format. Fields are encapsulated
     * in simple curly braces in the form {"field1": "stringVal", "field2": numVal}.
     * The name of the class is not provided, just the fields.
     * @return A JSON formatted string containing an object's field values.
     */
    String fieldsToJSON();
}
