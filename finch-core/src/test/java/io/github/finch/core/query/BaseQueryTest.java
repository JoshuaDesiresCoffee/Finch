package io.github.finch.core.query;

import io.github.finch.core.Table;
import io.github.finch.core.pool.ConnectionPool;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseQueryTest {

    // Minimal concrete subclass to expose the protected static helper.
    static class Stub extends BaseQuery<Object> {
        Stub() { super(null); }
        static Object jdbc(Object val) { return toJdbcValue(val); }
    }

    // Fixture for WHERE-from-object tests.
    @Table
    static class Widget {
        int id;
        String color;
        int stock;
    }

    // ── toJdbcValue ───────────────────────────────────────────────────────────

    @Test void toJdbcValue_null_returnsNull() {
        assertNull(Stub.jdbc(null));
    }

    @Test void toJdbcValue_plainTypes_passthrough() {
        assertEquals("hello", Stub.jdbc("hello"));
        assertEquals(42,      Stub.jdbc(42));
        assertEquals(3.14,    Stub.jdbc(3.14));
    }

    @Test void toJdbcValue_localDate_becomesSqlDate() {
        Object result = Stub.jdbc(LocalDate.of(2024, 6, 15));
        assertInstanceOf(java.sql.Date.class, result);
        assertEquals(java.sql.Date.valueOf("2024-06-15"), result);
    }

    @Test void toJdbcValue_localDateTime_becomesSqlTimestamp() {
        LocalDateTime ldt = LocalDateTime.of(2024, 6, 15, 10, 30);
        Object result = Stub.jdbc(ldt);
        assertInstanceOf(Timestamp.class, result);
        assertEquals(Timestamp.valueOf(ldt), result);
    }

    @Test void toJdbcValue_localTime_becomesSqlTime() {
        LocalTime lt = LocalTime.of(9, 15, 30);
        Object result = Stub.jdbc(lt);
        assertInstanceOf(Time.class, result);
        assertEquals(Time.valueOf(lt), result);
    }

    @Test void toJdbcValue_instant_becomesSqlTimestamp() {
        Instant instant = Instant.parse("2024-06-15T10:30:00Z");
        Object result = Stub.jdbc(instant);
        assertInstanceOf(Timestamp.class, result);
        assertEquals(Timestamp.from(instant), result);
    }

    @Test void toJdbcValue_enum_becomesName() {
        assertEquals("ACTIVE", Stub.jdbc(Status.ACTIVE));
        assertEquals("DONE",   Stub.jdbc(Status.DONE));
    }

    enum Status { ACTIVE, DONE }

    // ── WHERE building ────────────────────────────────────────────────────────

    @Test void whereFragment_null_whenNoWhereSet() {
        Stub q = new Stub();
        assertNull(q.whereFragment());
    }

    @Test void setWhere_storesTemplateAndParams() {
        Stub q = new Stub();
        q.setWhere("id = ?", new Object[]{7});
        assertEquals("id = ?", q.whereFragment());
    }

    @Test void setWhereRaw_storesLiteralSql() {
        Stub q = new Stub();
        q.setWhereRaw("created_at > NOW()");
        assertEquals("created_at > NOW()", q.whereFragment());
    }

    @Test void setWhereRaw_clearsTemplate() {
        Stub q = new Stub();
        q.setWhere("id = ?", new Object[]{1});
        q.setWhereRaw("color = 'red'");
        assertEquals("color = 'red'", q.whereFragment());
    }

    @Test void setWhere_clearsRaw() {
        Stub q = new Stub();
        q.setWhereRaw("color = 'red'");
        q.setWhere("id = ?", new Object[]{1});
        assertEquals("id = ?", q.whereFragment());
    }

    @Test void setWhereFromObject_buildsFromNonNullFields() {
        Widget w = new Widget();
        w.color = "blue";
        w.stock = 10;
        // id is 0 (primitive default), still non-null conceptually — int won't be null
        Stub q = new Stub();
        q.setWhereFromObject(w);
        String fragment = q.whereFragment();
        assertNotNull(fragment);
        assertTrue(fragment.contains("color = ?"));
        assertTrue(fragment.contains("stock = ?"));
    }

    @Test void setWhereFromObject_nullFieldsExcluded() {
        Widget w = new Widget();
        // color is null, stock is 0 (primitive)
        w.stock = 5;
        Stub q = new Stub();
        q.setWhereFromObject(w);
        assertFalse(q.whereFragment().contains("color"));
    }
}
