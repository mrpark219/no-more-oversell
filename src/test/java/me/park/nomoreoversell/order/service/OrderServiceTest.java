package me.park.nomoreoversell.order.service;

import me.park.nomoreoversell.inventory.service.InventoryService;
import me.park.nomoreoversell.order.domain.Order;
import me.park.nomoreoversell.order.domain.OrderStatus;
import me.park.nomoreoversell.order.repository.OrderRepository;
import me.park.nomoreoversell.ordersheet.domain.OrderSheetFailureReason;
import me.park.nomoreoversell.ordersheet.domain.OrderSheet;
import me.park.nomoreoversell.ordersheet.domain.OrderSheetStatus;
import me.park.nomoreoversell.ordersheet.repository.OrderSheetRepository;
import me.park.nomoreoversell.exception.OrderInProgressException;
import me.park.nomoreoversell.exception.PaymentFailedException;
import me.park.nomoreoversell.exception.PurchaseLimitExceededException;
import me.park.nomoreoversell.exception.StayProductNotOpenException;
import me.park.nomoreoversell.payment.domain.Payment;
import me.park.nomoreoversell.payment.domain.PaymentDetailStatus;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.payment.domain.PaymentStatus;
import me.park.nomoreoversell.payment.method.PaymentApprovalResult;
import me.park.nomoreoversell.payment.method.PaymentCancelResult;
import me.park.nomoreoversell.payment.method.PaymentMethodHandler;
import me.park.nomoreoversell.payment.method.PaymentMethodHandlerFinder;
import me.park.nomoreoversell.payment.repository.PaymentRepository;
import me.park.nomoreoversell.payment.service.PaymentDetailRequest;
import me.park.nomoreoversell.payment.service.PaymentPlanValidator;
import me.park.nomoreoversell.stayproduct.domain.StayProductStatus;
import me.park.nomoreoversell.stayproduct.service.StayProductService;
import me.park.nomoreoversell.stayproduct.service.StayProductView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderSheetRepository orderSheetRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private StayProductService stayProductService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private PaymentMethodHandler cardPaymentHandler;

    @Mock
    private PaymentMethodHandler pointPaymentHandler;

    @Mock
    private PaymentMethodHandlerFinder paymentMethodHandlerFinder;

    @Mock
    private PaymentPlanValidator paymentPlanValidator;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderSheetRepository,
                orderRepository,
                paymentRepository,
                inventoryService,
                stayProductService,
                transactionTemplate,
                paymentPlanValidator,
                paymentMethodHandlerFinder
        );
        given(transactionTemplate.execute(any()))
                .willAnswer(invocation -> invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    @DisplayName("주문서와 결제수단 정보로 결제 후 주문을 생성한다")
    void bookCreatesConfirmedOrder() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.CREATED);
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));
        given(orderSheetRepository.getByIdWithLock(1L))
                .willReturn(Optional.of(orderSheet));
        given(stayProductService.getOpenViewWithoutCache(10L))
                .willReturn(stayProduct());
        given(orderRepository.countByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.CONFIRMED))
                .willReturn(0L);
        given(paymentMethodHandlerFinder.get(PaymentMethod.CARD))
                .willReturn(cardPaymentHandler);
        given(paymentMethodHandlerFinder.get(PaymentMethod.POINT))
                .willReturn(pointPaymentHandler);
        given(cardPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.success("PG-CARD-1"));
        given(pointPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.success("POINT:1:1000"));
        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> {
                    var order = invocation.getArgument(0, Order.class);
                    ReflectionTestUtils.setField(order, "id", 100L);
                    return order;
                });

        // when
        var response = orderService.createOrder(request);

        // then
        assertThat(response.order().id()).isEqualTo(100L);
        assertThat(response.order().orderToken()).isNotBlank();
        assertThat(response.order().orderSheetToken()).isEqualTo("sheet-token");
        assertThat(response.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.stayProduct().id()).isEqualTo(10L);
        assertThat(response.stayProduct().accommodationName()).isEqualTo("테스트 호텔");
        assertThat(response.stayProduct().roomName()).isEqualTo("디럭스");
        assertThat(response.stayProduct().ratePlanName()).isEqualTo("특가");
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.payment().totalPaymentAmount()).isEqualTo(10_000L);
        assertThat(response.payment().details()).hasSize(2);
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.CONFIRMED);

        verify(paymentPlanValidator).validate(10_000L, paymentDetailRequests());
        verify(cardPaymentHandler).approve(any());
        verify(pointPaymentHandler).approve(any());
        var inOrder = inOrder(cardPaymentHandler, pointPaymentHandler, inventoryService, orderRepository);
        inOrder.verify(cardPaymentHandler).approve(any());
        inOrder.verify(pointPaymentHandler).approve(any());
        inOrder.verify(inventoryService).reserveOneStock(10L);
        inOrder.verify(orderRepository).countByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.CONFIRMED);
        verify(stayProductService, never()).getOpenView(10L);

        var paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentCaptor.getValue().getDetails()).hasSize(2);
    }

    @Test
    @DisplayName("이미 확정된 주문서로 다시 요청하면 기존 주문 정보를 반환한다")
    void bookReturnsExistingOrderWhenOrderSheetAlreadyConfirmed() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.CONFIRMED);
        var order = confirmedOrder(orderSheet);
        var payment = approvedPayment(orderSheet);
        ReflectionTestUtils.setField(order, "id", 100L);
        given(stayProductService.getViewWithoutCache(10L))
                .willReturn(stayProduct());
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));
        given(orderRepository.findByOrderSheetId(1L))
                .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderSheetId(1L))
                .willReturn(Optional.of(payment));

        // when
        var response = orderService.createOrder(request);

        // then
        assertThat(response.order().id()).isEqualTo(100L);
        assertThat(response.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(inventoryService, never()).reserveOneStock(10L);
        verify(stayProductService, never()).getView(10L);
    }

    @Test
    @DisplayName("PG 승인 이후 재고 락과 인당 구매 제한을 확인하고 초과 시 승인 결제를 취소한다")
    void createOrderChecksPurchaseLimitAfterInventoryLock() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.CREATED);
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));
        given(orderSheetRepository.getByIdWithLock(1L))
                .willReturn(Optional.of(orderSheet));
        given(stayProductService.getOpenViewWithoutCache(10L))
                .willReturn(stayProduct());
        given(paymentMethodHandlerFinder.get(PaymentMethod.CARD))
                .willReturn(cardPaymentHandler);
        given(paymentMethodHandlerFinder.get(PaymentMethod.POINT))
                .willReturn(pointPaymentHandler);
        given(cardPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.success("PG-CARD-1"));
        given(pointPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.success("POINT:1:1000"));
        given(cardPaymentHandler.cancel(any(), any(), any()))
                .willReturn(PaymentCancelResult.success("CANCEL-CARD-1"));
        given(pointPaymentHandler.cancel(any(), any(), any()))
                .willReturn(PaymentCancelResult.success("CANCEL-POINT-1"));
        given(orderRepository.countByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.CONFIRMED))
                .willReturn(1L);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(PurchaseLimitExceededException.class);

        var inOrder = inOrder(inventoryService, orderRepository);
        inOrder.verify(inventoryService).reserveOneStock(10L);
        inOrder.verify(orderRepository).countByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.CONFIRMED);
        verify(cardPaymentHandler).cancel(any(), any(), any());
        verify(pointPaymentHandler).cancel(any(), any(), any());
        verify(paymentRepository).save(any(Payment.class));
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.FAILED);
        assertThat(orderSheet.getFailureReason()).isEqualTo(OrderSheetFailureReason.PURCHASE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("PG 승인에 실패하면 재고 락을 잡지 않고 주문서를 결제 실패로 변경한다")
    void createOrderDoesNotLockInventoryWhenPaymentApprovalFails() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.CREATED);
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));
        given(orderSheetRepository.getByIdWithLock(1L))
                .willReturn(Optional.of(orderSheet));
        given(stayProductService.getOpenViewWithoutCache(10L))
                .willReturn(stayProduct());
        given(paymentMethodHandlerFinder.get(PaymentMethod.CARD))
                .willReturn(cardPaymentHandler);
        given(cardPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.fail("PG_APPROVAL_FAILED"));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(PaymentFailedException.class);

        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.FAILED);
        assertThat(orderSheet.getFailureReason()).isEqualTo(OrderSheetFailureReason.PAYMENT_FAILED);
        verify(inventoryService, never()).reserveOneStock(10L);
        verify(pointPaymentHandler, never()).approve(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("주문서가 이미 처리 중이면 처리 중 주문 예외를 던진다")
    void createOrderThrowsOrderInProgressWhenOrderSheetIsApproving() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.APPROVING);
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderInProgressException.class);

        verify(paymentPlanValidator, never()).validate(anyLong(), any());
        verify(cardPaymentHandler, never()).approve(any());
        verify(inventoryService, never()).reserveOneStock(10L);
    }

    @Test
    @DisplayName("주문 생성 시 상품이 오픈되지 않았으면 결제를 승인하지 않는다")
    void createOrderRejectsNotOpenProductBeforePaymentApproval() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.CREATED);
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));
        given(stayProductService.getOpenViewWithoutCache(10L))
                .willThrow(new StayProductNotOpenException());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(StayProductNotOpenException.class);

        verify(paymentPlanValidator, never()).validate(anyLong(), any());
        verify(cardPaymentHandler, never()).approve(any());
        verify(inventoryService, never()).reserveOneStock(10L);
    }

    @Test
    @DisplayName("확정 실패 후 PG 취소에 실패하면 취소 실패 상태 결제를 기록한다")
    void createOrderRecordsCancelFailedPaymentWhenCancelFails() {
        // given
        var request = bookingRequest();
        var orderSheet = orderSheet(OrderSheetStatus.CREATED);
        given(orderSheetRepository.getByTokenWithLock("sheet-token"))
                .willReturn(Optional.of(orderSheet));
        given(orderSheetRepository.getByIdWithLock(1L))
                .willReturn(Optional.of(orderSheet));
        given(stayProductService.getOpenViewWithoutCache(10L))
                .willReturn(stayProduct());
        given(paymentMethodHandlerFinder.get(PaymentMethod.CARD))
                .willReturn(cardPaymentHandler);
        given(paymentMethodHandlerFinder.get(PaymentMethod.POINT))
                .willReturn(pointPaymentHandler);
        given(cardPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.success("PG-CARD-1"));
        given(pointPaymentHandler.approve(any()))
                .willReturn(PaymentApprovalResult.success("POINT:1:1000"));
        given(pointPaymentHandler.cancel(any(), any(), any()))
                .willReturn(PaymentCancelResult.success("CANCEL-POINT-1"));
        given(cardPaymentHandler.cancel(any(), any(), any()))
                .willReturn(PaymentCancelResult.fail("PG_CANCEL_FAILED"));
        given(orderRepository.countByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.CONFIRMED))
                .willReturn(1L);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(PurchaseLimitExceededException.class);

        var paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.CANCEL_FAILED);
        assertThat(paymentCaptor.getValue().getDetails())
                .anyMatch(detail -> detail.getStatus() == PaymentDetailStatus.CANCEL_FAILED);
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.FAILED);
        assertThat(orderSheet.getFailureReason()).isEqualTo(OrderSheetFailureReason.PURCHASE_LIMIT_EXCEEDED);
    }

    private CreateOrderRequest bookingRequest() {
        return new CreateOrderRequest(
                1L,
                "sheet-token",
                10L,
                List.of(
                        new CreateOrderPaymentRequest(PaymentMethod.CARD, 9_000L),
                        new CreateOrderPaymentRequest(PaymentMethod.POINT, 1_000L)
                )
        );
    }

    private List<PaymentDetailRequest> paymentDetailRequests() {
        return List.of(
                new PaymentDetailRequest(PaymentMethod.CARD, 9_000L),
                new PaymentDetailRequest(PaymentMethod.POINT, 1_000L)
        );
    }

    private OrderSheet orderSheet(OrderSheetStatus status) {
        var orderSheet = OrderSheet.builder()
                .orderSheetToken("sheet-token")
                .userId(1L)
                .productId(10L)
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .status(status)
                .build();
        ReflectionTestUtils.setField(orderSheet, "id", 1L);
        return orderSheet;
    }

    private Order confirmedOrder(OrderSheet orderSheet) {
        return Order.confirmed(
                orderSheet.getId(),
                orderSheet.getUserId(),
                orderSheet.getProductId(),
                orderSheet.getOriginalPrice(),
                orderSheet.getSalePrice()
        );
    }

    private Payment approvedPayment(OrderSheet orderSheet) {
        var payment = Payment.ready(orderSheet.getId(), "payment-token");
        var paymentDetail = me.park.nomoreoversell.payment.domain.PaymentDetail.ready(
                payment,
                PaymentMethod.CARD,
                10_000L,
                "PG-CARD-1"
        );
        paymentDetail.markApproved();
        payment.addDetail(paymentDetail);
        payment.markApproved();
        return payment;
    }

    private StayProductView stayProduct() {
        return StayProductView.builder()
                .id(10L)
                .accommodationName("테스트 호텔")
                .roomName("디럭스")
                .ratePlanName("특가")
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .openAt(LocalDateTime.now().minusDays(1))
                .status(StayProductStatus.OPEN)
                .checkinTime(LocalTime.of(15, 0))
                .checkoutTime(LocalTime.of(11, 0))
                .maxPerUser(1L)
                .build();
    }
}
