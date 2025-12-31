package com.groom.e_commerce.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_seller")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SellerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seller_id", columnDefinition = "uuid")
    private UUID sellerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "store_name", length = 200, nullable = false)
    private String storeName;

    @Column(name = "business_no", length = 50)
    private String businessNo;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    @Column(name = "bank", length = 50)
    private String bank;

    @Column(name = "account", length = 100)
    private String account;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateInfo(String storeName, String businessNo, String zipCode,
                           String address, String detailAddress, String bank, String account) {
        if (storeName != null) this.storeName = storeName;
        if (businessNo != null) this.businessNo = businessNo;
        if (zipCode != null) this.zipCode = zipCode;
        if (address != null) this.address = address;
        if (detailAddress != null) this.detailAddress = detailAddress;
        if (bank != null) this.bank = bank;
        if (account != null) this.account = account;
    }
}
