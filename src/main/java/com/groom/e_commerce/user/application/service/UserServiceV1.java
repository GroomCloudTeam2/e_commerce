package com.groom.e_commerce.user.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.domain.entity.PeriodType;
import com.groom.e_commerce.user.domain.entity.UserEntity;
import com.groom.e_commerce.user.domain.entity.UserRole;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.request.ReqUpdateUserDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResSalesStatDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResUserDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceV1 {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public ResUserDtoV1 getMe(UUID userId) {
		return ResUserDtoV1.from(findUserById(userId));
	}

	@Transactional
	public void updateMe(UUID userId, ReqUpdateUserDtoV1 request) {
		UserEntity user = findUserById(userId);

		if (StringUtils.hasText(request.getNickname())) {
			validateNicknameNotTaken(request.getNickname(), userId);
			user.updateNickname(request.getNickname());
		}

		if (StringUtils.hasText(request.getPhoneNumber())) {
			user.updatePhoneNumber(request.getPhoneNumber());
		}

		if (StringUtils.hasText(request.getPassword())) {
			user.updatePassword(passwordEncoder.encode(request.getPassword()));
		}

		log.info("User updated: {}", userId);
	}

	@Transactional
	public void deleteMe(UUID userId) {
		UserEntity user = findUserById(userId);

		if (user.isWithdrawn()) {
			throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
		}

		user.withdraw();
		log.info("User withdrew: {}", userId);
	}

	public List<ResSalesStatDtoV1> getSalesStats(UUID userId, PeriodType periodType, LocalDate date) {
		UserEntity user = findUserById(userId);

		if (user.getRole() != UserRole.OWNER) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}

		log.info("Sales stats requested: userId={}, periodType={}, date={}", userId, periodType, date);

		LocalDate targetDate = date != null ? date : LocalDate.now();
		return List.of(ResSalesStatDtoV1.of(targetDate, BigDecimal.ZERO));
	}

	public UserEntity findUserById(UUID userId) {
		return userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateNicknameNotTaken(String nickname, UUID currentUserId) {
		userRepository.findByNickname(nickname)
			.filter(u -> !u.getUserId().equals(currentUserId))
			.ifPresent(u -> {
				throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
			});
	}
}
