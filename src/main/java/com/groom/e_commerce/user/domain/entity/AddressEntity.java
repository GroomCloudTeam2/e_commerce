package com.groom.e_commerce.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "address_id", columnDefinition = "uuid")
    private UUID addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "recipient", length = 50)
    private String recipient;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "zip_code", length = 10, nullable = false)
    private String zipCode;

    @Column(name = "address", length = 200, nullable = false)
    private String address;

    @Column(name = "detail_address", length = 200, nullable = false)
    private String detailAddress;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String zipCode, String address, String detailAddress, Boolean isDefault) {
        this.zipCode = zipCode;
        this.address = address;
        this.detailAddress = detailAddress;
        if (isDefault != null) {
            this.isDefault = isDefault;
        }
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
