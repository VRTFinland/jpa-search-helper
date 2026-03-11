package com.gisgro.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for collection fields that should be included in search criteria.
 * The 'mappedBy' attribute is optional when using @OneToMany with mappedBy
 * and can be used to specify the owning side of the relationship.
 * The 'targetType' attribute is required to specify the type of elements in the collection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CollectionSearchable {

  String mappedBy() default "";
  Class<?> targetType();

}
