package com.groom.e_commerce.claim.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groom.e_commerce.claim.domain.entity.Claim;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
}
