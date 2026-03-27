package com.csvsql.parser.engine.functions;

/**
 * Implementation of SQL math functions.
 *
 * <p>MathFunctions provides static methods for mathematical operations
 * commonly used in SQL queries:</p>
 * <ul>
 *   <li>Rounding: ROUND, CEILING, FLOOR, TRUNCATE</li>
 *   <li>Basic operations: ABS, MOD, SIGN</li>
 *   <li>Exponential/Logarithmic: SQRT, POWER, LOG, LOG10, EXP</li>
 * </ul>
 *
 * <p>All functions are null-safe and handle type conversion from strings.</p>
 *
 * @see com.csvsql.parser.engine.ExpressionEvaluator
 */
public class MathFunctions {

    /**
     * Round to nearest integer.
     */
    public static Long round(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return Math.round(((Number) value).doubleValue());
        }
        try {
            return Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Round to specified decimal places.
     */
    public static Double round(Object value, int decimals) {
        if (value == null) return null;
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            double factor = Math.pow(10, decimals);
            return Math.round(d * factor) / factor;
        }
        try {
            double d = Double.parseDouble(value.toString());
            double factor = Math.pow(10, decimals);
            return Math.round(d * factor) / factor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Ceiling - round up to nearest integer.
     */
    public static Long ceiling(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return (long) Math.ceil(((Number) value).doubleValue());
        }
        try {
            return (long) Math.ceil(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Floor - round down to nearest integer.
     */
    public static Long floor(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return (long) Math.floor(((Number) value).doubleValue());
        }
        try {
            return (long) Math.floor(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Absolute value.
     */
    public static Double abs(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return Math.abs(((Number) value).doubleValue());
        }
        try {
            return Math.abs(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Square root.
     */
    public static Double sqrt(Object value) {
        if (value == null) return null;
        double d;
        if (value instanceof Number) {
            d = ((Number) value).doubleValue();
        } else {
            try {
                d = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return d >= 0 ? Math.sqrt(d) : null;
    }

    /**
     * Power.
     */
    public static Double power(Object base, Object exponent) {
        if (base == null || exponent == null) return null;

        double b, e;
        if (base instanceof Number) {
            b = ((Number) base).doubleValue();
        } else {
            try {
                b = Double.parseDouble(base.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        if (exponent instanceof Number) {
            e = ((Number) exponent).doubleValue();
        } else {
            try {
                e = Double.parseDouble(exponent.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return Math.pow(b, e);
    }

    /**
     * Natural logarithm.
     */
    public static Double log(Object value) {
        if (value == null) return null;
        double d;
        if (value instanceof Number) {
            d = ((Number) value).doubleValue();
        } else {
            try {
                d = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return d > 0 ? Math.log(d) : null;
    }

    /**
     * Base-10 logarithm.
     */
    public static Double log10(Object value) {
        if (value == null) return null;
        double d;
        if (value instanceof Number) {
            d = ((Number) value).doubleValue();
        } else {
            try {
                d = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return d > 0 ? Math.log10(d) : null;
    }

    /**
     * Exponential function.
     */
    public static Double exp(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return Math.exp(((Number) value).doubleValue());
        }
        try {
            return Math.exp(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Modulo.
     */
    public static Double mod(Object dividend, Object divisor) {
        if (dividend == null || divisor == null) return null;

        double a, b;
        if (dividend instanceof Number) {
            a = ((Number) dividend).doubleValue();
        } else {
            try {
                a = Double.parseDouble(dividend.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (divisor instanceof Number) {
            b = ((Number) divisor).doubleValue();
        } else {
            try {
                b = Double.parseDouble(divisor.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return b != 0 ? a % b : null;
    }

    /**
     * Sign of value (-1, 0, 1).
     */
    public static Integer sign(Object value) {
        if (value == null) return null;
        double d;
        if (value instanceof Number) {
            d = ((Number) value).doubleValue();
        } else {
            try {
                d = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return d < 0 ? -1 : (d > 0 ? 1 : 0);
    }

    /**
     * Truncate to specified decimal places.
     */
    public static Double truncate(Object value, int decimals) {
        if (value == null) return null;
        double d;
        if (value instanceof Number) {
            d = ((Number) value).doubleValue();
        } else {
            try {
                d = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        double factor = Math.pow(10, decimals);
        return (long) (d * factor) / factor;
    }
}