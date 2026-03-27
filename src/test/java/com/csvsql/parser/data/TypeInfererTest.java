package com.csvsql.parser.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TypeInferer.
 */
class TypeInfererTest {

    private TypeInferer inferer;

    @BeforeEach
    void setUp() {
        inferer = new TypeInferer();
    }

    @Test
    void testInferInteger() {
        List<String> values = Arrays.asList("1", "2", "3", "100", "-50");
        assertEquals(ColumnType.INTEGER, inferer.inferType(values));
    }

    @Test
    void testInferLong() {
        List<String> values = Arrays.asList("10000000000", "20000000000");
        assertEquals(ColumnType.LONG, inferer.inferType(values));
    }

    @Test
    void testInferDouble() {
        List<String> values = Arrays.asList("1.5", "2.75", "3.14159");
        assertEquals(ColumnType.DOUBLE, inferer.inferType(values));
    }

    @Test
    void testInferBoolean() {
        List<String> values = Arrays.asList("true", "false", "true", "false");
        assertEquals(ColumnType.BOOLEAN, inferer.inferType(values));
    }

    @Test
    void testInferDate() {
        List<String> values = Arrays.asList("2023-01-15", "2023-06-20", "2022-12-31");
        assertEquals(ColumnType.DATE, inferer.inferType(values));
    }

    @Test
    void testInferString() {
        List<String> values = Arrays.asList("hello", "world", "test");
        assertEquals(ColumnType.STRING, inferer.inferType(values));
    }

    @Test
    void testInferMixedNumericAsString() {
        List<String> values = Arrays.asList("123", "45.67", "789");
        assertEquals(ColumnType.DOUBLE, inferer.inferType(values));
    }

    @Test
    void testInferWithNulls() {
        List<String> values = Arrays.asList("1", null, "2", null, "3");
        assertEquals(ColumnType.INTEGER, inferer.inferType(values));
    }

    @Test
    void testInferEmptyList() {
        List<String> values = Collections.emptyList();
        assertEquals(ColumnType.STRING, inferer.inferType(values));
    }

    @Test
    void testInferAllNulls() {
        List<String> values = Arrays.asList(null, null, null);
        assertEquals(ColumnType.STRING, inferer.inferType(values));
    }

    @Test
    void testParseInteger() {
        assertEquals(42, inferer.parseValue("42", ColumnType.INTEGER));
        assertEquals(-10, inferer.parseValue("-10", ColumnType.INTEGER));
        assertNull(inferer.parseValue(null, ColumnType.INTEGER));
    }

    @Test
    void testParseDouble() {
        assertEquals(3.14, inferer.parseValue("3.14", ColumnType.DOUBLE));
        assertEquals(-2.5, inferer.parseValue("-2.5", ColumnType.DOUBLE));
        assertNull(inferer.parseValue(null, ColumnType.DOUBLE));
    }

    @Test
    void testParseBoolean() {
        assertEquals(true, inferer.parseValue("true", ColumnType.BOOLEAN));
        assertEquals(false, inferer.parseValue("false", ColumnType.BOOLEAN));
        assertEquals(true, inferer.parseValue("TRUE", ColumnType.BOOLEAN));
        assertEquals(true, inferer.parseValue("1", ColumnType.BOOLEAN));
    }

    @Test
    void testParseDate() {
        java.time.LocalDate date = (java.time.LocalDate) inferer.parseValue("2023-06-15", ColumnType.DATE);
        assertEquals(2023, date.getYear());
        assertEquals(6, date.getMonthValue());
        assertEquals(15, date.getDayOfMonth());
    }

    @Test
    void testParseString() {
        assertEquals("hello", inferer.parseValue("hello", ColumnType.STRING));
        assertNull(inferer.parseValue(null, ColumnType.STRING));
    }

    @Test
    void testParseEmptyString() {
        assertNull(inferer.parseValue("", ColumnType.STRING));
        assertNull(inferer.parseValue("   ", ColumnType.STRING));
    }

    @Test
    void testParseNullString() {
        assertNull(inferer.parseValue("null", ColumnType.STRING));
        assertNull(inferer.parseValue("NULL", ColumnType.STRING));
        assertNull(inferer.parseValue("None", ColumnType.STRING));
    }
}