package com.groom.e_commerce.user.application.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.groom.e_commerce.global.infrastructure.config.security.JwtUtil;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.domain.entity.seller.SellerEntity;
import com.groom.e_commerce.user.domain.entity.seller.SellerStatus;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.domain.repository.SellerRepository;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.request.user.ReqLoginDtoV1;
import com.groom.e_commerce.user.presentation.dto.request.user.ReqSignupDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResTokenDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceV1 {

	private final UserRepository userRepository;
	private final SellerRepository sellerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	@Transactional
	public void signup(ReqSignupDtoV1 request) {
		// USER, OWNER만 회원가입 가능 (MANAGER는 MASTER가 생성)
		if (request.getRole() != UserRole.USER && request.getRole() != UserRole.SELLER) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "USER 또는 SELLER만 회원가입할 수 있습니다.");
		}

		// 탈퇴 유저 복구 처리
		Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
		if (existingUser.isPresent()) {
			UserEntity user = existingUser.get();
			if (user.isWithdrawn()) {
				user.reactivate(
					passwordEncoder.encode(request.getPassword()),
					request.getNickname(),
					request.getPhoneNumber()
				);
				log.info("User reactivated: {}", request.getEmail());
				return;
			}
			throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
		}

		if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
			throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
		}

		// SELLER인 경우 검증 + 저장을 한 블록에서 처리
		if (request.isOwner()) {
			validateOwnerFields(request);
			UserEntity user = createAndSaveUser(request);
			createAndSaveSeller(user, request);
			log.info("SELLER signed up with store: {}", request.getStore());
		} else {
			createAndSaveUser(request);
			log.info("User signed up: {}", request.getEmail());
		}
	}

	private UserEntity createAndSaveUser(ReqSignupDtoV1 request) {
		UserEntity user = UserEntity.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.nickname(request.getNickname())
			.phoneNumber(request.getPhoneNumber())
			.role(request.getRole())
			.status(UserStatus.ACTIVE)
			.build();
		return userRepository.save(user);
	}

	private void createAndSaveSeller(UserEntity user, ReqSignupDtoV1 request) {
		SellerEntity seller = SellerEntity.builder()
			.user(user)
			.storeName(request.getStore())
			.zipCode(request.getZipCode())
			.address(request.getAddress())
			.detailAddress(request.getDetailAddress())
			.bank(request.getBank())
			.account(request.getAccount())
			.approvalRequest(request.getApprovalRequest())
			.sellerStatus(SellerStatus.PENDING)
			.build();

		sellerRepository.save(seller);
	}

	public ResTokenDtoV1 login(ReqLoginDtoV1 request) {
		UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		if (user.isWithdrawn()) {
			throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
		}

		String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name());
		String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getEmail(), user.getRole().name());

		log.info("User logged in: {} (role: {})", request.getEmail(), user.getRole());
		return ResTokenDtoV1.of(accessToken, refreshToken);
	}

	public void logout() {
		log.info("User logged out");
	}

	private void validateDuplicateEmail(String email) {
		if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
			throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
		}
	}

	private void validateDuplicateNickname(String nickname) {
		if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
			throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
		}
	}

	private void validateOwnerFields(ReqSignupDtoV1 request) {
		if (!StringUtils.hasText(request.getStore())) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "가게 이름은 필수입니다.");
		}
	}
}
