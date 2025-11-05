package com.example.demo.hotspot;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotspotRepository extends JpaRepository<HotspotEntity, Long> {

    @Query("""
      SELECT h FROM HotspotEntity h
      WHERE h.lng BETWEEN :west AND :east
        AND h.lat BETWEEN :south AND :north
        AND (:year IS NULL OR h.statYear = :year)
        AND (:q IS NULL OR :q = '' OR LOWER(h.name) LIKE LOWER(CONCAT('%', :q, '%')))
      """)
    List<HotspotEntity> findInBBox(
            @Param("west") double west,
            @Param("south") double south,
            @Param("east") double east,
            @Param("north") double north,
            @Param("year") Integer year,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
      SELECT COUNT(h) FROM HotspotEntity h
      WHERE h.lng BETWEEN :west AND :east
        AND h.lat BETWEEN :south AND :north
        AND (:year IS NULL OR h.statYear = :year)
        AND (:q IS NULL OR :q = '' OR LOWER(h.name) LIKE LOWER(CONCAT('%', :q, '%')))
      """)
    long countInBBox(
            @Param("west") double west,
            @Param("south") double south,
            @Param("east") double east,
            @Param("north") double north,
            @Param("year") Integer year,
            @Param("q") String q
    );

    @Query(
            value = """
        SELECT 
          h.id, h.name, h.lat, h.lng, h.accidents, h.casualties, h.year AS statYear,
          (6371000 * acos(
             cos(radians(:lat)) * cos(radians(h.lat)) * cos(radians(h.lng) - radians(:lng)) +
             sin(radians(:lat)) * sin(radians(h.lat))
          )) AS distance
        FROM hotspots h
        WHERE h.lng BETWEEN :west AND :east
          AND h.lat BETWEEN :south AND :north
          AND (:year IS NULL OR h.year = :year)
          AND (:q IS NULL OR :q = '' OR LOWER(h.name) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY distance ASC, h.accidents DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM hotspots h
        WHERE h.lng BETWEEN :west AND :east
          AND h.lat BETWEEN :south AND :north
          AND (:year IS NULL OR h.year = :year)
          AND (:q IS NULL OR :q = '' OR LOWER(h.name) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
            nativeQuery = true
    )
    Page<Object[]> findInBBoxOrderByDistance(
            @Param("west") double west,
            @Param("south") double south,
            @Param("east") double east,
            @Param("north") double north,
            @Param("year") Integer year,
            @Param("q") String q,
            @Param("lat") double lat,
            @Param("lng") double lng,
            Pageable pageable
    );
}
