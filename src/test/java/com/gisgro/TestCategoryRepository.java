package com.gisgro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestCategoryRepository extends JpaRepository<TestCategory, Long>, JpaSpecificationExecutor<TestCategory> {
}
