package me.park.nomoreoversell.ordersheet.service;

import lombok.Builder;
import me.park.nomoreoversell.stayproduct.service.StayProductView;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record CheckoutResponse(
        String orderSheetToken,
        StayProductDetail stayProduct,
        UserDetail user
) {

    public record StayProductDetail(
            Long id,
            String accommodationName,
            String roomName,
            String ratePlanName,
            Long originalPrice,
            Long salePrice,
            LocalDateTime openAt,
            LocalTime checkinTime,
            LocalTime checkoutTime,
            Long maxPerUser,
            Boolean hasStock
    ) {
        public static StayProductDetail from(StayProductView stayProduct, boolean hasStock) {
            return new StayProductDetail(
                    stayProduct.id(),
                    stayProduct.accommodationName(),
                    stayProduct.roomName(),
                    stayProduct.ratePlanName(),
                    stayProduct.originalPrice(),
                    stayProduct.salePrice(),
                    stayProduct.openAt(),
                    stayProduct.checkinTime(),
                    stayProduct.checkoutTime(),
                    stayProduct.maxPerUser(),
                    hasStock
            );
        }
    }

    public record UserDetail(Long availablePoint) {
    }
}
