package com.groom.e_commerce.user.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.domain.entity.seller.SellerEntity;
import com.groom.e_commerce.user.domain.entity.seller.SellerStatus;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.domain.repository.SellerRepository;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.request.admin.ReqCreateManagerDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.admin.ResSellerApprovalListDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.seller.ResSellerApprovalDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResUserDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResUserListDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceV1 {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SellerRepository sellerRepository;

	// ==================== Manager 기능 ====================

	/**
	 * 회원 목록 조회 (Manager)
	 */
	public ResUserListDtoV1 getUserList(Pageable pageable) {
		Page<ResUserDtoV1> users = userRepository.findByDeletedAtIsNull(pageable)
			.map(ResUserDtoV1::from);
		return ResUserListDtoV1.from(users);
	}

	/**
	 * 회원 제재 (Manager) - 정책 위반 계정 밴
	 */
	@Transactional
	public void banUser(UUID userId) {
		UserEntity user = findUserById(userId);

		if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.MASTER) {
			throw new CustomException(ErrorCode.FORBIDDEN, "관리자 계정은 제재할 수 없습니다.");
		}

		if (user.isBanned()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "이미 제재된 사용자입니다.");
		}

		user.ban();
		log.info("User banned: {}", userId);
	}

	/**
	 * 회원 제재 해제 (Manager)
	 */
	@Transactional
	public void unbanUser(UUID userId) {
		UserEntity user = findUserById(userId);

		if (!user.isBanned()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "제재된 사용자가 아닙니다.");
		}

		user.activate();
		log.info("User unbanned: {}", userId);
	}

	// ==================== Master 기능 ====================

	/**
	 * Manager 계정 생성 (Master only)
	 */
	@Transactional
	public ResUserDtoV1 createManager(ReqCreateManagerDtoV1 request) {
		if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
			throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
		}

		if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
			throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
		}

		UserEntity manager = UserEntity.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.nickname(request.getNickname())
			.phoneNumber(request.getPhoneNumber())
			.role(UserRole.MANAGER)
			.status(UserStatus.ACTIVE)
			.build();

		userRepository.save(manager);
		log.info("Manager created: {}", request.getEmail());

		return ResUserDtoV1.from(manager);
	}

	/**
	 * Manager 계정 삭제 (Master only)
	 */
	@Transactional
	public void deleteManager(UUID managerId) {
		UserEntity manager = findUserById(managerId);

		if (manager.getRole() != UserRole.MANAGER) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "Manager 계정만 삭제할 수 있습니다.");
		}

		manager.withdraw();
		log.info("Manager deleted: {}", managerId);
	}

	/**
	 * Manager 목록 조회 (Master only)
	 */
	public ResUserListDtoV1 getManagerList(Pageable pageable) {
		Page<ResUserDtoV1> managers = userRepository.findByRoleAndDeletedAtIsNull(UserRole.MANAGER, pageable)
			.map(ResUserDtoV1::from);
		return ResUserListDtoV1.from(managers);
	}

	private UserEntity findUserById(UUID userId) {
		return userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 승인 대기 중인 Seller 목록 조회 (Manager)
	 */
	public ResSellerApprovalListDtoV1 getPendingSellerList(Pageable pageable) {
		Page<ResSellerApprovalDtoV1> pendingSellers = sellerRepository
			.findBySellerStatusAndDeletedAtIsNull(SellerStatus.PENDING, pageable)
			.map(ResSellerApprovalDtoV1::from);
		return ResSellerApprovalListDtoV1.from(pendingSellers);
	}

	/**
	 * 특정 상태의 Seller 목록 조회 (Manager)
	 */
	public ResSellerApprovalListDtoV1 getSellerListByStatus(SellerStatus status, Pageable pageable) {
		Page<ResSellerApprovalDtoV1> sellers = sellerRepository
			.findBySellerStatusAndDeletedAtIsNull(status, pageable)
			.map(ResSellerApprovalDtoV1::from);
		return ResSellerApprovalListDtoV1.from(sellers);
	}

	/**
	 * Seller 승인 요청 상세 조회 (Manager)
	 */
	public ResSellerApprovalDtoV1 getSellerApprovalDetail(UUID sellerId) {
		SellerEntity seller = findSellerById(sellerId);
		return ResSellerApprovalDtoV1.from(seller);
	}

	/**
	 * Seller 승인 (Manager)
	 */
	@Transactional
	public ResSellerApprovalDtoV1 approveSeller(UUID sellerId) {
		SellerEntity seller = findSellerById(sellerId);

		if (!seller.isPending()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR,
				"승인 대기 상태인 요청만 승인할 수 있습니다. 현재 상태: " + seller.getSellerStatus());
		}

		seller.approve();
		log.info("Seller approved: sellerId={}, storeName={}", sellerId, seller.getStoreName());

		return ResSellerApprovalDtoV1.from(seller);
	}

	/**
	 * Seller 승인 거절 (Manager)
	 */
	@Transactional
	public ResSellerApprovalDtoV1 rejectSeller(UUID sellerId, String rejectedReason) {
		SellerEntity seller = findSellerById(sellerId);

		if (!seller.isPending()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR,
				"승인 대기 상태인 요청만 거절할 수 있습니다. 현재 상태: " + seller.getSellerStatus());
		}

		seller.reject(rejectedReason);
		log.info("Seller rejected: sellerId={}, storeName={}, reason={}",
			sellerId, seller.getStoreName(), rejectedReason);

		return ResSellerApprovalDtoV1.from(seller);
	}

	private SellerEntity findSellerById(UUID sellerId) {
		return sellerRepository.findBySellerIdAndDeletedAtIsNull(sellerId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 판매자를 찾을 수 없습니다."));
	}
}
