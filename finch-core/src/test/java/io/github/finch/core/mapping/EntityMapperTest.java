package io.github.finch.core.mapping;

import io.github.finch.core.Table;
import io.github.finch.core.annotations.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    @Table
    static class Author {
        @Id int authorId;
        String name;
        @Column(name = "email_address", nullable = false, unique = true)
        String email;
    }

    @Table(name = "articles")
    static class Article {
        int id;
        String title;
        Author author;
        @OneToMany List<Comment> comments;
        @ManyToMany List<Tag> tags;
    }

    @Table
    static class Comment {
        int id;
        Article article;
    }

    @Table
    static class Tag {
        int id;
        String label;
    }

    static class NoTable { String value; }

    static class NoId { String name; }

    // ── tableName ─────────────────────────────────────────────────────────────

    @Test void tableName_defaultsToLowercaseSimpleName() {
        assertEquals("author", EntityMapper.tableName(Author.class));
    }

    @Test void tableName_usesAnnotationValue() {
        assertEquals("articles", EntityMapper.tableName(Article.class));
    }

    @Test void tableName_noAnnotation_stillLowercases() {
        assertEquals("notable", EntityMapper.tableName(NoTable.class));
    }

    // ── getFields – id detection ───────────────────────────────────────────

    @Test void getFields_idByAnnotation() {
        EntityMapper.FieldInfo id = EntityMapper.getFields(Author.class).stream()
                .filter(fi -> fi.isId).findFirst().orElseThrow();
        assertEquals("authorId", id.field.getName());
    }

    @Test void getFields_idByFieldName() {
        EntityMapper.FieldInfo id = EntityMapper.getFields(Article.class).stream()
                .filter(fi -> fi.isId).findFirst().orElseThrow();
        assertEquals("id", id.field.getName());
    }

    // ── getFields – column name resolution ───────────────────────────────────

    @Test void getFields_defaultColumnNameIsFieldName() {
        EntityMapper.FieldInfo fi = fieldNamed(Author.class, "name");
        assertEquals("name", fi.columnName);
    }

    @Test void getFields_customColumnNameFromAnnotation() {
        EntityMapper.FieldInfo fi = fieldNamed(Author.class, "email");
        assertEquals("email_address", fi.columnName);
    }

    @Test void getFields_foreignKeyColumnNameHasSuffix() {
        EntityMapper.FieldInfo fi = fieldNamed(Article.class, "author");
        assertEquals("author_id", fi.columnName);
    }

    @Test void getFields_oneToManyHasNullColumnName() {
        EntityMapper.FieldInfo fi = fieldNamed(Article.class, "comments");
        assertNull(fi.columnName);
    }

    @Test void getFields_manyToManyHasNullColumnName() {
        EntityMapper.FieldInfo fi = fieldNamed(Article.class, "tags");
        assertNull(fi.columnName);
    }

    // ── getFields – relation flags ────────────────────────────────────────────

    @Test void getFields_foreignKey_flags() {
        EntityMapper.FieldInfo fi = fieldNamed(Article.class, "author");
        assertTrue(fi.isForeignKey);
        assertFalse(fi.isOneToMany);
        assertFalse(fi.isManyToMany);
        assertEquals(Author.class, fi.relatedType);
    }

    @Test void getFields_oneToMany_flags() {
        EntityMapper.FieldInfo fi = fieldNamed(Article.class, "comments");
        assertTrue(fi.isOneToMany);
        assertFalse(fi.isForeignKey);
        assertFalse(fi.isManyToMany);
        assertEquals(Comment.class, fi.relatedType);
    }

    @Test void getFields_manyToMany_flags() {
        EntityMapper.FieldInfo fi = fieldNamed(Article.class, "tags");
        assertTrue(fi.isManyToMany);
        assertFalse(fi.isForeignKey);
        assertFalse(fi.isOneToMany);
        assertEquals(Tag.class, fi.relatedType);
    }

    @Test void getFields_plainScalar_noRelationFlags() {
        EntityMapper.FieldInfo fi = fieldNamed(Author.class, "name");
        assertFalse(fi.isForeignKey);
        assertFalse(fi.isOneToMany);
        assertFalse(fi.isManyToMany);
        assertNull(fi.relatedType);
    }

    // ── getIdField ────────────────────────────────────────────────────────────

    @Test void getIdField_returnsCorrectField() {
        assertEquals("id", EntityMapper.getIdField(Article.class).field.getName());
    }

    @Test void getIdField_missingThrows() {
        assertThrows(RuntimeException.class, () -> EntityMapper.getIdField(NoId.class));
    }

    // ── sqlType ───────────────────────────────────────────────────────────────

    @Test void sqlType_primitiveAndBoxed() {
        assertEquals("INTEGER",   EntityMapper.sqlType(int.class));
        assertEquals("INTEGER",   EntityMapper.sqlType(Integer.class));
        assertEquals("BIGINT",    EntityMapper.sqlType(long.class));
        assertEquals("BIGINT",    EntityMapper.sqlType(Long.class));
        assertEquals("DOUBLE",    EntityMapper.sqlType(double.class));
        assertEquals("DOUBLE",    EntityMapper.sqlType(Double.class));
        assertEquals("BOOLEAN",   EntityMapper.sqlType(boolean.class));
        assertEquals("BOOLEAN",   EntityMapper.sqlType(Boolean.class));
    }

    @Test void sqlType_temporalTypes() {
        assertEquals("DATE",      EntityMapper.sqlType(LocalDate.class));
        assertEquals("TIMESTAMP", EntityMapper.sqlType(LocalDateTime.class));
        assertEquals("TIMESTAMP", EntityMapper.sqlType(Instant.class));
        assertEquals("TIME",      EntityMapper.sqlType(LocalTime.class));
    }

    @Test void sqlType_otherTypes() {
        assertEquals("DECIMAL",   EntityMapper.sqlType(BigDecimal.class));
        assertEquals("TEXT",      EntityMapper.sqlType(UUID.class));
        assertEquals("BLOB",      EntityMapper.sqlType(byte[].class));
        assertEquals("TEXT",      EntityMapper.sqlType(String.class));
    }

    @Test void sqlType_enumIsText() {
        assertEquals("TEXT", EntityMapper.sqlType(Status.class));
    }

    enum Status { ACTIVE, INACTIVE }

    // ── caching ───────────────────────────────────────────────────────────────

    @Test void getFields_returnsSameInstanceOnRepeatCall() {
        assertSame(EntityMapper.getFields(Tag.class), EntityMapper.getFields(Tag.class));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static EntityMapper.FieldInfo fieldNamed(Class<?> cls, String name) {
        return EntityMapper.getFields(cls).stream()
                .filter(fi -> fi.field.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No field '" + name + "' on " + cls.getSimpleName()));
    }
}
