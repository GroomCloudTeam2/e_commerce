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

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.application.service.AddressServiceV1;
import com.groom.e_commerce.user.application.service.UserServiceV1;
import com.groom.e_commerce.user.domain.entity.address.AddressEntity;
import com.groom.e_commerce.user.domain.entity.user.UserEntity;
import com.groom.e_commerce.user.domain.entity.user.UserRole;
import com.groom.e_commerce.user.domain.entity.user.UserStatus;
import com.groom.e_commerce.user.domain.repository.AddressRepository;
import com.groom.e_commerce.user.presentation.dto.request.address.ReqAddressDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.address.ResAddressDtoV1;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressServiceV1 테스트")
class AddressServiceV1Test {

	@InjectMocks
	private AddressServiceV1 addressService;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private UserServiceV1 userService;

	private UserEntity user;
	private AddressEntity address;
	private AddressEntity defaultAddress;
	private UUID userId;
	private UUID addressId;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		addressId = UUID.randomUUID();

		user = UserEntity.builder()
			.userId(userId)
			.email("user@test.com")
			.password("encodedPassword")
			.nickname("테스트유저")
			.phoneNumber("010-1234-5678")
			.role(UserRole.USER)
			.status(UserStatus.ACTIVE)
			.build();

		address = AddressEntity.builder()
			.addressId(addressId)
			.user(user)
			.zipCode("12345")
			.address("서울시 강남구")
			.detailAddress("101호")
			.recipient("수령인")
			.recipientPhone("010-9999-8888")
			.isDefault(false)
			.build();

