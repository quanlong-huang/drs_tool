package com.csvsql.parser.error;

/**
 * Exception thrown when an unsupported SQL feature is used.
 *
 * <p>This exception is thrown when the user attempts to use a SQL feature
 * that is not supported by the CSV SQL Parser. Common unsupported features
 * include:</p>
 * <ul>
 *   <li>INSERT, UPDATE, DELETE statements</li>
 *   <li>Subqueries in SELECT clause</li>
 *   <li>UNION operations</li>
 *   <li>Window functions</li>
 *   <li>CTEs (Common Table Expressions)</li>
 * </ul>
 *
 * <p>The exception provides a message explaining why the feature is not
 * supported and may suggest alternative approaches.</p>
 *
 * @see CsvSqlException
 * @see com.csvsql.parser.parser.SupportedFeatures
 */
public class UnsupportedFeatureException extends CsvSqlException {

    private final String feature;
    private final String featureType;
    private final String alternatives;

    public UnsupportedFeatureException(String feature, String featureType) {
        super("Unsupported " + featureType + ": " + feature,
              "This feature is not supported. Please refer to the SQL syntax documentation.");
        this.feature = feature;
        this.featureType = featureType;
        this.alternatives = null;
    }

    public UnsupportedFeatureException(String feature, String featureType, String suggestion) {
        super("Unsupported " + featureType + ": " + feature, suggestion);
        this.feature = feature;
        this.featureType = featureType;
        this.alternatives = null;
    }

    public UnsupportedFeatureException(String feature, String featureType, String suggestion, String alternatives) {
        super("Unsupported " + featureType + ": " + feature, suggestion);
        this.feature = feature;
        this.featureType = featureType;
        this.alternatives = alternatives;
    }

    public String getFeature() {
        return feature;
    }

    public String getFeatureType() {
        return featureType;
    }

    public String getAlternatives() {
        return alternatives;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error: Unsupported ").append(featureType).append(": ").append(feature);

        if (hasSuggestion()) {
            sb.append("\n\n").append(getSuggestion());
        }

        if (alternatives != null) {
            sb.append("\n\nAlternatives: ").append(alternatives);
        }

        return sb.toString();
    }
}