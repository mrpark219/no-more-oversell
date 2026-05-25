package me.park.nomoreoversell.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.exception.*;
import me.park.nomoreoversell.inventory.service.InventoryService;
import me.park.nomoreoversell.order.domain.Order;
import me.park.nomoreoversell.order.domain.OrderStatus;
import me.park.nomoreoversell.order.repository.OrderRepository;
import me.park.nomoreoversell.ordersheet.domain.OrderSheet;
import me.park.nomoreoversell.ordersheet.domain.OrderSheetFailureReason;
import me.park.nomoreoversell.ordersheet.domain.OrderSheetStatus;
import me.park.nomoreoversell.ordersheet.repository.OrderSheetRepository;
import me.park.nomoreoversell.payment.domain.Payment;
import me.park.nomoreoversell.payment.domain.PaymentDetail;
import me.park.nomoreoversell.payment.method.PaymentApprovalRequest;
import me.park.nomoreoversell.payment.method.PaymentApprovalResult;
import me.park.nomoreoversell.payment.method.PaymentMethodHandlerFinder;
import me.park.nomoreoversell.payment.repository.PaymentRepository;
import me.park.nomoreoversell.payment.service.PaymentDetailRequest;
import me.park.nomoreoversell.payment.service.PaymentPlanValidator;
import me.park.nomoreoversell.stayproduct.service.StayProductService;
import me.park.nomoreoversell.stayproduct.service.StayProductView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderSheetRepository orderSheetRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final StayProductService stayProductService;
    private final TransactionTemplate transactionTemplate;
    private final PaymentPlanValidator paymentPlanValidator;
    private final PaymentMethodHandlerFinder paymentMethodHandlerFinder;

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        var preparation = prepareOrderSheetForApproval(request);
        var orderSheet = preparation.orderSheet();
        if (orderSheet.isConfirmed()) {
            return findConfirmedOrder(orderSheet);
        }

        var stayProduct = preparation.stayProduct();
        Payment payment;
        try {
            payment = approvePayment(request, orderSheet);
        } catch (RuntimeException e) {
            markOrderSheetFailed(orderSheet.getId(), OrderSheetFailureReason.PAYMENT_FAILED);
            throw e;
        }

        try {
            return confirmApprovedOrder(orderSheet.getId(), stayProduct, payment);
        } catch (RuntimeException e) {
            cancelApprovedPayment(request.userId(), payment, "ORDER_CONFIRM_FAILED");
            recordFailedConfirmation(orderSheet.getId(), payment, failureReason(e));
            throw e;
        }
    }

    private OrderPreparation prepareOrderSheetForApproval(CreateOrderRequest request) {
        return transactionTemplate.execute(status -> {
            var orderSheet = orderSheetRepository.getByTokenWithLock(request.orderSheetToken())
                    .orElseThrow(OrderSheetNotFoundException::new);

            validateRequestMatchesOrderSheet(request, orderSheet);
            if (orderSheet.isConfirmed()) {
                return OrderPreparation.confirmed(orderSheet);
            }
            if (orderSheet.isApproving()) {
                throw new OrderInProgressException();
            }
            if (orderSheet.getStatus() != OrderSheetStatus.CREATED) {
                throw new InvalidOrderSheetStateException();
            }

            var stayProduct = stayProductService.getOpenViewWithoutCache(request.stayProductId());
            paymentPlanValidator.validate(orderSheet.getSalePrice(), paymentDetailRequests(request.paymentDetails()));
            orderSheet.markApproving();
            return OrderPreparation.newOrder(orderSheet, stayProduct);
        });
    }

    private Payment approvePayment(CreateOrderRequest request, OrderSheet orderSheet) {
        var payment = Payment.ready(orderSheet.getId(), UUID.randomUUID().toString());
        var approvedPayments = new ArrayList<ApprovedPayment>();
        try {
            for (var paymentDetailRequest : request.paymentDetails()) {
                var approvalRequest = new PaymentApprovalRequest(
                        request.userId(),
                        paymentDetailRequest.paymentMethod(),
                        paymentDetailRequest.amount()
                );
                var approvalResult = paymentMethodHandlerFinder.get(paymentDetailRequest.paymentMethod())
                        .approve(approvalRequest);
                if (!approvalResult.success()) {
                    throw new PaymentFailedException();
                }

                var paymentDetail = PaymentDetail.ready(
                        payment,
                        paymentDetailRequest.paymentMethod(),
                        paymentDetailRequest.amount(),
                        approvalResult.transactionKey()
                );
                paymentDetail.markApproved();
                payment.addDetail(paymentDetail);
                approvedPayments.add(new ApprovedPayment(paymentDetailRequest, approvalRequest, approvalResult, paymentDetail));
            }
            payment.markApproved();
            return payment;
        } catch (RuntimeException e) {
            cancelApprovedPayments(payment, approvedPayments, "PAYMENT_APPROVAL_FAILED");
            payment.markFailed(e.getMessage());
            throw e;
        }
    }

    private CreateOrderResponse confirmApprovedOrder(Long orderSheetId, StayProductView stayProduct, Payment payment) {
        return transactionTemplate.execute(status -> {
            var orderSheet = orderSheetRepository.getByIdWithLock(orderSheetId)
                    .orElseThrow(OrderSheetNotFoundException::new);
            if (orderSheet.isConfirmed()) {
                return findConfirmedOrder(orderSheet);
            }
            if (!orderSheet.isApproving()) {
                throw new InvalidOrderSheetStateException();
            }

            inventoryService.reserveOneStock(orderSheet.getProductId());
            validatePurchaseLimit(orderSheet, stayProduct);
            paymentRepository.save(payment);

            var order = orderRepository.save(Order.confirmed(
                    orderSheet.getId(),
                    orderSheet.getUserId(),
                    orderSheet.getProductId(),
                    orderSheet.getOriginalPrice(),
                    orderSheet.getSalePrice()
            ));
            orderSheet.markConfirmed();

            log.info(
                    "주문 생성 완료. orderId={}, orderSheetId={}, userId={}, productId={}",
                    order.getId(),
                    orderSheet.getId(),
                    orderSheet.getUserId(),
                    orderSheet.getProductId()
            );
            return CreateOrderResponse.from(order, orderSheet.getOrderSheetToken(), stayProduct, payment);
        });
    }

    private void markOrderSheetFailed(Long orderSheetId, OrderSheetFailureReason failureReason) {
        transactionTemplate.execute(status -> {
            var orderSheet = orderSheetRepository.getByIdWithLock(orderSheetId)
                    .orElseThrow(OrderSheetNotFoundException::new);
            orderSheet.markFailed(failureReason);
            return null;
        });
    }

    private void cancelApprovedPayment(Long userId, Payment payment, String reason) {
        var approvedPayments = payment.orderedDetails().stream()
                .map(detail -> new ApprovedPayment(
                        new CreateOrderPaymentRequest(detail.getPaymentMethod(), detail.getPaymentAmount()),
                        new PaymentApprovalRequest(userId, detail.getPaymentMethod(), detail.getPaymentAmount()),
                        PaymentApprovalResult.success(detail.getExternalTransactionKey()),
                        detail
                ))
                .toList();
        cancelApprovedPayments(payment, approvedPayments, reason);
    }

    private void cancelApprovedPayments(Payment payment, List<ApprovedPayment> approvedPayments, String reason) {
        var hasCancelFailure = false;
        for (var approvedPayment : approvedPayments.reversed()) {
            var cancelResult = paymentMethodHandlerFinder.get(approvedPayment.paymentDetailRequest().paymentMethod())
                    .cancel(approvedPayment.approvalRequest(), approvedPayment.approvalResult(), reason);
            if (cancelResult.success()) {
                approvedPayment.paymentDetail().cancel(
                        approvedPayment.paymentDetailRequest().amount(),
                        cancelResult.cancelKey(),
                        reason
                );
                continue;
            }
            hasCancelFailure = true;
            approvedPayment.paymentDetail().markCancelFailed(cancelResult.reason());
        }
        if (hasCancelFailure) {
            payment.markCancelFailed();
            return;
        }
        payment.markCanceled();
    }

    private void recordFailedConfirmation(Long orderSheetId, Payment payment, OrderSheetFailureReason failureReason) {
        transactionTemplate.execute(status -> {
            var orderSheet = orderSheetRepository.getByIdWithLock(orderSheetId)
                    .orElseThrow(OrderSheetNotFoundException::new);
            orderSheet.markFailed(failureReason);
            paymentRepository.save(payment);
            return null;
        });
    }

    private OrderSheetFailureReason failureReason(RuntimeException exception) {
        if (exception instanceof SoldOutException) {
            return OrderSheetFailureReason.SOLD_OUT;
        }
        if (exception instanceof PurchaseLimitExceededException) {
            return OrderSheetFailureReason.PURCHASE_LIMIT_EXCEEDED;
        }
        return OrderSheetFailureReason.ORDER_CONFIRM_FAILED;
    }

    private void validateRequestMatchesOrderSheet(CreateOrderRequest request, OrderSheet orderSheet) {
        if (!orderSheet.getUserId().equals(request.userId())) {
            throw new OrderSheetOwnerMismatchException();
        }
        if (!orderSheet.getProductId().equals(request.stayProductId())) {
            throw new InvalidOrderSheetStateException();
        }
    }

    private void validatePurchaseLimit(OrderSheet orderSheet, StayProductView stayProduct) {
        if (stayProduct.maxPerUser() == null) {
            return;
        }

        var confirmedOrderCount = orderRepository.countByUserIdAndProductIdAndStatus(
                orderSheet.getUserId(),
                orderSheet.getProductId(),
                OrderStatus.CONFIRMED
        );
        if (confirmedOrderCount >= stayProduct.maxPerUser()) {
            log.info(
                    "인당 구매 제한 초과. userId={}, productId={}, confirmedOrderCount={}, maxPerUser={}",
                    orderSheet.getUserId(),
                    orderSheet.getProductId(),
                    confirmedOrderCount,
                    stayProduct.maxPerUser()
            );
            throw new PurchaseLimitExceededException();
        }
    }

    private CreateOrderResponse findConfirmedOrder(OrderSheet orderSheet) {
        var stayProduct = stayProductService.getViewWithoutCache(orderSheet.getProductId());
        var order = orderRepository.findByOrderSheetId(orderSheet.getId())
                .orElseThrow(InvalidOrderSheetStateException::new);
        var payment = paymentRepository.findByOrderSheetId(orderSheet.getId())
                .orElseThrow(InvalidOrderSheetStateException::new);
        return CreateOrderResponse.from(order, orderSheet.getOrderSheetToken(), stayProduct, payment);
    }

    private List<PaymentDetailRequest> paymentDetailRequests(List<CreateOrderPaymentRequest> paymentDetails) {
        return paymentDetails.stream()
                .map(detail -> new PaymentDetailRequest(detail.paymentMethod(), detail.amount()))
                .toList();
    }

    private record ApprovedPayment(
            CreateOrderPaymentRequest paymentDetailRequest,
            PaymentApprovalRequest approvalRequest,
            PaymentApprovalResult approvalResult,
            PaymentDetail paymentDetail
    ) {
    }

    private record OrderPreparation(OrderSheet orderSheet, StayProductView stayProduct) {

        private static OrderPreparation confirmed(OrderSheet orderSheet) {
            return new OrderPreparation(orderSheet, null);
        }

        private static OrderPreparation newOrder(OrderSheet orderSheet, StayProductView stayProduct) {
            return new OrderPreparation(orderSheet, stayProduct);
        }
    }
}
