package com.groom.e_commerce.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.application.service.AdminServiceV1;
import com.groom.e_commerce.user.domain.entity.owner.OwnerEntity;
import com.groom.e_commerce.user.domain.entity.owner.OwnerStatus;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.domain.repository.OwnerRepository;
import com.groom.e_commerce.user.domain.repository.UserRepository;
import com.groom.e_commerce.user.presentation.dto.request.admin.ReqCreateManagerDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.admin.ResOwnerApprovalListDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResUserDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.user.ResUserListDtoV1;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceV1 테스트")
class AdminServiceV1Test {

	@InjectMocks
	private AdminServiceV1 adminService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserEntity normalUser;
	private UserEntity managerUser;
	private UserEntity bannedUser;
	private OwnerEntity pendingOwner;
	private Pageable pageable;

	@BeforeEach
	void setUp() {
		pageable = PageRequest.of(0, 20);

		// 일반 유저
		normalUser = UserEntity.builder()
			.userId(UUID.randomUUID())
			.email("user@test.com")
			.password("encodedPassword")
			.nickname("일반유저")
			.phoneNumber("010-1234-5678")
			.role(UserRole.USER)
			.status(UserStatus.ACTIVE)
			.build();

		// 매니저 유저
		managerUser = UserEntity.builder()
			.userId(UUID.randomUUID())
			.email("manager@test.com")
			.password("encodedPassword")
			.nickname("매니저")
			.phoneNumber("010-1111-2222")
			.role(UserRole.MANAGER)
			.status(UserStatus.ACTIVE)
			.build();

		// 제재된 유저
		bannedUser = UserEntity.builder()
			.userId(UUID.randomUUID())
			.email("banned@test.com")
			.password("encodedPassword")
			.nickname("제재유저")
			.phoneNumber("010-3333-4444")
			.role(UserRole.USER)
			.status(UserStatus.BANNED)
			.build();

		// 승인 대기 중인 Owner
		pendingOwner = OwnerEntity.builder()
			.ownerId(UUID.randomUUID())
			.user(normalUser)
			.storeName("테스트가게")
			.ownerStatus(OwnerStatus.PENDING)
			.build();
	}

	@Nested
	@DisplayName("회원 목록 조회 테스트")
	class GetUserListTest {

		@Test
		@DisplayName("회원 목록 조회 성공")
		void getUserList_success() {
			// given
			Page<UserEntity> userPage = new PageImpl<>(List.of(normalUser));
			given(userRepository.findByDeletedAtIsNull(pageable)).willReturn(userPage);

			// when
			ResUserListDtoV1 result = adminService.getUserList(pageable);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getUsers()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("회원 제재 테스트")
	class BanUserTest {

		@Test
		@DisplayName("회원 제재 성공")
		void banUser_success() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(normalUser));

			// when & then
			assertThatCode(() -> adminService.banUser(normalUser.getUserId()))
				.doesNotThrowAnyException();

			assertThat(normalUser.isBanned()).isTrue();
		}

		@Test
		@DisplayName("관리자 계정 제재 시도 시 실패")
		void banUser_manager_fail() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(managerUser));

			// when & then
			assertThatThrownBy(() -> adminService.banUser(managerUser.getUserId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}

		@Test
		@DisplayName("이미 제재된 유저 제재 시도 시 실패")
		void banUser_alreadyBanned_fail() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(bannedUser));

