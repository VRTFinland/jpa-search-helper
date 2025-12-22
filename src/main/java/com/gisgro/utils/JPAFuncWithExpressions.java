package com.gisgro.utils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

public interface JPAFuncWithExpressions<T, V> {
    Expression<V> apply(CriteriaBuilder t, Expression<T>[] u);
}
