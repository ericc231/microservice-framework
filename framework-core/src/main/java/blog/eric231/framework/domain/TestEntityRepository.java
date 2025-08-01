package blog.eric231.framework.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
}
