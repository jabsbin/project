package com.example.demo.restaurant;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {

    @Query("""
      SELECT r FROM RestaurantEntity r
      WHERE r.lng BETWEEN :west AND :east
        AND r.lat BETWEEN :south AND :north
        AND (:q IS NULL OR :q = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:category IS NULL OR :category = '' OR r.category = :category)
      """)
    List<RestaurantEntity> findInBBox(
            @Param("west") double west,
            @Param("south") double south,
            @Param("east") double east,
            @Param("north") double north,
            @Param("q") String q,
            @Param("category") String category,
            Pageable pageable
    );

    @Query("""
      SELECT COUNT(r) FROM RestaurantEntity r
      WHERE r.lng BETWEEN :west AND :east
        AND r.lat BETWEEN :south AND :north
        AND (:q IS NULL OR :q = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:category IS NULL OR :category = '' OR r.category = :category)
      """)
    long countInBBox(
            @Param("west") double west,
            @Param("south") double south,
            @Param("east") double east,
            @Param("north") double north,
            @Param("q") String q,
            @Param("category") String category
    );

    @Query(
            value = """
        SELECT 
          r.id, r.name, r.lat, r.lng, r.category, r.address, r.phone,
          (6371000 * acos(
             cos(radians(:lat)) * cos(radians(r.lat)) * cos(radians(r.lng) - radians(:lng)) +
             sin(radians(:lat)) * sin(radians(r.lat))
          )) AS distance
        FROM restaurants r
        WHERE r.lng BETWEEN :west AND :east
          AND r.lat BETWEEN :south AND :north
          AND (:q IS NULL OR :q = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:category IS NULL OR :category = '' OR r.category = :category)
        ORDER BY distance ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM restaurants r
        WHERE r.lng BETWEEN :west AND :east
          AND r.lat BETWEEN :south AND :north
          AND (:q IS NULL OR :q = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:category IS NULL OR :category = '' OR r.category = :category)
        """,
            nativeQuery = true
    )
    Page<Object[]> findInBBoxOrderByDistance(
            @Param("west") double west,
            @Param("south") double south,
            @Param("east") double east,
            @Param("north") double north,
            @Param("q") String q,
            @Param("category") String category,
            @Param("lat") double lat,
            @Param("lng") double lng,
            Pageable pageable
    );
}