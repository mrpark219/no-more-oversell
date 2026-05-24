package me.park.nomoreoversell.stayproduct.doamin;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "stay_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StayProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accommodationName;

    @Column(nullable = false)
    private String roomName;

    @Column(nullable = false)
    private String ratePlanName;

    @Column(nullable = false)
    private Long originalPrice;

    @Column(nullable = false)
    private Long salePrice;

    @Column(nullable = false)
    private LocalDateTime openAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StayProductStatus status;

    @Column(nullable = false)
    private LocalTime checkinTime;

    @Column(nullable = false)
    private LocalTime checkoutTime;

    private Long maxPerUser;

    @Builder
    public StayProduct(
            String accommodationName,
            String roomName,
            String ratePlanName,
            Long originalPrice,
            Long salePrice,
            LocalDateTime openAt,
            StayProductStatus status,
            LocalTime checkinTime,
            LocalTime checkoutTime,
            Long maxPerUser
    ) {
        this.accommodationName = accommodationName;
        this.roomName = roomName;
        this.ratePlanName = ratePlanName;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.openAt = openAt;
        this.status = status;
        this.checkinTime = checkinTime;
        this.checkoutTime = checkoutTime;
        this.maxPerUser = maxPerUser;
    }

    public boolean hasPurchaseLimit() {
        return maxPerUser != null;
    }

    public boolean isOpenAt(LocalDateTime now) {
        return status == StayProductStatus.OPEN && !now.isBefore(openAt);
    }

    public void open() {
        status = StayProductStatus.OPEN;
    }

    public void close() {
        status = StayProductStatus.CLOSED;
    }
}
