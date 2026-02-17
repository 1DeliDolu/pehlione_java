package com.pehlione.web.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

	@Query("""
			select distinct ci from CartItem ci
			join fetch ci.product p
			left join fetch p.images
			where ci.user.id = :userId
			order by ci.updatedAt desc
			""")
	List<CartItem> findWithProductAndImagesByUserId(@Param("userId") Long userId);

	List<CartItem> findByUserId(Long userId);

	@Modifying
	@Query("delete from CartItem ci where ci.user.id = :userId")
	int deleteAllByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("delete from CartItem ci where ci.user.id = :userId and ci.product.id = :productId")
	int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
