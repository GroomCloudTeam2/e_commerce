package com.groom.e_commerce.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groom.e_commerce.global.infrastructure.config.security.JwtUtil;
import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.application.service.AuthServiceV1;
import com.groom.e_commerce.user.domain.entity.owner.OwnerEntity;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.domain.repository.OwnerRepository;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.request.user.ReqLoginDtoV1;
import com.groom.e_commerce.user.presentation.dto.request.user.ReqSignupDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResTokenDtoV1;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceV1 테스트")
class AuthServiceV1Test {

	@InjectMocks
	private AuthServiceV1 authService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	private ReqSignupDtoV1 userSignupRequest;
	private ReqSignupDtoV1 ownerSignupRequest;
	private ReqLoginDtoV1 loginRequest;
	private UserEntity savedUser;

	@BeforeEach
	void setUp() {
		// USER 회원가입 요청
		userSignupRequest = new ReqSignupDtoV1();
		userSignupRequest.setEmail("user@test.com");
		userSignupRequest.setPassword("password123");
		userSignupRequest.setNickname("테스트유저");
		userSignupRequest.setPhoneNumber("010-1234-5678");
		userSignupRequest.setRole(UserRole.USER);

		// OWNER 회원가입 요청
		ownerSignupRequest = new ReqSignupDtoV1();
		ownerSignupRequest.setEmail("owner@test.com");
		ownerSignupRequest.setPassword("password123");
		ownerSignupRequest.setNickname("테스트오너");
		ownerSignupRequest.setPhoneNumber("010-1111-2222");
		ownerSignupRequest.setRole(UserRole.OWNER);
		ownerSignupRequest.setStore("테스트가게");
		ownerSignupRequest.setZipCode("12345");
		ownerSignupRequest.setAddress("서울시 강남구");
		ownerSignupRequest.setDetailAddress("101호");

		// 로그인 요청
		loginRequest = new ReqLoginDtoV1();
		loginRequest.setEmail("user@test.com");
		loginRequest.setPassword("password123");

		// 저장된 유저
		savedUser = UserEntity.builder()
			.userId(UUID.randomUUID())
			.email("user@test.com")
			.password("encodedPassword")
			.nickname("테스트유저")
			.phoneNumber("010-1234-5678")
			.role(UserRole.USER)
			.status(UserStatus.ACTIVE)
			.build();
	}

	@Nested
	@DisplayName("회원가입 테스트")
	class SignupTest {

		@Test
		@DisplayName("USER 회원가입 성공")
		void signup_user_success() {
			// given
			given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
			given(userRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(false);
			given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
			given(userRepository.save(any(UserEntity.class))).willReturn(savedUser);

			// when & then
			assertThatCode(() -> authService.signup(userSignupRequest))
				.doesNotThrowAnyException();

			then(userRepository).should().save(any(UserEntity.class));
		}

		@Test
		@DisplayName("OWNER 회원가입 성공")
		void signup_owner_success() {
			// given
			given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
			given(userRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(false);
			given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
			given(userRepository.save(any(UserEntity.class))).willReturn(savedUser);
			given(ownerRepository.save(any(OwnerEntity.class))).willReturn(mock(OwnerEntity.class));

			// when & then
			assertThatCode(() -> authService.signup(ownerSignupRequest))
				.doesNotThrowAnyException();

			then(userRepository).should().save(any(UserEntity.class));
			then(ownerRepository).should().save(any(OwnerEntity.class));
		}

		@Test
		@DisplayName("MANAGER 역할로 회원가입 시도 시 실패")
		void signup_manager_fail() {
			// given
			userSignupRequest.setRole(UserRole.MANAGER);

			// when & then
			assertThatThrownBy(() -> authService.signup(userSignupRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}

		@Test
		@DisplayName("이메일 중복 시 회원가입 실패")
		void signup_duplicateEmail_fail() {
			// given
			UserEntity existingUser = UserEntity.builder()
				.email("user@test.com")
				.status(UserStatus.ACTIVE)
				.build();
			given(userRepository.findByEmail(anyString())).willReturn(Optional.of(existingUser));

			// when & then
			assertThatThrownBy(() -> authService.signup(userSignupRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATED);
		}

		@Test
		@DisplayName("닉네임 중복 시 회원가입 실패")
		void signup_duplicateNickname_fail() {
			// given
			given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
			given(userRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> authService.signup(userSignupRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
		}

		@Test
		@DisplayName("OWNER 회원가입 시 가게 이름 없으면 실패")
		void signup_owner_noStoreName_fail() {
			// given
			ownerSignupRequest.setStore(null);
			given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
			given(userRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.signup(ownerSignupRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}

		@Test
		@DisplayName("탈퇴한 유저 재가입 시 복구 성공")
		void signup_reactivate_withdrawnUser_success() {
			// given
			UserEntity withdrawnUser = UserEntity.builder()
				.email("user@test.com")
				.status(UserStatus.WITHDRAWN)
				.build();
			given(userRepository.findByEmail(anyString())).willReturn(Optional.of(withdrawnUser));
			given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

			// when & then
			assertThatCode(() -> authService.signup(userSignupRequest))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("로그인 테스트")
	class LoginTest {

		@Test
		@DisplayName("로그인 성공")
		void login_success() {
			// given
			given(userRepository.findByEmailAndDeletedAtIsNull(anyString()))
				.willReturn(Optional.of(savedUser));
			given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
			given(jwtUtil.generateAccessToken(any(), anyString(), anyString()))
				.willReturn("accessToken");
			given(jwtUtil.generateRefreshToken(any(), anyString(), anyString()))
				.willReturn("refreshToken");

			// when
			ResTokenDtoV1 result = authService.login(loginRequest);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getAccessToken()).isEqualTo("accessToken");
			assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
		}

		@Test
		@DisplayName("존재하지 않는 이메일로 로그인 시 실패")
		void login_userNotFound_fail() {
			// given
			given(userRepository.findByEmailAndDeletedAtIsNull(anyString()))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}

		@Test
		@DisplayName("비밀번호 불일치 시 로그인 실패")
		void login_invalidPassword_fail() {
			// given
			given(userRepository.findByEmailAndDeletedAtIsNull(anyString()))
				.willReturn(Optional.of(savedUser));
			given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
		}

		@Test
		@DisplayName("탈퇴한 유저 로그인 시 실패")
		void login_withdrawnUser_fail() {
			// given
			UserEntity withdrawnUser = UserEntity.builder()
				.email("user@test.com")
				.password("encodedPassword")
				.status(UserStatus.WITHDRAWN)
				.build();
			given(userRepository.findByEmailAndDeletedAtIsNull(anyString()))
				.willReturn(Optional.of(withdrawnUser));
			given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAWN);
		}
	}
}
