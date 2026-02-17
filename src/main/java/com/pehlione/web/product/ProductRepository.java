package com.pehlione.web.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    @EntityGraph(attributePaths = { "categories", "images" })
    Optional<Product> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);

    @Query("""
            select p.id from Product p
            where p.status = :status
            """)
    Page<Long> findIdsByStatus(@Param("status") ProductStatus status, Pageable pageable);

    @Query("""
            select p.id from Product p
            where p.status = :status
            and lower(p.name) like lower(concat('%', :q, '%'))
            """)
    Page<Long> findIdsByStatusAndNameContainingIgnoreCase(
            @Param("status") ProductStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            select distinct p from Product p
            left join fetch p.categories
            left join fetch p.images
            where p.id in :ids
            """)
    List<Product> findWithDetailsByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            select distinct p from Product p
            left join fetch p.categories
            left join fetch p.images
            join p.categories c
            where p.status = :status and c.slug = :slug
            """)
    List<Product> findActiveByCategorySlugFetchAll(
            @Param("status") ProductStatus status,
            @Param("slug") String slug);
}
