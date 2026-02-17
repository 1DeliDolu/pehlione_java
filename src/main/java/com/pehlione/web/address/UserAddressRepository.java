package com.pehlione.web.address;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

	List<UserAddress> findByUserIdOrderByIsDefaultDescUpdatedAtDesc(Long userId);

	Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

	@Modifying
	@Query("update UserAddress a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
	int clearDefault(@Param("userId") Long userId);
}