			// when & then
			assertThatThrownBy(() -> adminService.banUser(bannedUser.getUserId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}

		@Test
		@DisplayName("존재하지 않는 유저 제재 시도 시 실패")
		void banUser_userNotFound_fail() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> adminService.banUser(UUID.randomUUID()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("회원 제재 해제 테스트")
	class UnbanUserTest {

		@Test
		@DisplayName("회원 제재 해제 성공")
		void unbanUser_success() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(bannedUser));

			// when & then
			assertThatCode(() -> adminService.unbanUser(bannedUser.getUserId()))
				.doesNotThrowAnyException();

			assertThat(bannedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		}

		@Test
		@DisplayName("제재되지 않은 유저 해제 시도 시 실패")
		void unbanUser_notBanned_fail() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(normalUser));

			// when & then
			assertThatThrownBy(() -> adminService.unbanUser(normalUser.getUserId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}
	}

	@Nested
	@DisplayName("Manager 계정 생성 테스트")
	class CreateManagerTest {

		@Test
		@DisplayName("Manager 계정 생성 성공")
		void createManager_success() {
			// given
			ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
			request.setEmail("newmanager@test.com");
			request.setPassword("password123");
			request.setNickname("새매니저");
			request.setPhoneNumber("010-5555-6666");

			given(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
			given(userRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(false);
			given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
			given(userRepository.save(any(UserEntity.class))).willReturn(managerUser);

			// when
			ResUserDtoV1 result = adminService.createManager(request);

			// then
			assertThat(result).isNotNull();
			then(userRepository).should().save(any(UserEntity.class));
		}

		@Test
		@DisplayName("이메일 중복 시 Manager 생성 실패")
		void createManager_duplicateEmail_fail() {
			// given
			ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
			request.setEmail("existing@test.com");

			given(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> adminService.createManager(request))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATED);
		}

		@Test
		@DisplayName("닉네임 중복 시 Manager 생성 실패")
		void createManager_duplicateNickname_fail() {
			// given
			ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
			request.setEmail("newmanager@test.com");
			request.setNickname("기존닉네임");

			given(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
			given(userRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> adminService.createManager(request))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
		}
	}

	@Nested
	@DisplayName("Manager 계정 삭제 테스트")
	class DeleteManagerTest {

		@Test
		@DisplayName("Manager 계정 삭제 성공")
		void deleteManager_success() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(managerUser));

			// when & then
			assertThatCode(() -> adminService.deleteManager(managerUser.getUserId()))
				.doesNotThrowAnyException();

			assertThat(managerUser.isWithdrawn()).isTrue();
		}

		@Test
		@DisplayName("Manager가 아닌 계정 삭제 시도 시 실패")
		void deleteManager_notManager_fail() {
			// given
			given(userRepository.findByUserIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(normalUser));

			// when & then
			assertThatThrownBy(() -> adminService.deleteManager(normalUser.getUserId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}
	}

	@Nested
	@DisplayName("Owner 승인 테스트")
	class ApproveOwnerTest {

		@Test
		@DisplayName("Owner 승인 성공")
		void approveOwner_success() {
			// given
			given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(pendingOwner));

			// when
			ResOwnerApprovalDtoV1 result = adminService.approveOwner(pendingOwner.getOwnerId());

			// then
			assertThat(result).isNotNull();
			assertThat(pendingOwner.isApproved()).isTrue();
		}

		@Test
		@DisplayName("이미 승인된 Owner 승인 시도 시 실패")
		void approveOwner_notPending_fail() {
			// given
			OwnerEntity approvedOwner = OwnerEntity.builder()
				.ownerId(UUID.randomUUID())
				.user(normalUser)
				.storeName("승인된가게")
				.ownerStatus(OwnerStatus.APPROVED)
				.build();

			given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(approvedOwner));

			// when & then
			assertThatThrownBy(() -> adminService.approveOwner(approvedOwner.getOwnerId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}

		@Test
		@DisplayName("존재하지 않는 Owner 승인 시도 시 실패")
		void approveOwner_notFound_fail() {
			// given
			given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> adminService.approveOwner(UUID.randomUUID()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("Owner 승인 거절 테스트")
	class RejectOwnerTest {

		@Test
		@DisplayName("Owner 승인 거절 성공")
		void rejectOwner_success() {
			// given
			String rejectedReason = "서류 미비";
			given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(pendingOwner));

			// when
			ResOwnerApprovalDtoV1 result = adminService.rejectOwner(pendingOwner.getOwnerId(), rejectedReason);

			// then
			assertThat(result).isNotNull();
			assertThat(pendingOwner.isRejected()).isTrue();
			assertThat(pendingOwner.getRejectedReason()).isEqualTo(rejectedReason);
		}

		@Test
		@DisplayName("승인 대기 상태가 아닌 Owner 거절 시도 시 실패")
		void rejectOwner_notPending_fail() {
			// given
			OwnerEntity approvedOwner = OwnerEntity.builder()
				.ownerId(UUID.randomUUID())
				.user(normalUser)
				.storeName("승인된가게")
				.ownerStatus(OwnerStatus.APPROVED)
				.build();

			given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(any(UUID.class)))
				.willReturn(Optional.of(approvedOwner));

			// when & then
			assertThatThrownBy(() -> adminService.rejectOwner(approvedOwner.getOwnerId(), "거절 사유"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
		}
	}

	@Nested
	@DisplayName("Owner 목록 조회 테스트")
	class GetOwnerListTest {

		@Test
		@DisplayName("승인 대기 Owner 목록 조회 성공")
		void getPendingOwnerList_success() {
			// given
			Page<OwnerEntity> ownerPage = new PageImpl<>(List.of(pendingOwner));
			given(ownerRepository.findByOwnerStatusAndDeletedAtIsNull(eq(OwnerStatus.PENDING), any(Pageable.class)))
				.willReturn(ownerPage);

			// when
			ResOwnerApprovalListDtoV1 result = adminService.getPendingOwnerList(pageable);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getContent()).hasSize(1);
		}

		@Test
		@DisplayName("상태별 Owner 목록 조회 성공")
		void getOwnerListByStatus_success() {
			// given
			Page<OwnerEntity> ownerPage = new PageImpl<>(List.of(pendingOwner));
			given(ownerRepository.findByOwnerStatusAndDeletedAtIsNull(eq(OwnerStatus.PENDING), any(Pageable.class)))
				.willReturn(ownerPage);

			// when
			ResOwnerApprovalListDtoV1 result = adminService.getOwnerListByStatus(OwnerStatus.PENDING, pageable);

			// then
			assertThat(result).isNotNull();
		}
	}
}
