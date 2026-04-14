package com.codek.movieauthservice.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("User")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNode {

    /** Maps 1-to-1 with the PostgreSQL users.id */
    @Id
    private Long id;

    private String username;

    @Relationship(type = "WATCHED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<WatchedRelationship> watched = new ArrayList<>();

    @Relationship(type = "RATED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<RatedRelationship> rated = new ArrayList<>();
}
