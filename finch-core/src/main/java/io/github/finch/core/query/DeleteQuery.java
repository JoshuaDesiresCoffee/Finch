package io.github.finch.core.query;

public interface DeleteQuery<T> {
    DeleteQuery<T> WHERE(String condition, Object... params);
    DeleteQuery<T> WHERE(Object example);
    DeleteQuery<T> whereRaw(String rawSql);
    void EXEC();
}
