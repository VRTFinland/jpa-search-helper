package com.gisgro;

import com.gisgro.annotations.NestedSearchable;
import com.gisgro.annotations.Searchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class TestEntity3 {

    public TestEntity3() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Searchable
    private String string;
}
