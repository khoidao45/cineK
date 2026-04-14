package com.codek.movieauthservice.repository.neo4j;

import com.codek.movieauthservice.entity.neo4j.MovieNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieNodeRepository extends Neo4jRepository<MovieNode, Long> {

    /**
     * Collaborative filtering: tìm các movie mà những user "giống" target user đã xem,
     * nhưng target user chưa xem. Score = số similar users đã xem movie đó.
     */
    @Query("""
            MATCH (u:User {id: $userId})-[:WATCHED]->(m:Movie)<-[:WATCHED]-(similar:User)
            WHERE similar.id <> $userId
            WITH u, similar, count(m) AS commonMovies
            ORDER BY commonMovies DESC
            LIMIT 20
            MATCH (similar)-[:WATCHED]->(rec:Movie)
            WHERE rec.deleted = false
              AND NOT EXISTS { MATCH (u)-[:WATCHED]->(rec) }
            WITH rec, count(similar) AS collabScore
            ORDER BY collabScore DESC
            LIMIT $limit
            RETURN rec
            """)
    List<MovieNode> findCollaborativeRecommendations(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Content-based: tìm movie cùng genre/director với những gì user đã xem.
     */
    @Query("""
            MATCH (u:User {id: $userId})-[:WATCHED]->(w:Movie)
            WITH u, collect(DISTINCT w.genre) AS genres, collect(DISTINCT w.director) AS directors
            MATCH (rec:Movie)
            WHERE rec.deleted = false
              AND (rec.genre IN genres OR rec.director IN directors)
              AND NOT EXISTS { MATCH (u)-[:WATCHED]->(rec) }
            WITH rec, 1 AS contentScore
            ORDER BY contentScore DESC
            LIMIT $limit
            RETURN rec
            """)
    List<MovieNode> findContentBasedRecommendations(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Cold-start fallback: movie phổ biến chưa xem.
     */
    @Query("""
            MATCH (rec:Movie)
            WHERE rec.deleted = false
              AND NOT EXISTS { MATCH (u:User {id: $userId})-[:WATCHED]->(rec) }
            WITH rec, size([(u2:User)-[:WATCHED]->(rec) | u2]) AS watchCount
            ORDER BY watchCount DESC
            LIMIT $limit
            RETURN rec
            """)
    List<MovieNode> findColdStartRecommendations(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Trending: top movie được xem nhiều nhất.
     */
    @Query("""
            MATCH (rec:Movie)
            WHERE rec.deleted = false
            WITH rec, size([(u:User)-[:WATCHED]->(rec) | u]) AS watchCount
            ORDER BY watchCount DESC
            LIMIT $limit
            RETURN rec
            """)
    List<MovieNode> findTrendingMovieNodes(@Param("limit") int limit);
}
