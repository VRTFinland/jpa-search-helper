package com.gisgro;

import com.fasterxml.jackson.databind.JsonNode;
import com.gisgro.annotations.CollectionSearchable;
import com.gisgro.annotations.Searchable;
import com.gisgro.exceptions.InvalidFieldException;
import com.gisgro.exceptions.JPASearchException;
import com.gisgro.model.Operator;
import com.gisgro.model.SearchType;
import com.gisgro.utils.ReflectionUtils;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.lang.reflect.Field;
import java.util.*;

import static com.gisgro.JPASearchFunctions.getPath;

public class JPASearchCore {
    public static <R, T> Specification<R> specification(
            JsonNode filterPayload,
            Class<T> entityClass,
            boolean throwsIfNotExistsOrNotSearchable
    ) {
        return specification(
                filterPayload,
                entityClass,
                throwsIfNotExistsOrNotSearchable,
                Collections.emptySet()
        );
    }

    public static <R, T> Specification<R> specification(
            JsonNode filterPayload,
            Class<T> entityClass,
            boolean throwsIfNotExistsOrNotSearchable,
            Set<Class<?>> searchableSubclasses
    ) {
        HashSet<Class<?>> entityClasses = new HashSet<>(searchableSubclasses);
        entityClasses.add(entityClass);

        var filterExpression = filterPayload.get("filter");

        return (root, query, criteriaBuilder) -> {
            var searchableFields = ReflectionUtils.getAllSearchableFields(entityClasses);
            var expr = processExpression(
                    filterExpression,
                    criteriaBuilder,
                    root,
                    query,
                    entityClass,
                    entityClasses,
                    throwsIfNotExistsOrNotSearchable,
                    searchableFields
            );
            if (expr instanceof Predicate) {
                return (Predicate) expr;
            } else {
                throw new JPASearchException("Not resulting a predicate" + expr);
            }
        };
    }

    private static <T> Object processValue(
            Operator op,
            JsonNode node,
            CriteriaBuilder cb,
            Root<?> root,
            AbstractQuery<?> query,
            Class<?> entityClass,
            Set<Class<?>> entityClasses,
            boolean throwsIfNotExistsOrNotSearchable,
            Map<String, List<Field>> searchableFields
    ) {
        if (node.isTextual()) {
            var text = node.asText();
            if (Objects.equals(op.getName(), "field")) {
                return processField(
                        cb,
                        root,
                        throwsIfNotExistsOrNotSearchable,
                        searchableFields,
                        text
                );
            } else if (!op.isEvaluateStrings()) {
                return text;
            } else {
                return cb.literal(text);
            }
        } else if (node.isInt()) {
            return cb.literal(node.asInt());
        } else if (node.isLong()) {
            return cb.literal(node.asLong());
        } else if (node.isDouble()) {
            return cb.literal(node.asDouble());
        } else if (node.isBoolean()) {
            return cb.literal(node.asBoolean());
        } else if (node.isArray()) {
            return processExpression(
                    node,
                    cb,
                    root,
                    query,
                    entityClass,
                    entityClasses,
                    throwsIfNotExistsOrNotSearchable,
                    searchableFields
            );
        } else if (node.isNull()) {
            return cb.nullLiteral(entityClass);
        } else {
            throw new JPASearchException("unexpected: " + node);
        }
    }

    private static <T, U extends T> Expression<?> processField(
            CriteriaBuilder cb,
            Root<T> root,
            boolean throwsIfNotExistsOrNotSearchable,
            Map<String, List<Field>> searchableFields,
            String text
    ) {
        var descriptor = loadDescriptor(
                text,
                throwsIfNotExistsOrNotSearchable,
                false,
                false,
                searchableFields
        );
        if (descriptor == null) {
            return null;
        }

        var path = getPath(cb, root, descriptor);
        var field = descriptor.fieldPath.get(descriptor.fieldPath.size() - 1);
        var searchable = field.getAnnotation(Searchable.class);

        if (searchable.trim() && descriptor.searchType == SearchType.STRING) {
            return cb.trim(path.as(String.class));
        }

        return path;
    }

