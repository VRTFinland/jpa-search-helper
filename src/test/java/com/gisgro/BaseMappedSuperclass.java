package com.gisgro;

import com.gisgro.annotations.Searchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Test mapped superclass to verify fix for ClassCastException
 * when navigating through @MappedSuperclass fields.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMappedSuperclass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Searchable
    private String name;

    @Column(name = "description")
    @Searchable
    private String description;
}
