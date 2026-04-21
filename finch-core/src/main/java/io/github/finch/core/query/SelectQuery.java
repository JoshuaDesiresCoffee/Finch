package io.github.finch.core.query;

import java.util.List;

public interface SelectQuery<T> {
    SelectQuery<T> WHERE(String condition, Object... params);
    SelectQuery<T> WHERE(Object example);
    SelectQuery<T> whereRaw(String rawSql);
    SelectQuery<T> JOIN(Class<?> related);
    SelectQuery<T> ORDER_BY(String orderByClause);
    SelectQuery<T> LIMIT(int n);
    SelectQuery<T> OFFSET(int n);
    List<T> EXEC();
}
