package com.codek.movieauthservice;

import com.codek.movieauthservice.repository.neo4j.MovieNodeRepository;
import com.codek.movieauthservice.repository.neo4j.UserNodeRepository;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MovieAuthServiceApplicationTests {

    // Redis auto-config is excluded in application-test.properties, but any bean
    // that directly injects RedisConnectionFactory or StringRedisTemplate still
    // needs a mock provided here.
    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean @SuppressWarnings("rawtypes") ProxyManager rateLimitProxyManager;

    // Neo4j repositories are not registered (Neo4jConfig is @Profile("!test")),
    // but Neo4jSyncService and RecommendationService declare them as dependencies.
    @MockBean MovieNodeRepository movieNodeRepository;
    @MockBean UserNodeRepository userNodeRepository;

    @Test
    void contextLoads() {
    }

}
