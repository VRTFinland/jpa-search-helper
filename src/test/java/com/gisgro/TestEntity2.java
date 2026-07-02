package com.gisgro;

import com.gisgro.annotations.Searchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class TestEntity2 {

    public TestEntity2() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Searchable
    private String string;

    @ManyToOne
    private TestEntity entity1;

    public TestEntity2(Long id, String string) {
        this.id = id;
        this.string = string;
        this.entity1 = null;
    }
}
