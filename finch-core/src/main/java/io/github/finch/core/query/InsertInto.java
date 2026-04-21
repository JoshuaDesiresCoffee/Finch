package io.github.finch.core.query;

public interface InsertInto<T> {
    InsertQuery<T> VALUES(T obj);
}
