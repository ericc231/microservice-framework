package blog.eric231.framework;

import blog.eric231.framework.domain.TestEntity;
import blog.eric231.framework.domain.TestEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "jasypt.encryptor.enabled=false")
public class DatabaseConnectionTest {

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Test
    void testDatabaseConnection() {
        TestEntity entity = new TestEntity();
        entity.setName("test");

        TestEntity savedEntity = testEntityRepository.save(entity);
        assertThat(savedEntity).isNotNull();
        assertThat(savedEntity.getId()).isNotNull();

        TestEntity foundEntity = testEntityRepository.findById(savedEntity.getId()).orElse(null);
        assertThat(foundEntity).isNotNull();
        assertThat(foundEntity.getName()).isEqualTo("test");
    }
}
