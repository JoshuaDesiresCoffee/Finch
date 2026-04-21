package io.github.finch.core.mapping;

import io.github.finch.core.Table;
import io.github.finch.core.annotations.Column;
import io.github.finch.core.pool.ConnectionPool;

import java.sql.*;
import java.util.*;

public class SchemaSync {

    public static void sync(ConnectionPool pool, Class<?>... tables) {
        for (Class<?> t : tables) {
            if (!t.isAnnotationPresent(Table.class))
                throw new RuntimeException(t.getName() + " must be annotated with @Table");
            syncTable(pool, t);
        }
        for (Class<?> t : tables) {
            for (EntityMapper.FieldInfo fi : EntityMapper.getFields(t)) {
                if (fi.isManyToMany && fi.relatedType != null)
                    syncJunctionTable(pool, t, fi.relatedType);
            }
        }
    }

    private static void syncTable(ConnectionPool pool, Class<?> t) {
        List<EntityMapper.FieldInfo> fields = EntityMapper.getFields(t);
        List<String> cols = new ArrayList<>();

        for (EntityMapper.FieldInfo fi : fields) {
            if (fi.columnName == null) continue; // OneToMany / ManyToMany — no column here

            StringBuilder col = new StringBuilder(fi.columnName).append(" ");
            col.append(fi.isForeignKey ? "INTEGER" : EntityMapper.sqlType(fi.field.getType()));

            if (fi.isId) col.append(" PRIMARY KEY");

            if (fi.field.isAnnotationPresent(Column.class)) {
                Column ann = fi.field.getAnnotation(Column.class);
                if (!ann.nullable()) col.append(" NOT NULL");
                if (ann.unique())    col.append(" UNIQUE");
            }
            cols.add(col.toString());
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + EntityMapper.tableName(t)
                + " (" + String.join(", ", cols) + ")";
        exec(pool, sql, t.getSimpleName());
    }

    private static void syncJunctionTable(ConnectionPool pool, Class<?> a, Class<?> b) {
        String nameA = EntityMapper.tableName(a);
        String nameB = EntityMapper.tableName(b);
        String[] sorted = {nameA, nameB};
        Arrays.sort(sorted);
        String junction = sorted[0] + "_" + sorted[1];

        String colA = nameA + "_id";
        String colB = nameB + "_id";
        String sql  = "CREATE TABLE IF NOT EXISTS " + junction + " ("
                + colA + " INTEGER, " + colB + " INTEGER, "
                + "PRIMARY KEY (" + colA + ", " + colB + "))";
        exec(pool, sql, junction);
    }

    private static void exec(ConnectionPool pool, String sql, String context) {
        Connection conn = null;
        try {
            conn = pool.borrow();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Schema sync failed for " + context + ": " + sql, e);
        } finally {
            if (conn != null) pool.release(conn);
        }
    }
}