    /**
     * Processes the "has" operator, which checks for the existence of related entities in a collection.
     * This operator needs to create a subquery to check for the existence of related entities that match the specified criteria.
     * Basic Operator handling is not sufficient for this operator, as it requires a more complex query structure involving joins and subqueries.
     */
    private static Expression<?> processHasOperator(
            JsonNode node,
            CriteriaBuilder cb,
            Root<?> root,
            AbstractQuery<?> query,
            Set<Class<?>> entityClasses,
            boolean throwsIfNotExistsOrNotSearchable,
            Map<String, List<Field>> searchableFields
    ) {
        // First we need to do build the subquery for the collection items
        var attrName = node.get(1).asText();
        var descriptor = loadDescriptor(attrName, throwsIfNotExistsOrNotSearchable, false, false, searchableFields);
        if (descriptor == null) {
            throw new JPASearchException("Invalid field for has operator: " + attrName);
        }
        var field = descriptor.fieldPath.get(descriptor.fieldPath.size() - 1);
        var collectionSearchable = field.getAnnotation(CollectionSearchable.class);
        var subClass = collectionSearchable.targetType();

        // Resolve the path to the parent entity that owns the collection (this is the main
        // root, unless the collection is reached through a nested path) and its id field.
        var parentPath = descriptor.fieldPath.size() > 1 ? descriptor.fieldPath.subList(0, descriptor.fieldPath.size() - 1) : null;
        var rootParentDescriptor = parentPath != null ? new JPASearchCore.Descriptor(descriptor.searchType, parentPath) : null;
        var rootParentExpr = rootParentDescriptor != null ? getPath(cb, root, rootParentDescriptor) : root;
        var parentClass = rootParentExpr.getJavaType();
        var parentIdField = Arrays.stream(parentClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Id.class))
            .findFirst()
            .orElseThrow(() -> new JPASearchException("Cannot find id field for class " + parentClass.getName()));
        var parentIdFieldName = parentIdField.getName();

        // The subquery selects the parent id, so it must be declared with the id's type: the
        // IN comparison below is resolved from the subquery's declared type, not its select.
        Subquery<?> subquery = query.subquery(parentIdField.getType());
        Root<?> subRoot = subquery.from(subClass);

        var subFields = ReflectionUtils.getAllSearchableFields(Set.of(subClass));

        var op = Operator.load(node.get(0).textValue());
        var subValue = processValue(
                op,
                node.get(2),
                cb,
                subRoot,
                subquery,
                subClass,
                entityClasses,
                throwsIfNotExistsOrNotSearchable,
                subFields
        );


        // Second we need to join the result back to root query

        // Join from subquery root to parent entity (not necessarily the main root, but the parent of the collection)
        var oneToMany = field.getAnnotation(OneToMany.class);
        var backRefFieldName = collectionSearchable.mappedBy().isEmpty() ? oneToMany.mappedBy() : collectionSearchable.mappedBy();
        var join = subRoot.join(backRefFieldName, JoinType.INNER);

