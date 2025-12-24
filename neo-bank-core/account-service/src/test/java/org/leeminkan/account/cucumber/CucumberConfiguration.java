package org.leeminkan.account.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",       // Stop Eureka calls
                "spring.cloud.discovery.enabled=false" // Stop Discovery
        }
)
// @Testcontainers  <-- REMOVE THIS (It relies on JUnit lifecycle which Cucumber might skip)
public class CucumberConfiguration {

    // 1. Define Containers as static final (Remove @Container)
    // @Container <-- REMOVE THIS
    @ServiceConnection // Keep this! It tells Spring to use these details
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    // @Container <-- REMOVE THIS
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    // 2. Manually Start them in a static block
    static {
        postgres.start();
        kafka.start();
    }
}