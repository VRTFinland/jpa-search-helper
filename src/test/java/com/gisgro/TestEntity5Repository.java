package com.gisgro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TestEntity5Repository extends JpaRepository<TestEntity5, Long>, JpaSpecificationExecutor<TestEntity5> {

}
