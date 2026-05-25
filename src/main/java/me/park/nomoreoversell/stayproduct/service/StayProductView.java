package me.park.nomoreoversell.stayproduct.service;

import lombok.Builder;
import me.park.nomoreoversell.stayproduct.domain.StayProduct;
import me.park.nomoreoversell.stayproduct.domain.StayProductStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record StayProductView(
        Long id,
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

    public static StayProductView from(StayProduct stayProduct) {
        return StayProductView.builder()
                .id(stayProduct.getId())
                .accommodationName(stayProduct.getAccommodationName())
                .roomName(stayProduct.getRoomName())
                .ratePlanName(stayProduct.getRatePlanName())
                .originalPrice(stayProduct.getOriginalPrice())
                .salePrice(stayProduct.getSalePrice())
                .openAt(stayProduct.getOpenAt())
                .status(stayProduct.getStatus())
                .checkinTime(stayProduct.getCheckinTime())
                .checkoutTime(stayProduct.getCheckoutTime())
                .maxPerUser(stayProduct.getMaxPerUser())
                .build();
    }

    public boolean isOpenAt(LocalDateTime now) {
        return status == StayProductStatus.OPEN && !now.isBefore(openAt);
    }
}