		defaultAddress = AddressEntity.builder()
			.addressId(UUID.randomUUID())
			.user(user)
			.zipCode("67890")
			.address("서울시 서초구")
			.detailAddress("202호")
			.recipient("기본수령인")
			.recipientPhone("010-1111-2222")
			.isDefault(true)
			.build();
	}

	@Nested
	@DisplayName("배송지 목록 조회 테스트")
	class GetAddressesTest {

		@Test
		@DisplayName("배송지 목록 조회 성공")
		void getAddresses_success() {
			// given
			given(addressRepository.findByUserUserId(userId))
				.willReturn(List.of(address, defaultAddress));

			// when
			List<ResAddressDtoV1> result = addressService.getAddresses(userId);

			// then
			assertThat(result).hasSize(2);
		}

		@Test
		@DisplayName("배송지 없을 때 빈 리스트 반환")
		void getAddresses_empty() {
			// given
			given(addressRepository.findByUserUserId(userId))
				.willReturn(List.of());

			// when
			List<ResAddressDtoV1> result = addressService.getAddresses(userId);

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("배송지 등록 테스트")
	class CreateAddressTest {

		@Test
		@DisplayName("배송지 등록 성공")
		void createAddress_success() {
			// given
			ReqAddressDtoV1 request = new ReqAddressDtoV1();
			request.setZipCode("11111");
			request.setAddress("서울시 송파구");
			request.setDetailAddress("303호");
			request.setRecipient("새수령인");
			request.setRecipientPhone("010-3333-4444");
			request.setIsDefault(false);

			given(userService.findUserById(userId)).willReturn(user);
			given(addressRepository.save(any(AddressEntity.class))).willReturn(address);

			// when & then
			assertThatCode(() -> addressService.createAddress(userId, request))
				.doesNotThrowAnyException();

			then(addressRepository).should().save(any(AddressEntity.class));
		}

		@Test
		@DisplayName("기본 배송지로 등록 시 기존 기본 배송지 해제")
		void createAddress_asDefault_clearPreviousDefault() {
			// given
			ReqAddressDtoV1 request = new ReqAddressDtoV1();
			request.setZipCode("11111");
			request.setAddress("서울시 송파구");
			request.setDetailAddress("303호");
			request.setRecipient("새수령인");
			request.setRecipientPhone("010-3333-4444");
			request.setIsDefault(true);  // 기본 배송지로 설정

			given(userService.findUserById(userId)).willReturn(user);
			given(addressRepository.save(any(AddressEntity.class))).willReturn(address);

			// when & then
			assertThatCode(() -> addressService.createAddress(userId, request))
				.doesNotThrowAnyException();

			then(addressRepository).should().clearDefaultAddress(userId);
			then(addressRepository).should().save(any(AddressEntity.class));
		}
	}

	@Nested
	@DisplayName("배송지 수정 테스트")
	class UpdateAddressTest {

		@Test
		@DisplayName("배송지 수정 성공")
		void updateAddress_success() {
			// given
			ReqAddressDtoV1 request = new ReqAddressDtoV1();
			request.setZipCode("99999");
			request.setAddress("부산시 해운대구");
			request.setDetailAddress("404호");
			request.setRecipient("수정수령인");
			request.setRecipientPhone("010-5555-6666");
			request.setIsDefault(false);

			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.of(address));

			// when & then
			assertThatCode(() -> addressService.updateAddress(userId, addressId, request))
				.doesNotThrowAnyException();

			assertThat(address.getZipCode()).isEqualTo("99999");
			assertThat(address.getAddress()).isEqualTo("부산시 해운대구");
		}

		@Test
		@DisplayName("존재하지 않는 배송지 수정 시 실패")
		void updateAddress_notFound_fail() {
			// given
			ReqAddressDtoV1 request = new ReqAddressDtoV1();
			request.setZipCode("99999");

			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> addressService.updateAddress(userId, addressId, request))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
		}

		@Test
		@DisplayName("기본 배송지로 수정 시 기존 기본 배송지 해제")
		void updateAddress_asDefault_clearPreviousDefault() {
			// given
			ReqAddressDtoV1 request = new ReqAddressDtoV1();
			request.setZipCode("99999");
			request.setAddress("부산시 해운대구");
			request.setDetailAddress("404호");
			request.setRecipient("수정수령인");
			request.setRecipientPhone("010-5555-6666");
			request.setIsDefault(true);

			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.of(address));

			// when & then
			assertThatCode(() -> addressService.updateAddress(userId, addressId, request))
				.doesNotThrowAnyException();

			then(addressRepository).should().clearDefaultAddress(userId);
		}
	}

	@Nested
	@DisplayName("배송지 삭제 테스트")
	class DeleteAddressTest {

		@Test
		@DisplayName("배송지 삭제 성공")
		void deleteAddress_success() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.of(address));
			willDoNothing().given(addressRepository).delete(any(AddressEntity.class));

			// when & then
			assertThatCode(() -> addressService.deleteAddress(userId, addressId))
				.doesNotThrowAnyException();

			then(addressRepository).should().delete(address);
		}

		@Test
		@DisplayName("존재하지 않는 배송지 삭제 시 실패")
		void deleteAddress_notFound_fail() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> addressService.deleteAddress(userId, addressId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("기본 배송지 설정 테스트")
	class SetDefaultAddressTest {

		@Test
		@DisplayName("기본 배송지 설정 성공")
		void setDefaultAddress_success() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.of(address));

			// when & then
			assertThatCode(() -> addressService.setDefaultAddress(userId, addressId))
				.doesNotThrowAnyException();

			then(addressRepository).should().clearDefaultAddress(userId);
			assertThat(address.getIsDefault()).isTrue();
		}

		@Test
		@DisplayName("이미 기본 배송지인 경우 실패")
		void setDefaultAddress_alreadyDefault_fail() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(any(UUID.class), eq(userId)))
				.willReturn(Optional.of(defaultAddress));

			// when & then
			assertThatThrownBy(() -> addressService.setDefaultAddress(userId, defaultAddress.getAddressId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_DEFAULT_ADDRESS);
		}

		@Test
		@DisplayName("존재하지 않는 배송지를 기본으로 설정 시 실패")
		void setDefaultAddress_notFound_fail() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> addressService.setDefaultAddress(userId, addressId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("배송지 단건 조회 테스트")
	class GetAddressTest {

		@Test
		@DisplayName("배송지 단건 조회 성공")
		void getAddress_success() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.of(address));

			// when
			ResAddressDtoV1 result = addressService.getAddress(addressId, userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getZipCode()).isEqualTo("12345");
			assertThat(result.getAddress()).isEqualTo("서울시 강남구");
		}

		@Test
		@DisplayName("존재하지 않는 배송지 조회 시 실패")
		void getAddress_notFound_fail() {
			// given
			given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> addressService.getAddress(addressId, userId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
		}
	}
}
