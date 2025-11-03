package com.gisgro;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Concrete entity extending BaseMappedSuperclass to test
 * @MappedSuperclass field navigation.
 */
@Entity
@Table(name = "test_categories")
@NoArgsConstructor
public class TestCategory extends BaseMappedSuperclass {

    public TestCategory(Long id, String name, String description) {
        super(id, name, description);
    }
}
