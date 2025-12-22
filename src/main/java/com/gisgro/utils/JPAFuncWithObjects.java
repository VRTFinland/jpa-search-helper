package com.gisgro.utils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public interface JPAFuncWithObjects<V> {
    Expression<V> apply(Root<?> r, CriteriaQuery<?> query, CriteriaBuilder cb, Object[] u, Map<String, List<Field>> s);
}
