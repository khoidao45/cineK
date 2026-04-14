package com.codek.movieauthservice.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Movie")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieNode {

    /** Maps 1-to-1 with the PostgreSQL movies.id */
    @Id
    private Long id;

    private String title;
    private String genre;
    private String director;
    private String actors;
    private Integer releaseYear;
    private boolean deleted;
}
