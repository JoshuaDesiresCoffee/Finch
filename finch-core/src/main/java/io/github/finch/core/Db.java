package Autumn.orm;

import io.github.finch.core.mapping.SchemaSync;
import io.github.finch.core.pool.ConnectionPool;
import io.github.finch.core.pool.DataSourcePool;
import io.github.finch.core.pool.SimpleConnectionPool;
import io.github.finch.core.query.*;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Entry point for the Autumn ORM.
 *
 * Initialization:
 *   Db db = Db.configure()
 *               .url("jdbc:sqlite:data/app.db")
 *               .poolSize(10)
 *               .tables(User.class, Post.class)
 *               .connect();
 *
 * Annotation shortcut (reads @Database, no classpath scan):
 *   Db db = Db.from(AppConfig.class);
 *
 * Query syntax:
 *   db.SELECT.FROM(User.class).WHERE("id = ?", 1).EXEC()
 *   db.INSERT.INTO(User.class).VALUES(user).EXEC()
 *   db.UPDATE(User.class).SET(user).WHERE("id = ?", user.id).EXEC()
 *   db.UPDATE(Post.class).SET(category).WHERE("id = ?", post.id).EXEC()  // FK only
 *   db.DELETE.FROM(User.class).WHERE(user).EXEC()
 */
public class Db {

    /** Convenience reference set automatically by connect() / Db.from(). */
    public static Db instance;

    private final ConnectionPool pool;

    private Db(ConnectionPool pool) {
        this.pool = pool;
    }

    // ── Dispatchers ───────────────────────────────────────────────────────────

    public final class Select {
        public <T> SelectQuery<T> FROM(Class<T> tableClass) {
            return new SelectQueryImpl<>(pool, tableClass);
        }
    }

    public final class Insert {
        public <T> InsertInto<T> INTO(Class<T> tableClass) {
            return new InsertIntoImpl<>(pool, tableClass);
        }
    }

    public final class Delete {
        public <T> DeleteQuery<T> FROM(Class<T> tableClass) {
            return new DeleteQueryImpl<>(pool, tableClass);
        }
    }

    public final Select SELECT = new Select();
    public final Insert INSERT = new Insert();
    public final Delete DELETE = new Delete();

    public <T> UpdateSet<T> UPDATE(Class<T> tableClass) {
        return new UpdateQueryImpl<>(pool, tableClass);
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    /**
     * Begin a transaction. The returned DbTransaction holds one connection
     * from the pool with autoCommit disabled.
     *
     * try-with-resources (recommended — auto-rollback on exception):
     *   try (DbTransaction t = db.TRANSACTION()) {
     *       t.UPDATE(Account.class).SET(a).WHERE("id = ?", id).EXEC();
     *       t.COMMIT();
     *   }
     *
     * Manual:
     *   DbTransaction t = db.TRANSACTION();
     *   t.INSERT.INTO(Post.class).VALUES(post).EXEC();
     *   t.COMMIT();  // or t.ROLLBACK()
     */
    public DbTransaction TRANSACTION() {
        try {
            return new DbTransaction(pool.borrow(), pool);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    // ── Raw SQL ───────────────────────────────────────────────────────────────

    /**
     * Execute arbitrary SQL with positional parameters.
     * Returns rows as a list of column-name → value maps for SELECT statements.
     * Returns an empty list for INSERT / UPDATE / DELETE.
     *
     *   db.EXEC("SELECT * FROM user WHERE id = ?", List.of(1))
     *   db.EXEC("DELETE FROM session WHERE expires_at < ?", List.of(cutoff))
     */
    public List<Map<String, Object>> EXEC(String sql, List<Object> params) {
        Connection conn = null;
        try {
            conn = pool.borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.size(); i++)
                        ps.setObject(i + 1, params.get(i));
                }
                List<Map<String, Object>> rows = new ArrayList<>();
                if (ps.execute()) {
                    try (ResultSet rs = ps.getResultSet()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int cols = meta.getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= cols; i++)
                                row.put(meta.getColumnName(i), rs.getObject(i));
                            rows.add(row);
                        }
                    }
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("EXEC failed: " + sql, e);
        } finally {
            if (conn != null) pool.release(conn);
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    public Db sync(Class<?>... tables) {
        SchemaSync.sync(pool, tables);
        return this;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void close() {
        pool.close();
    }

    // ── Initialization ────────────────────────────────────────────────────────

    public static DbConfig configure() {
        return new DbConfig();
    }

    /** Reads connection info from a @Database-annotated class. No classpath scanning. */
    public static Db from(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(Database.class))
            throw new RuntimeException(configClass.getName() + " must be annotated with @Database");
        Database ann = configClass.getAnnotation(Database.class);
        return configure()
                .url(ann.url())
                .user(ann.user())
                .password(ann.password())
                .connect();
    }

    // ── Config builder ────────────────────────────────────────────────────────

    public static final class DbConfig {

        private String     url      = "";
        private String     user     = "";
        private String     password = "";
        private int        poolSize = 10;
        private DataSource dataSource;
        private Class<?>[] tables   = new Class[0];

        public DbConfig url(String url)           { this.url        = url; return this; }
        public DbConfig user(String user)         { this.user       = user; return this; }
        public DbConfig password(String pw)       { this.password   = pw;  return this; }
        public DbConfig poolSize(int n)           { this.poolSize   = n;   return this; }
        public DbConfig dataSource(DataSource ds) { this.dataSource = ds;  return this; }
        public DbConfig tables(Class<?>... cls)   { this.tables     = cls; return this; }

        public Db connect() {
            ensureDirectories(url);
            ConnectionPool cp;
            try {
                cp = dataSource != null
                        ? new DataSourcePool(dataSource)
                        : new SimpleConnectionPool(url, user, password, poolSize);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialise connection pool", e);
            }
            Db db = new Db(cp);
            if (tables.length > 0) db.sync(tables);
            instance = db;
            return db;
        }

        private static void ensureDirectories(String url) {
            if (url.startsWith("jdbc:sqlite:")) {
                File parent = new File(url.substring("jdbc:sqlite:".length())).getParentFile();
                if (parent != null) parent.mkdirs();
            }
        }
    }
}
