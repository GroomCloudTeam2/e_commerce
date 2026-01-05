package com.groom.e_commerce.user.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.groom.e_commerce.user.domain.entity.AddressEntity;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {

	List<AddressEntity> findByUserUserId(UUID userId);

	Optional<AddressEntity> findByAddressIdAndUserUserId(UUID addressId, UUID userId);

    Optional<AddressEntity> findByUserUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
	@Query("UPDATE AddressEntity a SET a.isDefault = false WHERE a.user.userId = :userId AND a.isDefault = true")
	void clearDefaultAddress(@Param("userId") UUID userId);
}
