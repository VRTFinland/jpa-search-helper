package com.gisgro;

import com.gisgro.annotations.NestedSearchable;
import com.gisgro.annotations.Searchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Test entity with a relationship to an entity that extends @MappedSuperclass.
 * Used to test nested searchable field navigation through @MappedSuperclass fields.
 */
@Entity
@Table(name = "test_entities_with_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestEntityWithCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Searchable
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @NestedSearchable
    private TestCategory category;
}
