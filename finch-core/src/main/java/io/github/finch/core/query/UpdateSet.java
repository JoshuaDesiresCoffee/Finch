package io.github.finch.core.query;

public interface UpdateSet<T> {
    UpdateQuery<T> SET(Object obj);
}