        subquery.select(join.get(parentIdFieldName)).where((Predicate) subValue);
        // Compare parent id to parent id: rootParentExpr is the parent entity, so the IN
        // must be built on its id, matching the id selected by the subquery.
        return ((Path<?>) rootParentExpr).get(parentIdFieldName).in(subquery);
    }

    private static Expression<?> processExpression(
            JsonNode node,
            CriteriaBuilder cb,
            Root<?> root,
            AbstractQuery<?> query,
            Class<?> entityClass,
            Set<Class<?>> entityClasses,
            boolean throwsIfNotExistsOrNotSearchable,
            Map<String, List<Field>> searchableFields
    ) {
        if (!node.isArray() || node.isEmpty() || !node.get(0).isTextual()) {
            throw new JPASearchException("Invalid expression");
        }

        var op = Operator.load(node.get(0).textValue());
        var arguments = new ArrayList<>();
        if (op.getName().equals("has")) {
            return processHasOperator(
                    node,
                    cb,
                    root,
                    query,
                    entityClasses,
                    throwsIfNotExistsOrNotSearchable,
                    searchableFields
            );
        }
        for (var i = 1; i < node.size(); i++) {
            var child = node.get(i);
            arguments.add(
                    processValue(
                            op,
                            child,
                            cb,
                            root,
                            query,
                            entityClass,
                            entityClasses,
                            throwsIfNotExistsOrNotSearchable,
                            searchableFields
                    )
            );
        }
        if (op.isEvaluateStrings()) {
            return op.getExprFunction().apply(cb, arguments.toArray(new Expression[0]));
        } else {
            return op.getObjFunction().apply(root, query, cb, arguments.toArray(), searchableFields);
        }
    }

    public static Sort loadSort(
            JsonNode filterPayload,
            Set<Class<?>> entityClasses,
            boolean throwsIfNotSortable,
            boolean throwsIfNotExistsOrNotSearchable
    ) {
        ArrayList<Sort.Order> orderSpecs = new ArrayList<>();
        var options = filterPayload.get("options");
        var searchableFields = ReflectionUtils.getAllSearchableFields(entityClasses);
        if (options != null) {
            var sortKeysNode = options.get("sortKey");
            if (sortKeysNode != null) {
                var keyList = new ArrayList<String>();
                if (sortKeysNode.isTextual()) {
                    keyList.add(sortKeysNode.asText());
                } else if (sortKeysNode.isArray()) {
                    for (var itm : sortKeysNode) {
                        keyList.add(itm.asText());
                    }
                }
                for (var sortKeyStr : keyList) {
                    var descending = false;
                    if (sortKeyStr.startsWith("-")) {
                        sortKeyStr = sortKeyStr.substring(1);
                        descending = true;
                    }

                    // noinspection unused: this is used for checking for sortability
                    var descriptor = loadDescriptor(
                            sortKeyStr,
                            throwsIfNotExistsOrNotSearchable,
                            true,
                            throwsIfNotSortable,
                            searchableFields
                    );

                    if (descending) {
                        orderSpecs.add(Sort.Order.desc(sortKeyStr));
                    } else {
                        orderSpecs.add(Sort.Order.asc(sortKeyStr));
                    }
                }
            }
        }
        return Sort.by(orderSpecs);
    }

    public static PageRequest loadSortAndPagination(
            JsonNode filterPayload,
            Class<?> entityClass,
            boolean throwsIfNotSortable,
            boolean throwsIfNotExistsOrSearchable
    ) {
        return loadSortAndPagination(
                filterPayload,
                entityClass,
                throwsIfNotSortable,
                throwsIfNotExistsOrSearchable,
                Collections.emptySet()
        );
    }

    public static PageRequest loadSortAndPagination(
            JsonNode filterPayload,
            Class<?> entityClass,
            boolean throwsIfNotSortable,
            boolean throwsIfNotExistsOrSearchable,
            Set<Class<?>> searchableSubclasses
    ) {
        HashSet<Class<?>> entityClasses = new HashSet<>(searchableSubclasses);
        entityClasses.add(entityClass);

        Integer pageSize = null;
        Integer pageOffset = null;
        Sort sort = null;

        var options = filterPayload.get("options");

        if (options != null) {
            sort = loadSort(
                    filterPayload,
                    entityClasses,
                    throwsIfNotSortable,
                    throwsIfNotExistsOrSearchable
            );

            var pageOffsetNode = options.get("pageOffset");
            if (pageOffsetNode != null) {
                pageOffset = pageOffsetNode.asInt();
            }
            var pageSizeNode = options.get("pageSize");
            if (pageSizeNode != null) {
                pageSize = pageSizeNode.asInt();
            }
        }

        if (pageSize == null) {
            throw new JPASearchException("Invalid or not present limit");
        }

        PageRequest result = PageRequest.ofSize(pageSize);
        if (pageOffset != null) {
            result = result.withPage(pageOffset);
        }
        if (sort != null) {
            result = result.withSort(sort);
        }

        return result;
    }

    public static Descriptor loadDescriptor(
            String key,
            boolean throwsIfNotExistsOrNotSortable,
            boolean checkSortable,
            boolean throwsIfNotSortable,
            Map<String, List<Field>> searchableFields
    ) {
        if (!searchableFields.containsKey(key)) {

            if (throwsIfNotExistsOrNotSortable) {
                throw new InvalidFieldException("Field [" + key + "] does not exists or not sortable", key);
            }

            return null;
        }

        var path = searchableFields.get(key);
        var field = path.get(path.size() - 1);
        var searchable = field.getAnnotation(Searchable.class);

        if (searchable == null) {
            var collectionSearchable = field.getAnnotation(CollectionSearchable.class);
            if (collectionSearchable == null) {
                return null;
            } else {
                return new Descriptor(SearchType.UNTYPED, path);
            }
        }

        if (checkSortable && !searchable.sortable()) {
            if (throwsIfNotSortable) {
                throw new InvalidFieldException("Field [" + key + "] is not sortable", key);
            }

            return null;
        }

        var searchType = SearchType.UNTYPED.equals(searchable.targetType())
                ? SearchType.load(field.getType(), SearchType.STRING)
                : searchable.targetType();

        return new Descriptor(searchType, path);
    }

    @Data
    @AllArgsConstructor
    public static class Descriptor {
        private SearchType searchType;
        private List<Field> fieldPath;
    }
}
