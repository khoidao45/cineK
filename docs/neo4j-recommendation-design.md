# Neo4j recommendation design (hybrid)

## Graph model
- Nodes:
  - `User {id, username}`
  - `Movie {id, title, genre, ratingAvg, views}`
  - `Genre {name}`
- Relationships:
  - `(u:User)-[:WATCHED {ts}]->(m:Movie)`
  - `(u:User)-[:LIKED]->(m:Movie)`
  - `(m:Movie)-[:HAS_GENRE]->(g:Genre)`
  - `(m1:Movie)-[:SIMILAR {score}]->(m2:Movie)`

## API
- `GET /api/recommendations/{userId}`

## Core logic
- Collaborative component (weight 0.5):
  - Find users with overlapping WATCHED movies
  - Recommend movies they watched that current user has not watched
- Genre component (weight 0.3):
  - Boost movies matching user preferred genres
- Trending component (weight 0.2):
  - Boost by recent watch velocity and movie views
- Additional boost:
  - High rating bonus based on `ratingAvg`

## Cypher queries

### 1) Find similar users
```cypher
MATCH (u:User {id: $userId})-[:WATCHED]->(m:Movie)<-[:WATCHED]-(other:User)
WHERE other.id <> $userId
WITH other, count(DISTINCT m) AS overlap
ORDER BY overlap DESC
LIMIT 30
RETURN other.id AS userId, overlap
```

### 2) Recommend unseen movies from similar users
```cypher
MATCH (u:User {id: $userId})
MATCH (sim:User)-[:WATCHED]->(cand:Movie)
WHERE sim.id IN $similarUserIds
  AND NOT (u)-[:WATCHED]->(cand)
WITH cand, count(*) AS collaborativeScore
RETURN cand.id AS movieId, collaborativeScore
ORDER BY collaborativeScore DESC
LIMIT $limit
```

### 3) Exclude watched movies explicitly
```cypher
MATCH (u:User {id: $userId})-[:WATCHED]->(m:Movie)
RETURN collect(m.id) AS watchedMovieIds
```

## PostgreSQL -> Neo4j sync
- Source of truth remains PostgreSQL
- Recommended sync options:
  - Debezium CDC from Postgres WAL to Kafka to Neo4j sink
  - Or periodic batch sync job every 1-5 minutes for startup phase
- Idempotent upsert strategy:
  - `MERGE` by business IDs for User/Movie/Genre and relationships

## Scaling strategy
- Precompute recommendations per active user (e.g., hourly)
- Cache top-N recommendations in Redis with short TTL (5-15 minutes)
- Warm cache for high-traffic users/movies
- Fallback to online compute when cache miss occurs
