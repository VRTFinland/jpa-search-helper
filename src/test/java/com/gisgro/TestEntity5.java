package com.gisgro;

import com.gisgro.annotations.CollectionSearchable;
import com.gisgro.annotations.NestedSearchable;
import com.gisgro.annotations.Searchable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
