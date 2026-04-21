package io.github.finch.core.query;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.UUID;

class CoerceTest {

    // ── null / pass-through ───────────────────────────────────────────────────

    @Test void coerce_null_returnsNull() {
        assertNull(SelectQueryImpl.coerce(null, String.class));
    }

    @Test void coerce_alreadyCorrectType_returnsSameInstance() {
        String s = "hello";
        assertSame(s, SelectQueryImpl.coerce(s, String.class));
    }

    // ── numeric from String ───────────────────────────────────────────────────

    @Test void coerce_stringToInt() {
        assertEquals(42, SelectQueryImpl.coerce("42", int.class));
        assertEquals(42, SelectQueryImpl.coerce("42", Integer.class));
    }

    @Test void coerce_stringToLong() {
        assertEquals(9_000_000_000L, SelectQueryImpl.coerce("9000000000", long.class));
        assertEquals(9_000_000_000L, SelectQueryImpl.coerce("9000000000", Long.class));
    }

    @Test void coerce_stringToDouble() {
        assertEquals(3.14, (Double) SelectQueryImpl.coerce("3.14", double.class), 1e-9);
        assertEquals(3.14, (Double) SelectQueryImpl.coerce("3.14", Double.class), 1e-9);
    }

    @Test void coerce_stringToBoolean() {
        assertEquals(true,  SelectQueryImpl.coerce("true",  boolean.class));
        assertEquals(false, SelectQueryImpl.coerce("false", Boolean.class));
    }

    @Test void coerce_stringToBigDecimal() {
        assertEquals(new BigDecimal("123.456"), SelectQueryImpl.coerce("123.456", BigDecimal.class));
    }

    @Test void coerce_stringToUuid() {
        UUID u = UUID.randomUUID();
        assertEquals(u, SelectQueryImpl.coerce(u.toString(), UUID.class));
    }

    // ── temporal from sql types ───────────────────────────────────────────────

    @Test void coerce_sqlDateToLocalDate() {
        java.sql.Date d = java.sql.Date.valueOf("2024-06-15");
        assertEquals(LocalDate.of(2024, 6, 15), SelectQueryImpl.coerce(d, LocalDate.class));
    }

    @Test void coerce_sqlTimestampToLocalDate() {
        Timestamp ts = Timestamp.valueOf("2024-06-15 10:30:00");
        assertEquals(LocalDate.of(2024, 6, 15), SelectQueryImpl.coerce(ts, LocalDate.class));
    }

    @Test void coerce_sqlTimestampToLocalDateTime() {
        Timestamp ts = Timestamp.valueOf("2024-06-15 10:30:00");
        assertEquals(LocalDateTime.of(2024, 6, 15, 10, 30), SelectQueryImpl.coerce(ts, LocalDateTime.class));
    }

    @Test void coerce_sqlDateToLocalDateTimeAtMidnight() {
        java.sql.Date d = java.sql.Date.valueOf("2024-01-01");
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), SelectQueryImpl.coerce(d, LocalDateTime.class));
    }

    @Test void coerce_sqliteStringToLocalDateTime() {
        // SQLite stores TIMESTAMP as "YYYY-MM-DD HH:MM:SS"
        assertEquals(LocalDateTime.of(2024, 3, 20, 14, 5, 59),
                SelectQueryImpl.coerce("2024-03-20 14:05:59", LocalDateTime.class));
    }

    @Test void coerce_stringToLocalDate() {
        assertEquals(LocalDate.of(2024, 3, 20), SelectQueryImpl.coerce("2024-03-20", LocalDate.class));
    }

    @Test void coerce_sqlTimeToLocalTime() {
        Time t = Time.valueOf("09:15:30");
        assertEquals(LocalTime.of(9, 15, 30), SelectQueryImpl.coerce(t, LocalTime.class));
    }

    @Test void coerce_stringToLocalTime() {
        assertEquals(LocalTime.of(9, 15, 30), SelectQueryImpl.coerce("09:15:30", LocalTime.class));
    }

    @Test void coerce_sqlTimestampToInstant() {
        Instant expected = LocalDateTime.of(2024, 6, 15, 10, 30, 0)
                .toInstant(java.time.ZoneOffset.UTC);
        Timestamp ts = Timestamp.from(expected);
        assertEquals(expected, SelectQueryImpl.coerce(ts, Instant.class));
    }

    @Test void coerce_longToInstant() {
        Instant expected = Instant.ofEpochMilli(1_700_000_000_000L);
        assertEquals(expected, SelectQueryImpl.coerce(1_700_000_000_000L, Instant.class));
    }

    // ── enum from String ──────────────────────────────────────────────────────

    @Test void coerce_stringToEnum() {
        assertEquals(Color.GREEN, SelectQueryImpl.coerce("GREEN", Color.class));
    }

    enum Color { RED, GREEN, BLUE }
}
