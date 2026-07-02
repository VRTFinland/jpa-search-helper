package com.gisgro;

import com.gisgro.annotations.CollectionSearchable;
import com.gisgro.annotations.NestedSearchable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity5 {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CollectionSearchable(targetType = TestEntity.class)
  @OneToMany(mappedBy = "entity5")
  private List<TestEntity> nestedList = new ArrayList<>();

  @NestedSearchable
  @ManyToOne
  private TestEntity entity1;

}
