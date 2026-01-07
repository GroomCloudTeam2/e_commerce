package com.groom.e_commerce.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.global.util.SecurityUtil;
import com.groom.e_commerce.user.application.service.UserServiceV1;
import com.groom.e_commerce.user.domain.entity.address.AddressEntity;
import com.groom.e_commerce.user.domain.entity.owner.OwnerEntity;
import com.groom.e_commerce.user.domain.entity.owner.OwnerStatus;
import com.groom.e_commerce.user.domain.entity.user.PeriodType;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.domain.repository.AddressRepository;
import com.groom.e_commerce.user.domain.repository.OwnerRepository;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.request.user.ReqUpdateUserDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.owner.ResSalesStatDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResUserDtoV1;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceV1 테스트")
class UserServiceV1Test {

	@InjectMocks
	private UserServiceV1 userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private OwnerRepository ownerRepository;

	private UserEntity normalUser;
	private UserEntity ownerUser;
	private AddressEntity defaultAddress;
	private OwnerEntity ownerEntity;
	private UUID userId;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();

		// 일반 유저
		normalUser = UserEntity.builder()
			.userId(userId)
			.email("user@test.com")
			.password("encodedPassword")
			.nickname("테스트유저")
			.phoneNumber("010-1234-5678")
			.role(UserRole.USER)
			.status(UserStatus.ACTIVE)
			.build();

		// Owner 유저
		ownerUser = UserEntity.builder()
			.userId(userId)
			.email("owner@test.com")
			.password("encodedPassword")
			.nickname("테스트오너")
			.phoneNumber("010-1111-2222")
			.role(UserRole.OWNER)
			.status(UserStatus.ACTIVE)
			.build();

		// 기본 배송지
		defaultAddress = AddressEntity.builder()
			.addressId(UUID.randomUUID())
			.user(normalUser)
			.zipCode("12345")
			.address("서울시 강남구")
			.detailAddress("101호")
			.recipient("수령인")
			.recipientPhone("010-9999-8888")
			.isDefault(true)
			.build();

