package com.gisgro.utils;

import com.gisgro.annotations.NestedSearchable;
import com.gisgro.annotations.Searchable;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ReflectionUtils {
    private static final ConcurrentHashMap<Class<?>, Map<String,Pair<Searchable, Class<?>>>> cache = new ConcurrentHashMap<>();

    public static Map<String, Pair<Searchable, Class<?>>> getAllSearchableFields(Class<?> beanClass) {
        return cache.computeIfAbsent(beanClass, key -> {
            Map<String, Pair<Searchable, Class<?>>> res = new HashMap<>();
            getAllSearchableFields(new StringBuilder(), beanClass, res);
            return res;
        });
    }

    private static void getAllSearchableFields(final StringBuilder root, Class<?> beanClass, Map<String, Pair<Searchable, Class<?>>> res) {
        Stream.of(beanClass.getDeclaredFields())
            .forEach(f -> {
                if (f.isAnnotationPresent(Searchable.class)) {
                    res.putIfAbsent(root.isEmpty() ? f.getName() : root + "." + f.getName(), Pair.of(f.getAnnotation(Searchable.class), f.getType()));
                }
                if (f.isAnnotationPresent(NestedSearchable.class)) {
                    if (!root.isEmpty()) {
                        root.append(".");
                    }
                    root.append(f.getName());
                    getAllSearchableFields(root, f.getType(), res);

                    if (root.indexOf(".") != -1) {
                        root.delete(root.lastIndexOf("."), root.length());

                    } else {
                        root.setLength(0);
                    }
                }
            });
    }
}
