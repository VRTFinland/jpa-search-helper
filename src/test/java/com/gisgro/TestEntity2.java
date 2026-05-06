package com.gisgro;

import com.gisgro.annotations.Searchable;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class TestEntity2 {

    public TestEntity2() {
    }

    @Id
    @Searchable
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
