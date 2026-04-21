# Finch

A lightweight Java ORM that reads like SQL. Finch gives you type-safe query builder syntax, automatic schema management, and ORM conveniences — without hiding what's happening.

```java
db.SELECT.FROM(Post.class).JOIN(User.class).WHERE("published = ?", true).ORDER_BY("created_at DESC").LIMIT(10).EXEC()
db.UPDATE(Post.class).SET(post).WHERE("id = ?", post.id).EXEC()
db.EXEC("SELECT COUNT(*) AS n FROM post WHERE published = ?", List.of(true))
```

---

## Quick start

**1. Annotate your entities**

```java
@Table
public class User {
    @Id public int    id;
    public String     name;
    public String     email;
}

@Table
public class Post {
    @Id public int       id;
    public String        title;
    public User          author;     // FK → author_id column

    @OneToMany
    public List<Comment> comments;   // no column; Comment.post is the FK side
}
```

**2. Connect**

```java
Db db = Db.configure()
        .url("jdbc:sqlite:data/app.db")
        .poolSize(10)
        .tables(User.class, Post.class, Comment.class)
        .connect();
```

`tables(...)` runs `CREATE TABLE IF NOT EXISTS` for every class and auto-creates junction tables for `@ManyToMany` relationships.

**Alternatives**

```java
// Annotation config
@Database(url = "jdbc:sqlite:data/app.db")
public class AppConfig {}
Db db = Db.from(AppConfig.class);

// Bring your own pool (HikariCP, etc.)
Db db = Db.configure().dataSource(hikariDataSource).tables(...).connect();
```

---

## Queries

### SELECT

```java
db.SELECT.FROM(User.class).EXEC()
db.SELECT.FROM(User.class).WHERE("id = ?", 1).EXEC()
db.SELECT.FROM(User.class).WHERE("name = ? AND active = ?", "Alice", true).EXEC()
db.SELECT.FROM(User.class).WHERE(partialUser).EXEC()              // non-null fields → AND conditions
db.SELECT.FROM(Post.class).ORDER_BY("created_at DESC").EXEC()
db.SELECT.FROM(Post.class).LIMIT(10).OFFSET(20).EXEC()
db.SELECT.FROM(User.class).whereRaw("created_at > datetime('now', '-7 days')").EXEC()
```

All return `List<T>`.

### INSERT

```java
db.INSERT.INTO(User.class).VALUES(alice).EXEC()
```

FK fields are resolved automatically — if `post.author = alice`, Finch stores `author_id = alice.id`.

### UPDATE

```java
// Full object
db.UPDATE(User.class).SET(alice).WHERE("id = ?", alice.id).EXEC()

// FK only — updates just the foreign key column
db.UPDATE(Post.class).SET(tech).WHERE("id = ?", post.id).EXEC()

// FK only — updates just the foreign key column
db.UPDATE(Post.class).SET(bob).WHERE("id = ?", post.id).EXEC()
```

### DELETE

```java
db.DELETE.FROM(User.class).WHERE("id = ?", 1).EXEC()
db.DELETE.FROM(User.class).WHERE(bob).EXEC()   // non-null fields
db.DELETE.FROM(User.class).EXEC()              // all rows
```

### Raw SQL

```java
// Returns List<Map<String, Object>>
List<Map<String, Object>> rows = db.EXEC(
    "SELECT category, COUNT(*) AS n FROM post GROUP BY category",
    List.of()
);

// DML
db.EXEC("DELETE FROM session WHERE expires_at < ?", List.of(cutoff));
```

---

## Relationships

### Many-to-one (FK)

```java
@Table public class Post {
    @Id public int id;
    public User    author;   // column: author_id
}
```

Without `JOIN`, the related field is an id-only stub. With `JOIN`, it's fully hydrated via a single batched `IN (?)` query — no N+1.

```java
db.SELECT.FROM(Post.class).JOIN(User.class).EXEC()              // author fully loaded
db.SELECT.FROM(Post.class).JOIN(User.class).JOIN(Category.class).EXEC()  // multiple
```

### One-to-many

```java
@Table public class Post {
    @Id public int id;
    @OneToMany public List<Comment> comments;
}

db.SELECT.FROM(Post.class).JOIN(Comment.class).EXEC()  // comments list populated
```

### Many-to-many

```java
@Table public class Post {
    @Id public int id;
    @ManyToMany public List<Tag> tags;
}
```

Junction table (`post_tag`) is created automatically by `tables(...)`. Junction rows are currently managed manually via `EXEC`.

---

## Transactions

```java
try (DbTransaction t = db.TRANSACTION()) {
    Account from = t.SELECT.FROM(Account.class).WHERE("id = ?", fromId).EXEC().get(0);
    Account to   = t.SELECT.FROM(Account.class).WHERE("id = ?", toId).EXEC().get(0);

    from.balance -= amount;
    to.balance   += amount;

    t.UPDATE(Account.class).SET(from).WHERE("id = ?", from.id).EXEC();
    t.UPDATE(Account.class).SET(to).WHERE("id = ?", to.id).EXEC();

    t.COMMIT();
}  // uncaught exception → auto ROLLBACK
```

`DbTransaction` exposes the same `SELECT / INSERT / UPDATE / DELETE / EXEC` API as `Db`.

---

## Type support

| Java | SQL |
|---|---|
| `int` / `Integer` | `INTEGER` |
| `long` / `Long` | `BIGINT` |
| `double` / `Double` | `DOUBLE` |
| `boolean` / `Boolean` | `BOOLEAN` |
| `String` | `TEXT` |
| `BigDecimal` | `DECIMAL` |
| `LocalDate` | `DATE` |
| `LocalDateTime` | `TIMESTAMP` |
| `Instant` | `TIMESTAMP` |
| `UUID` | `TEXT` |
| `byte[]` | `BLOB` |
| Any `enum` | `TEXT` (stored as name) |

---

## Annotations

| Annotation | Target | Notes |
|---|---|---|
| `@Table` | class | Marks an entity. `name` overrides table name. |
| `@Id` | field | Primary key. Falls back to a field named `id`. |
| `@Column` | field | `name`, `nullable`, `unique` attributes. |
| `@OneToMany` | `List<T>` field | Child table holds the FK. |
| `@ManyToMany` | `List<T>` field | Junction table auto-created alphabetically (`alpha_beta`). |
| `@Database` | class | JDBC config for use with `Db.from()`. |

---

## Safety

All values go through `PreparedStatement`. Use `?` placeholders — never string-concat user input into SQL.

```java
db.SELECT.FROM(User.class).WHERE("email = ?", userInput).EXEC()  // safe
db.SELECT.FROM(User.class).whereRaw(userInput).EXEC()            // unsafe — never do this
```

---

## Connection pool

The built-in `SimpleConnectionPool` pre-warms `poolSize / 4` connections, grows lazily to `poolSize`, validates on borrow, and blocks up to 5 s before throwing on exhaustion. Pass a `DataSource` to use HikariCP or any other external pool instead.

---

## Project layout

```
finch-core/          Shared ORM logic (query builder, mapping, pool)
finch-maven-plugin/  Maven plugin wrapper
finch-gradle-plugin/ Gradle plugin wrapper
```

---

## Contributing

Issues and PRs are welcome. Please open an issue before starting significant work so we can align on direction.

## License

[Apache 2.0](LICENSE)
