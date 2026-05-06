package com.gisgro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisgro.model.Operator;
import com.gisgro.utils.JPAFuncWithObjects;
import com.gisgro.utils.ReflectionUtils;
import java.util.function.BiConsumer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@DataJpaTest
@ContextConfiguration(classes = {JpaSearchTests.class})
@EnableAutoConfiguration
@TestPropertySource(properties = {
        "spring.jpa.show-sql=true",
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
public class JpaSearchTests {
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TestEntityRepository testEntityRepository;
    @Autowired
    private TestEntity2Repository testEntity2Repository;
    @Autowired
    private TestEntity3Repository testEntity3Repository;
    @Autowired
    private TestEntity4Repository testEntity4Repository;
    @Autowired
    private TestEntity5Repository testEntity5Repository;
    @Autowired
    private ParentEntityRepository parentEntityRepository;
    @Autowired
    private TestCategoryRepository testCategoryRepository;
    @Autowired
    private TestEntityWithCategoryRepository testEntityWithCategoryRepository;

    private TestEntity setup() {
        var ent2 = new TestEntity2(
                0L,
                "Nested! daa dumdidum"
        );
        ent2 = testEntity2Repository.save(ent2);
        // Create TestEntity first
        TestEntity ent = new TestEntity(
                0L,
                6,
                null,
                "asdf",
                "test@test.fi",
                "",
                "20240609",
                new Date(2024, Calendar.MARCH, 1),
                new Date(2024, Calendar.MARCH, 1),
                1L,
                10L,
                1.35F,
                5.6F,
                1.3,
                2.3,
                new BigDecimal("1.23"),
                LocalDateTime.now(),
                LocalDate.now(),
                LocalTime.now(),
                OffsetDateTime.now(),
                OffsetTime.now(),
                "fieldName",
                false,
                true,
                ent2,
                new HashSet<>(),
                TestEnum.VALUE1,
                Period.parse("P6M"),
                null
        );
        ent = testEntityRepository.save(ent);
        // Now create set0 and set1, set their parent to ent
        var set0 = new TestEntity2(
                0L,
                "nestedSet0",
                ent
        );
        set0 = testEntity2Repository.save(set0);
        var set1 = new TestEntity2(
                0L,
                "nestedSet1",
                ent
        );
        set1 = testEntity2Repository.save(set1);
        // Update ent's nestedSet and save again
        ent.setNestedSet(new HashSet<>(Arrays.asList(set0, set1)));
        return testEntityRepository.save(ent);
    }

    private void setup2() {
        var ent2a = new TestEntity2(
                0L,
                "nested1"
        );
        ent2a = testEntity2Repository.save(ent2a);
        var ent2b = new TestEntity2(
                0L,
                "nested2"
        );
        ent2b = testEntity2Repository.save(ent2b);

        TestEntity ent = new TestEntity(
                0L,
                6,
                null,
                "asdf",
                "test@test.fi",
                "",
                "20240609",
                new Date(2024, Calendar.MARCH, 1),
                new Date(2024, Calendar.MARCH, 1),
                1L,
                10L,
                1.35F,
                5.6F,
                1.3,
                2.3,
                new BigDecimal("1.23"),
                LocalDateTime.now(),
                LocalDate.now(),
                LocalTime.now(),
                OffsetDateTime.now(),
                OffsetTime.now(),
                "fieldName",
                false,
                true,
                ent2a,
                Collections.emptySet(),
                TestEnum.VALUE1,
                Period.parse("P12M"),
                null
        );
        testEntityRepository.save(ent);
        ent = new TestEntity(
                0L,
                7,
                null,
                "asdf",
                "test@test.fi",
                "",
                "20240609",
                new Date(2024, Calendar.MARCH, 1),
                new Date(2024, Calendar.MARCH, 1),
                1L,
                10L,
                1.35F,
                5.6F,
                1.3,
                2.3,
                new BigDecimal("1.23"),
                LocalDateTime.now(),
                LocalDate.now(),
                LocalTime.now(),
                OffsetDateTime.now(),
                OffsetTime.now(),
                "fieldName",
                false,
                true,
                ent2b,
                Collections.emptySet(),
                TestEnum.VALUE2,
                Period.parse("P6M"),
                null
        );
        testEntityRepository.save(ent);
    }

    private void setup3() {
        var foo = testEntity3Repository.save(new TestEntity3(0L, "parentFoo", "foo", null));
        testEntity3Repository.save(new TestEntity3(0L, "parentBar", "bar", foo));
    }

    private void setup4() {
        var foo = testEntity4Repository.save(new TestEntity4(0L, "parentFoo", "foo", null));
        testEntity4Repository.save(new TestEntity4(0L, "parentBar", "bar", foo));
    }

    private void setup5() {
        var entity = setup();
        var entity5 = new TestEntity5();
        entity5.setEntity1(entity);
        testEntity5Repository.save(entity5);
        entity.setEntity5(entity5);
        testEntityRepository.save(entity);
        List<TestEntity> nestedList = new ArrayList<>();
        nestedList.add(entity);
        entity5.setNestedList(nestedList);
        entity5.setEntity1(entity);
        testEntity5Repository.save(entity5);
    }

    private void setupMappedSuperclass() {
        var category1 = testCategoryRepository.save(
            new TestCategory(0L, "office rentals", "Office space rental contracts")
        );
        var category2 = testCategoryRepository.save(
            new TestCategory(0L, "equipment leases", "Equipment leasing contracts")
        );

        testEntityWithCategoryRepository.save(
            new TestEntityWithCategory(0L, "Contract A", category1)
        );
        testEntityWithCategoryRepository.save(
            new TestEntityWithCategory(0L, "Contract B", category2)
        );
    }

    private <T> Specification<T> specificationFrom(String filterString, Class<T> clazz) {
        return specificationFrom(filterString, clazz, Collections.emptySet());
    }

    @SneakyThrows
    private <T> Specification<T> specificationFrom(
            String filterString,
            Class<T> clazz,
            Set<Class<?>> searchableSubclasses
    ) {
        JsonNode filters = mapper.readTree(filterString);
        return JPASearchCore.specification(
                filters,
                clazz,
                true,
                searchableSubclasses
        );
    }

    private <T> PageRequest pageRequestFrom(String filterString, Class<T> clazz) {
        return pageRequestFrom(filterString, clazz, Collections.emptySet());
    }

    @SneakyThrows
    private <T> PageRequest pageRequestFrom(
            String filterString,
            Class<T> clazz,
            Set<Class<?>> searchableSubclasses
    ) {
        JsonNode filters = mapper.readTree(filterString);
        return JPASearchCore.loadSortAndPagination(
                filters,
                clazz,
                true,
                true,
                searchableSubclasses
        );
    }

    @Test
    public void testLinked() {
        setup3();

        var filterString = """
                    {"filter":
                        ["and",
                         ["eq", ["field", "parentField"], "parentBar"],
                         ["eq", ["field", "payload"], "bar"],
                         ["eq", ["field", "previous.payload"], "foo"]
                        ]}
                """;

        List<TestEntity3> result = testEntity3Repository.findAll(specificationFrom(filterString, TestEntity3.class));
        assertThat(result).hasSize(1);
    }

    @Test
    public void testParent() {
        setup3();
        setup4();

        List<ParentEntity> result1 = parentEntityRepository.findAll(
                specificationFrom(
                        """
                                    {"filter":
                                        ["and",
                                         ["eq", ["field", "parentField"], "parentBar"],
                                         ["eq", ["field", "payload"], "bar"],
                                         ["eq", ["field", "previous.payload"], "foo"]
                                        ]}
                                """,
                        ParentEntity.class,
                        Set.of(TestEntity3.class, TestEntity4.class)
                )
        );

        assertThat(result1).hasSize(1);

        List<ParentEntity> result2 = parentEntityRepository.findAll(
                specificationFrom(
                        """
                                    {"filter":
                                        ["and",
                                         ["eq", ["field", "parentField"], "parentBar"],
                                         ["eq", ["field", "totallyNotPayload"], "bar"],
                                         ["eq", ["field", "previous.totallyNotPayload"], "foo"]
                                        ]}
                                """,
                        ParentEntity.class,
                        Set.of(TestEntity3.class, TestEntity4.class)
                )
        );

        assertThat(result2).hasSize(1);
    }

    @Test
    public void testAllFilters() {
        setup();
        var filterString = """
                       {
                         "filter":
                 ["and",
                   ["and",
                     ["and",
                       ["eq", ["field","primitiveInteger"], 6],
                       ["eq", ["lower" , ["field","email"]], "test@test.fi"],
                       ["lt", ["field" ,"primitiveLong"], 10],
                       ["in", ["field","primitiveDouble"], 1.3, 1.4],
                       ["between", ["field","primitiveFloat"], 1.3, 1.4],
                       ["lte", ["field","wrapperLong"], 10],
                       ["not", ["in", ["field","wrapperDouble"], 1.3, 1.4]],
                       ["isNull", ["field","wrapperInteger"]],
                       ["eq", ["field","integerString"], ""],
                       ["eq", ["field","testEnum"], ["enum", "TestEnum", "VALUE1"]],
                       ["eq", ["field","period"], ["period", "P6M"]]
                     ]
                   ],
                   ["and",
                     ["not", ["isNull", ["field" ,"dateString"]]],
                     ["not", ["eq", ["field","bigDecimal"],  ["bigDecimal", "1.35"]]],
                     ["eq", ["field","bigDecimal"],  ["bigDecimal", "1.23"]],
                     ["eq", ["field","nested.string"], "Nested! daa dumdidum"],
                     ["not", ["eq", ["lower", ["field","nested.string"]], "blaa!"]],
                     ["startsWith", ["field","nested.string"], "Nested!"],
                     ["startsWith", ["lower" ,["field","nested.string"]], "nested!"],
                     ["contains", ["field","nested.string"], "Nested!"],
                     ["contains", ["lower",["field","nested.string"]], "nested!"],
                     ["endsWith", ["field","nested.string"], "dum"],
                     ["endsWith", ["lower",["field","nested.string"]], "dum"]
                   ]
                   ]            
                
                }
                """;

        /*
             H2 does not support STR_TO_DATE function, so can't test this

        ["gt", "date1", ["date", "2018-04-26T15:41:49Z"]],
        ["gte", "date2", ["date", "2018-04-26T15:41:49Z"]],

        */

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testSimple() {
        setup();
        var filterString = """
                {
                 "filter": ["eq", ["field", "primitiveInteger"], 6]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNullParsing() {
        /* this test only verifies that null parsing, once
           broken in feature/subclass-fields, works again */

        var filterString = """
                {
                 "filter": ["isNull", null]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(0);
    }

    @Test
    public void testNested() {
        setup();
        var filterString = """
                {
                 "filter": ["eq", ["field", "nested.string"], "Nested! daa dumdidum"]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testOrOperator() {
        setup();
        var filterString = """
                {
                 "filter": ["or",
                  ["eq", ["field", "primitiveInteger"], 6],
                  ["eq", ["field", "primitiveInteger"], 7]
                 ]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNotOperator() {
        setup();
        var filterString = """
                {"filter": ["not",
                  ["eq", ["field", "primitiveInteger"], 7]
                ]}
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testEnum() {
        setup2();
        var filterString = """
                {"filter": ["eq", ["field", "testEnum"], ["enum", "TestEnum", "VALUE1"]]}
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testSort() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["primitiveInteger"],
                  "pageSize": 2,
                  "pageOffset": 0
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(6);
        assertThat(result.getContent().get(1).getPrimitiveInteger()).isEqualTo(7);
    }

    @Test
    public void testSortDesc() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["-primitiveInteger"],
                  "pageSize": 2,
                  "pageOffset": 0
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(7);
        assertThat(result.getContent().get(1).getPrimitiveInteger()).isEqualTo(6);
    }

    @Test
    public void testSortDesc2() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": "-primitiveInteger",
                  "pageSize": 2,
                  "pageOffset": 0
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(7);
        assertThat(result.getContent().get(1).getPrimitiveInteger()).isEqualTo(6);
    }

    @Test
    public void testSortLimit() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["primitiveInteger"],
                  "pageSize": 1,
                  "pageOffset": 0
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(6);
    }

    @Test
    public void testSortLimit2() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["primitiveInteger"],
                  "pageSize": 1,
                  "pageOffset": 1
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(7);
    }

    @Test
    public void testNestedSortLimit() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["nested.string"],
                  "pageSize": 1,
                  "pageOffset": 1
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(7);
    }

    @Test
    public void testNestedSortLimitDesc() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["-nested.string"],
                  "pageSize": 1,
                  "pageOffset": 1
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(6);
    }

    @Test
    public void testNestedSortLimitMultipleCriteria() {
        setup2();
        var filterString = """
                {"filter": ["gte", ["field", "primitiveInteger"], 6],
                "options": {
                  "sortKey": ["primitiveInteger", "-nested.string"],
                  "pageSize": 1,
                  "pageOffset": 1
                }
                }
                """;

        Page<TestEntity> result = testEntityRepository.findAll(
                specificationFrom(filterString, TestEntity.class),
                pageRequestFrom(filterString, TestEntity.class)
        );
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPrimitiveInteger()).isEqualTo(7);
    }

    @Test
    public void testOwnBooleanOperator() {
        setup2();

        JPAFuncWithObjects<Boolean> func = (root, query, cb, values, searchableFields) -> {
            var nested = root.join("nested", JoinType.LEFT);
            return cb.equal(nested.get("string"), values[0]);
        };

        Operator.addOperator(new Operator("ownFunc", func));

        var filterString = """
                {"filter": ["ownFunc", "nested2"]}
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));
        assertThat(result.get(0).getPrimitiveInteger()).isEqualTo(7);
    }

    @Test
    public void testOwnStringOperator() {
        setup2();

        JPAFuncWithObjects<String> func = (root, query, cb, values, searchableFields) -> {
            var nested = root.join("nested", JoinType.LEFT);
            return cb.concat(nested.get("string"), cb.literal((String) values[0]));
        };

        Operator.addOperator(new Operator("ownFunc2", func));

        var filterString = """
                {"filter": ["eq", ["ownFunc2", "blah"], "nested2blah"]}
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));
        assertThat(result.get(0).getPrimitiveInteger()).isEqualTo(7);
    }

    /**
     * Test that nested searchable fields from @MappedSuperclass work correctly.
     * This test verifies the fix for ClassCastException when navigating through
     * fields declared in @MappedSuperclass (e.g., category.name where name is
     * declared in BaseMappedSuperclass).
     */
    @Test
    public void testMappedSuperclassNestedSearch() {
        setupMappedSuperclass();

        var filterString = """
                {
                 "filter": ["eq", ["field", "category.name"], "office rentals"]
                }
                """;

        List<TestEntityWithCategory> result = testEntityWithCategoryRepository.findAll(
            specificationFrom(filterString, TestEntityWithCategory.class)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Contract A");
        assertThat(result.get(0).getCategory().getName()).isEqualTo("office rentals");
    }

    /**
     * Test that case-insensitive search works with @MappedSuperclass fields.
     */
    @Test
    public void testMappedSuperclassNestedSearchCaseInsensitive() {
        setupMappedSuperclass();

        var filterString = """
                {
                 "filter": ["eq", ["lower", ["field", "category.name"]], "office rentals"]
                }
                """;

        List<TestEntityWithCategory> result = testEntityWithCategoryRepository.findAll(
            specificationFrom(filterString, TestEntityWithCategory.class)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Contract A");
    }

    /**
     * Test that description field (also from @MappedSuperclass) works correctly.
     */
    @Test
    public void testMappedSuperclassNestedSearchDescription() {
        setupMappedSuperclass();

        var filterString = """
                {
                 "filter": ["contains", ["field", "category.description"], "rental"]
                }
                """;

        List<TestEntityWithCategory> result = testEntityWithCategoryRepository.findAll(
            specificationFrom(filterString, TestEntityWithCategory.class)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Contract A");
        assertThat(result.get(0).getCategory().getDescription()).contains("rental");
    }

    @Test
    public void testNestedSetEqual() {
        setup();
        var filterString = """
                {
                 "filter": ["has", "nestedSet", ["eq", ["field", "string"], "nestedSet0"]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNestedSetEqualAndEqual() {
        setup();
        // This filter looks for TestEntity that has a nestedSet item with string equal to "nestedSet0" AND string equal to "nestedSet1"
        var filterString = """
                {
                 "filter": ["has", "nestedSet", ["and",
                   ["eq", ["field", "string"], "nestedSet0"],
                   ["eq", ["field", "string"], "nestedSet1"]]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(0);
    }

    @Test
    public void testNestedSetEqualAndEqual2() {
        setup5();
        // This filter looks for TestEntity that has a nestedSet item with string equal to "nestedSet0" AND string equal to "nestedSet1"
        var filterString = """
                {
                 "filter": ["has", "nestedList", ["and",
                   ["eq", ["field", "email"], "test@test.fi"],
                   ["eq", ["field", "dateString"], "20240609"]]]
                }
                """;

        List<TestEntity5> result = testEntity5Repository.findAll(specificationFrom(filterString, TestEntity5.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNestedSetEqualOrEqual() {
        setup5();
        // This filter looks for TestEntity that has a nestedSet item with string equal to "nestedSet0" AND string equal to "nestedSet1"
        var filterString = """
                {
                 "filter": ["has", "nestedList", ["or",
                   ["eq", ["field", "email"], "t@t.fi"],
                   ["eq", ["field", "dateString"], "20240609"]]]
                }
                """;

        List<TestEntity5> result = testEntity5Repository.findAll(specificationFrom(filterString, TestEntity5.class));

        assertThat(result).hasSize(1);
    }
    @Test
    public void testNestedSetEqualAndNestedSetEqual() {
        setup();
        // This filter looks for TestEntity that has a nestedSet item with string equal to "nestedSet0" and another nestedSet item with string equal to "nestedSet1"
        var filterString = """
                {
                 "filter": ["and",
                   ["has", "nestedSet", ["eq", ["field", "string"], "nestedSet0"]],
                   ["has", "nestedSet", ["eq", ["field", "string"], "nestedSet1"]]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNestedSetContains() {
        setup();
        var filterString = """
                {
                 "filter": ["has", "nestedSet", ["contains", ["field", "string"], "nested"]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNestedSetIsNull() {
        setup();
        var filterString = """
                {
                 "filter": ["has", "nestedSet", ["isNull", ["field", "string"]]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(0);

        var filterString2 = """
                {
                 "filter": ["isNull", ["field", "string"]]
                }
                """;
        List<TestEntity2> result2 = testEntity2Repository.findAll(specificationFrom(filterString2, TestEntity2.class));

        assertThat(result2).hasSize(0);
    }

    @Test
    public void testNestedSetWithoutMatch() {
        setup();
        var filterString = """
                {
                 "filter": ["not", ["has", "nestedSet", ["isNull", ["field", "string"]]]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testNestedSetNot() {
        setup();
        var filterString = """
                {
                 "filter": ["has", "nestedSet", ["not", ["isNull", ["field", "string"]]]]
                }
                """;

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testDoublyNestedSetEqual() {
        setup5();
        var filterString = """
                {
                 "filter": ["has", "nestedList",
                   ["has", "nestedSet", ["eq", ["field", "string"], "nestedSet0"]]]
                }
                """;

        List<TestEntity5> result = testEntity5Repository.findAll(specificationFrom(filterString, TestEntity5.class));

        assertThat(result).hasSize(1);
    }

    @Test
    public void testDoublyNestedSetNot() {
        setup5();
        BiConsumer<String, Integer> runTest = (searchString, expectedSize) -> {
            String filterString = String.format("""
                {
                 "filter": ["has", "nestedList",
                   ["has", "nestedSet", ["not", ["contains", ["field", "string"], "%s"]]]]
                }
                """, searchString);

            List<TestEntity5> result = testEntity5Repository.findAll(specificationFrom(filterString, TestEntity5.class));
            assertThat(result).hasSize(expectedSize);
        };

        runTest.accept("nestedSet", 0);
        runTest.accept("nestedSet0", 1);
    }

    @Test
    public void testHasWithNestedField() {
        setup5();

        var filterString = """
                {
                 "filter": ["has", "entity1.nestedSet", ["contains", ["field", "string"], "nestedSet"]]
                }
                """;

        List<TestEntity5> result = testEntity5Repository.findAll(specificationFrom(filterString, TestEntity5.class));

        assertThat(result).hasSize(1);
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that @NestedSearchable and @CollectionSearchable fields don't collide.
     * Ensures collection element fields are namespaced (e.g., "nestedSet.id" instead of "id").
     */
    @Test
    public void testFieldsAreNotShadowedByCollectionElements() {
        // Setup: TestEntity has both @NestedSearchable nested and @CollectionSearchable nestedSet
        // Both point to TestEntity2 which has an "id" field
        var fieldMap = ReflectionUtils.getAllSearchableFields(Set.of(TestEntity.class));

        // Core assertion: "id" should resolve to TestEntity.id, not TestEntity2.id
        assertThat(fieldMap)
            .as("Field map should contain top-level id")
            .containsKey("id");

        var topLevelIdPath = fieldMap.get("id");
        assertThat(topLevelIdPath)
            .as("id should resolve to TestEntity.id")
            .isNotEmpty();
        assertThat(topLevelIdPath.get(topLevelIdPath.size() - 1).getDeclaringClass())
            .as("Top-level id should come from TestEntity, not TestEntity2")
            .isEqualTo(TestEntity.class);
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that nested fields and collection fields are registered with unique paths.
     * Collection elements should be namespaced with their collection field name.
     */
    @Test
    public void testNestedAndCollectionFieldsHaveUniquePaths() {
        var fieldMap = ReflectionUtils.getAllSearchableFields(Set.of(TestEntity.class));

        // Verify nested fields are registered correctly
        assertThat(fieldMap)
            .as("Field map should have nested.id from @NestedSearchable")
            .containsKey("nested.id");

        // Verify collection fields are namespaced with collection field name
        assertThat(fieldMap)
            .as("Field map should have nestedSet.id from @CollectionSearchable with field name prefix")
            .containsKey("nestedSet.id");

        // Verify they're different entries
        var nestedIdPath = fieldMap.get("nested.id");
        var nestedSetIdPath = fieldMap.get("nestedSet.id");

        assertThat(nestedIdPath)
            .as("nested.id path should exist")
            .isNotNull();
        assertThat(nestedSetIdPath)
            .as("nestedSet.id path should exist")
            .isNotNull();

        // Both should reference TestEntity2.id but via different paths
        assertThat(nestedIdPath.get(nestedIdPath.size() - 1).getDeclaringClass())
            .as("nested.id should reference TestEntity2")
            .isEqualTo(TestEntity2.class);
        assertThat(nestedSetIdPath.get(nestedSetIdPath.size() - 1).getDeclaringClass())
            .as("nestedSet.id should reference TestEntity2")
            .isEqualTo(TestEntity2.class);
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that field registration is deterministic across multiple runs.
     * Ensures HashSet iteration randomness doesn't affect field path resolution.
     */
    @Test
    public void testFieldRegistrationIsDeterministic() {
        // Run field discovery multiple times and verify consistency
        Map<String, Class<?>> baselineResults = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            var fieldMap = ReflectionUtils.getAllSearchableFields(Set.of(TestEntity.class));

            // Check that id always resolves to TestEntity (not TestEntity2)
            var idPath = fieldMap.get("id");
            assertThat(idPath)
                .as("Run " + i + ": id field should exist")
                .isNotNull();

            var idDeclaringClass = idPath.get(idPath.size() - 1).getDeclaringClass();

            if (i == 0) {
                baselineResults.put("id", idDeclaringClass);
            } else {
                assertThat(idDeclaringClass)
                    .as("Run " + i + ": id should consistently resolve to " + baselineResults.get("id"))
                    .isEqualTo(baselineResults.get("id"));
            }
        }

        // Verify all runs resolved id to TestEntity
        assertThat(baselineResults.get("id"))
            .as("All runs should resolve id to TestEntity")
            .isEqualTo(TestEntity.class);
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that queries can resolve fields correctly after the fix.
     * Ensures loadDescriptor works with both nested and collection field paths.
     */
    @Test
    public void testLoadDescriptorResolvesFieldsCorrectly() {
        var fieldMap = ReflectionUtils.getAllSearchableFields(Set.of(TestEntity.class));

        // Test: top-level field lookup
        var idDescriptor = JPASearchCore.loadDescriptor("id", true, false, false, fieldMap);
        assertThat(idDescriptor)
            .as("Should find top-level id field")
            .isNotNull();
        if (idDescriptor != null) {
            assertThat(idDescriptor.getFieldPath().get(0).getDeclaringClass())
                .as("id should resolve to TestEntity")
                .isEqualTo(TestEntity.class);
        }

        // Test: nested field lookup
        var nestedIdDescriptor = JPASearchCore.loadDescriptor("nested.id", true, false, false, fieldMap);
        assertThat(nestedIdDescriptor)
            .as("Should find nested.id field")
            .isNotNull();
        if (nestedIdDescriptor != null) {
            assertThat(nestedIdDescriptor.getFieldPath().get(nestedIdDescriptor.getFieldPath().size() - 1).getDeclaringClass())
                .as("nested.id should resolve to TestEntity2")
                .isEqualTo(TestEntity2.class);
        }

        // Test: collection field lookup
        var collectionIdDescriptor = JPASearchCore.loadDescriptor("nestedSet.id", true, false, false, fieldMap);
        assertThat(collectionIdDescriptor)
            .as("Should find nestedSet.id field")
            .isNotNull();
        if (collectionIdDescriptor != null) {
            assertThat(collectionIdDescriptor.getFieldPath().get(collectionIdDescriptor.getFieldPath().size() - 1).getDeclaringClass())
                .as("nestedSet.id should resolve to TestEntity2")
                .isEqualTo(TestEntity2.class);
        }
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that searches using collection fields work correctly.
     * This specifically targets the scenario from PR #14683.
     */
    @Test
    @SneakyThrows
    public void testSearchingByIdOnEntityWithCollectionDoesNotShadow() {
        // This test verifies the fix for PR #14683
        // When @CollectionSearchable is added to an entity with nested relationships,
        // searching for "id" on the parent entity should not be shadowed by child entity ids

        var setup = setup();  // Sets up TestEntity with nestedSet

        // Search for top-level entity by id
        var jsonFilter = """
                {
                 "filter": ["eq", ["field", "id"], %d],
                 "options": {"pageOffset": 0, "pageSize": 10}
                }
                """.formatted(setup.getId());
        Specification<TestEntity> spec = specificationFrom(jsonFilter, TestEntity.class);

        var results = testEntityRepository.findAll(spec);

        // Should find exactly the setup entity by its id (not shadowed by nested entities)
        assertThat(results)
            .as("Search for top-level TestEntity.id should find exactly one result (not shadowed)")
            .hasSize(1);
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that both nested and collection searches work independently.
     * Ensures separation of concerns between nested and collection field paths.
     */
    @Test
    @SneakyThrows
    public void testBothNestedAndCollectionFieldsAreSearchable() {
        var setup = setup();

        // Verify setup has both nested and collection entities
        assertThat(setup.getNested()).isNotNull();
        assertThat(setup.getNestedSet()).isNotEmpty();

        // Search via nested field (should use dot notation)
        var nestedJson = """
                {
                 "filter": ["eq", ["field", "nested.string"], "Nested! daa dumdidum"],
                 "options": {"pageOffset": 0, "pageSize": 10}
                }
                """;
        Specification<TestEntity> nestedSpec = specificationFrom(nestedJson, TestEntity.class);

        var nestedResults = testEntityRepository.findAll(nestedSpec);
        assertThat(nestedResults)
            .as("Search by nested field should find the entity")
            .hasSize(1);

        // Search via collection field
        // Note: This uses the "has" operator which is specific to @CollectionSearchable
        var collectionJson = """
                {
                 "filter": ["has", "nestedSet", ["contains", ["field", "string"], "nestedSet"]],
                 "options": {"pageOffset": 0, "pageSize": 10}
                }
                """;
        Specification<TestEntity> collectionSpec = specificationFrom(collectionJson, TestEntity.class);

        var collectionResults = testEntityRepository.findAll(collectionSpec);
        assertThat(collectionResults)
            .as("Search by collection field should find the entity")
            .hasSize(1);
    }

    /**
     * REGRESSION TEST FOR FIELD SHADOWING BUG
     *
     * Tests that searching by id on an entity with @CollectionSearchable fields
     * does not shadow the parent entity's id with child entity ids.
     * Uses the declarative filter syntax like testSimple and testNested tests.
     */
    @Test
    public void testIdFieldNotShadowedByCollectionSearchable() {
        var setup = setup();  // Sets up TestEntity with nestedSet
        
        var filterString = """
                {
                 "filter": ["eq", ["field", "id"], %d]
                }
                """.formatted(setup.getId());

        List<TestEntity> result = testEntityRepository.findAll(specificationFrom(filterString, TestEntity.class));

        assertThat(result)
            .as("Search for top-level TestEntity.id should find exactly one result (not shadowed by TestEntity2.id from nestedSet)")
            .hasSize(1);
        assertThat(result.get(0).getId())
            .as("Result should be the entity we searched for")
            .isEqualTo(setup.getId());
    }
}
