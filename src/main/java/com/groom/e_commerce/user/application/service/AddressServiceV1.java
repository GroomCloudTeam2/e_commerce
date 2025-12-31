package com.groom.e_commerce.user.application.service;

import com.groom.e_commerce.global.presentation.advice.CustomException;
import com.groom.e_commerce.global.presentation.advice.ErrorCode;
import com.groom.e_commerce.user.domain.entity.AddressEntity;
import com.groom.e_commerce.user.domain.entity.UserEntity;
import com.groom.e_commerce.user.domain.repository.AddressRepository;
import com.groom.e_commerce.user.presentation.dto.request.ReqAddressDtoV1;
import com.groom.e_commerce.user.presentation.dto.response.ResAddressDtoV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceV1 {

    private final AddressRepository addressRepository;
    private final UserServiceV1 userService;

    public List<ResAddressDtoV1> getAddresses(UUID userId) {
        return addressRepository.findByUserUserId(userId).stream()
                .map(ResAddressDtoV1::from)
                .toList();
    }

    @Transactional
    public void createAddress(UUID userId, ReqAddressDtoV1 request) {
        UserEntity user = userService.findUserById(userId);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultAddress(userId);
        }

        AddressEntity address = AddressEntity.builder()
                .user(user)
                .zipCode(request.getZipCode())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        addressRepository.save(address);
        log.info("Address created for user: {}", userId);
    }

    @Transactional
    public void updateAddress(UUID userId, UUID addressId, ReqAddressDtoV1 request) {
        AddressEntity address = findAddressByIdAndUserId(addressId, userId);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultAddress(userId);
        }

        address.update(
                request.getZipCode(),
                request.getAddress(),
                request.getDetailAddress(),
                request.getIsDefault()
        );

        log.info("Address updated: {}", addressId);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        AddressEntity address = findAddressByIdAndUserId(addressId, userId);
        addressRepository.delete(address);
        log.info("Address deleted: {}", addressId);
    }

    @Transactional
    public void setDefaultAddress(UUID userId, UUID addressId) {
        AddressEntity address = findAddressByIdAndUserId(addressId, userId);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            throw new CustomException(ErrorCode.ALREADY_DEFAULT_ADDRESS);
        }

        addressRepository.clearDefaultAddress(userId);
        address.setDefault(true);
        log.info("Default address set: {}", addressId);
    }

    private AddressEntity findAddressByIdAndUserId(UUID addressId, UUID userId) {
        return addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
    }
}
