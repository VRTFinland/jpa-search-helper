package com.gisgro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestEntity3Repository extends JpaRepository<TestEntity3, Long> {}
