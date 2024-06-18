package app.tozzi.utils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

public interface JPAExpressionFunction<T, V> {
    Expression<V> apply(CriteriaBuilder t, Expression<T>[] u, Class<?> s);
}
