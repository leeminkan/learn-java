package org.leeminkan.bookstore.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    // This method manually configures and runs Flyway.
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        // We configure Flyway using the data source it needs to run against
        return Flyway.configure()
                .dataSource(dataSource)
                // CRITICAL: Tells Flyway where to find the V1__Initial_Schema.sql file
                .locations("classpath:db/migration")
                .load();
    }
}