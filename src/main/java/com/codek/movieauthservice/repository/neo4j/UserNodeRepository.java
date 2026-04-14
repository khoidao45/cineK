package com.codek.movieauthservice.repository.neo4j;

import com.codek.movieauthservice.entity.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
}
