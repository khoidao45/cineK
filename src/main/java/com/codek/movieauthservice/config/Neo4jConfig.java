package com.codek.movieauthservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Profile("!test")
@EnableNeo4jRepositories(basePackages = "com.codek.movieauthservice.repository.neo4j")
@EnableTransactionManagement
public class Neo4jConfig {
    // Spring Boot auto-configures the driver via spring.neo4j.* properties.
    // This class pins the repository scan path to avoid conflicts with JPA repositories.
}