		// Owner 엔티티
		ownerEntity = OwnerEntity.builder()
			.ownerId(UUID.randomUUID())
			.user(ownerUser)
			.storeName("테스트가게")
			.ownerStatus(OwnerStatus.APPROVED)
			.build();
	}

	@Nested
	@DisplayName("내 정보 조회 테스트")
	class GetMeTest {

		@Test
		@DisplayName("일반 유저 내 정보 조회 성공")
		void getMe_normalUser_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));
				given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
					.willReturn(Optional.of(defaultAddress));

				// when
				ResUserDtoV1 result = userService.getMe();

				// then
				assertThat(result).isNotNull();
				assertThat(result.getEmail()).isEqualTo("user@test.com");
				assertThat(result.getNickname()).isEqualTo("테스트유저");
				assertThat(result.getDefaultAddress()).isNotNull();
				assertThat(result.getOwnerInfo()).isNull();
			}
		}

		@Test
		@DisplayName("Owner 유저 내 정보 조회 성공 (ownerInfo 포함)")
		void getMe_ownerUser_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(ownerUser));
				given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
					.willReturn(Optional.empty());
				given(ownerRepository.findByUserUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(ownerEntity));

				// when
				ResUserDtoV1 result = userService.getMe();

				// then
				assertThat(result).isNotNull();
				assertThat(result.getEmail()).isEqualTo("owner@test.com");
				assertThat(result.getRole()).isEqualTo(UserRole.OWNER);
				assertThat(result.getOwnerInfo()).isNotNull();
			}
		}

		@Test
		@DisplayName("존재하지 않는 유저 조회 시 실패")
		void getMe_userNotFound_fail() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.empty());

				// when & then
				assertThatThrownBy(() -> userService.getMe())
					.isInstanceOf(CustomException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
			}
		}
	}

	@Nested
	@DisplayName("내 정보 수정 테스트")
	class UpdateMeTest {

		@Test
		@DisplayName("닉네임 수정 성공")
		void updateMe_nickname_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
				request.setNickname("새닉네임");

				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));
				given(userRepository.findByNickname("새닉네임"))
					.willReturn(Optional.empty());

				// when & then
				assertThatCode(() -> userService.updateMe(request))
					.doesNotThrowAnyException();

				assertThat(normalUser.getNickname()).isEqualTo("새닉네임");
			}
		}

		@Test
		@DisplayName("전화번호 수정 성공")
		void updateMe_phoneNumber_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
				request.setPhoneNumber("010-9999-0000");

				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));

				// when & then
				assertThatCode(() -> userService.updateMe(request))
					.doesNotThrowAnyException();

				assertThat(normalUser.getPhoneNumber()).isEqualTo("010-9999-0000");
			}
		}

		@Test
		@DisplayName("비밀번호 수정 성공")
		void updateMe_password_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
				request.setPassword("newPassword123");

				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));
				given(passwordEncoder.encode("newPassword123"))
					.willReturn("newEncodedPassword");

				// when & then
				assertThatCode(() -> userService.updateMe(request))
					.doesNotThrowAnyException();

				assertThat(normalUser.getPassword()).isEqualTo("newEncodedPassword");
			}
		}

		@Test
		@DisplayName("중복된 닉네임으로 수정 시 실패")
		void updateMe_duplicateNickname_fail() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
				request.setNickname("중복닉네임");

				UserEntity anotherUser = UserEntity.builder()
					.userId(UUID.randomUUID())
					.nickname("중복닉네임")
					.build();

				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));
				given(userRepository.findByNickname("중복닉네임"))
					.willReturn(Optional.of(anotherUser));

				// when & then
				assertThatThrownBy(() -> userService.updateMe(request))
					.isInstanceOf(CustomException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
			}
		}
	}

	@Nested
	@DisplayName("회원 탈퇴 테스트")
	class DeleteMeTest {

		@Test
		@DisplayName("회원 탈퇴 성공")
		void deleteMe_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));

				// when & then
				assertThatCode(() -> userService.deleteMe())
					.doesNotThrowAnyException();

				assertThat(normalUser.isWithdrawn()).isTrue();
			}
		}

		@Test
		@DisplayName("이미 탈퇴한 유저 탈퇴 시도 시 실패")
		void deleteMe_alreadyWithdrawn_fail() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				UserEntity withdrawnUser = UserEntity.builder()
					.userId(userId)
					.status(UserStatus.WITHDRAWN)
					.build();

				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(withdrawnUser));

				// when & then
				assertThatThrownBy(() -> userService.deleteMe())
					.isInstanceOf(CustomException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAWN);
			}
		}
	}

	@Nested
	@DisplayName("매출 통계 조회 테스트")
	class GetSalesStatsTest {

		@Test
		@DisplayName("Owner 매출 통계 조회 성공")
		void getSalesStats_success() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				LocalDate targetDate = LocalDate.of(2026, 1, 6);

				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(ownerUser));

				// when
				List<ResSalesStatDtoV1> result = userService.getSalesStats(PeriodType.DAILY, targetDate);

				// then
				assertThat(result).isNotNull();
				assertThat(result).hasSize(1);
				assertThat(result.get(0).getDate()).isEqualTo(targetDate);
			}
		}

		@Test
		@DisplayName("Owner가 아닌 유저 매출 통계 조회 시 실패")
		void getSalesStats_notOwner_fail() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(normalUser));

				// when & then
				assertThatThrownBy(() -> userService.getSalesStats(PeriodType.DAILY, LocalDate.now()))
					.isInstanceOf(CustomException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
			}
		}

		@Test
		@DisplayName("날짜 없이 매출 통계 조회 시 오늘 날짜로 조회")
		void getSalesStats_noDate_useToday() {
			try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
				// given
				securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
				given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
					.willReturn(Optional.of(ownerUser));

				// when
				List<ResSalesStatDtoV1> result = userService.getSalesStats(PeriodType.DAILY, null);

				// then
				assertThat(result).isNotNull();
				assertThat(result.get(0).getDate()).isEqualTo(LocalDate.now());
			}
		}
	}

	@Nested
	@DisplayName("유저 조회 테스트")
	class FindUserByIdTest {

		@Test
		@DisplayName("유저 ID로 조회 성공")
		void findUserById_success() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
				.willReturn(Optional.of(normalUser));

			// when
			UserEntity result = userService.findUserById(userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getUserId()).isEqualTo(userId);
		}

		@Test
		@DisplayName("존재하지 않는 유저 조회 시 실패")
		void findUserById_notFound_fail() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> userService.findUserById(userId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}
	}
}
