package me.park.nomoreoversell.order.service;

import me.park.nomoreoversell.order.domain.Order;
import me.park.nomoreoversell.order.domain.OrderStatus;
import me.park.nomoreoversell.payment.domain.Payment;
import me.park.nomoreoversell.payment.domain.PaymentDetail;
import me.park.nomoreoversell.payment.domain.PaymentDetailStatus;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.payment.domain.PaymentStatus;
import me.park.nomoreoversell.stayproduct.service.StayProductView;

import java.util.List;

public record CreateOrderResponse(
        OrderDetail order,
        StayProductDetail stayProduct,
        PaymentInfo payment
) {

    public static CreateOrderResponse from(
            Order order,
            String orderSheetToken,
            StayProductView stayProduct,
            Payment payment
    ) {
        return new CreateOrderResponse(
                new OrderDetail(order.getId(), order.getOrderToken(), orderSheetToken, order.getStatus()),
                StayProductDetail.from(stayProduct),
                PaymentInfo.from(payment)
        );
    }

    public record OrderDetail(
            Long id,
            String orderToken,
            String orderSheetToken,
            OrderStatus status
    ) {
    }

    public record StayProductDetail(
            Long id,
            String accommodationName,
            String roomName,
            String ratePlanName
    ) {
        public static StayProductDetail from(StayProductView stayProduct) {
            return new StayProductDetail(
                    stayProduct.id(),
                    stayProduct.accommodationName(),
                    stayProduct.roomName(),
                    stayProduct.ratePlanName()
            );
        }
    }

    public record PaymentInfo(
            PaymentStatus status,
            long totalPaymentAmount,
            List<PaymentDetailInfo> details
    ) {
        public static PaymentInfo from(Payment payment) {
            return new PaymentInfo(
                    payment.getStatus(),
                    payment.totalPaymentAmount(),
                    payment.orderedDetails().stream()
                            .map(PaymentDetailInfo::from)
                            .toList()
            );
        }
    }

    public record PaymentDetailInfo(
            PaymentMethod paymentMethod,
            long paymentAmount,
            PaymentDetailStatus status
    ) {
        public static PaymentDetailInfo from(PaymentDetail paymentDetail) {
            return new PaymentDetailInfo(
                    paymentDetail.getPaymentMethod(),
                    paymentDetail.getPaymentAmount(),
                    paymentDetail.getStatus()
            );
        }
    }
}
