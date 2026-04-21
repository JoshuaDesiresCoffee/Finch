package io.github.finch.core.query;

public interface UpdateQuery<T> {
    UpdateQuery<T> SET(Object obj);
    UpdateQuery<T> WHERE(String condition, Object... params);
    UpdateQuery<T> WHERE(Object example);
    UpdateQuery<T> whereRaw(String rawSql);
    void EXEC();
}
