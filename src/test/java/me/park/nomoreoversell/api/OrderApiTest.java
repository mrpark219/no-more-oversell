package me.park.nomoreoversell.api;

import me.park.nomoreoversell.common.ApiExceptionHandler;
import me.park.nomoreoversell.common.web.UserIdArgumentResolver;
import me.park.nomoreoversell.common.web.WebMvcConfig;
import me.park.nomoreoversell.exception.SoldOutException;
import me.park.nomoreoversell.order.controller.OrderController;
import me.park.nomoreoversell.order.service.CreateOrderPaymentRequest;
import me.park.nomoreoversell.order.domain.OrderStatus;
import me.park.nomoreoversell.order.service.CreateOrderRequest;
import me.park.nomoreoversell.order.service.CreateOrderResponse;
import me.park.nomoreoversell.order.service.OrderService;
import me.park.nomoreoversell.ordersheet.controller.CheckoutController;
import me.park.nomoreoversell.ordersheet.service.CheckoutRequest;
import me.park.nomoreoversell.ordersheet.service.CheckoutResponse;
import me.park.nomoreoversell.ordersheet.service.OrderSheetService;
import me.park.nomoreoversell.payment.domain.PaymentDetailStatus;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CheckoutController.class, OrderController.class})
@Import({ApiExceptionHandler.class, WebMvcConfig.class, UserIdArgumentResolver.class})
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderSheetService orderSheetService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("체크아웃 API는 userId 헤더와 상품 ID 파라미터로 주문서를 조회한다")
    void checkoutReturnsOrderSheet() throws Exception {
        // given
        given(orderSheetService.prepareCheckout(new CheckoutRequest(1L, 10L)))
                .willReturn(checkoutResponse());

        // when & then
        mockMvc.perform(get("/api/checkout")
                        .header("userId", "1")
                        .param("stayProductId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderSheetToken").value("sheet-token"))
                .andExpect(jsonPath("$.stayProduct.id").value(10))
                .andExpect(jsonPath("$.stayProduct.accommodationName").value("테스트 호텔"))
                .andExpect(jsonPath("$.stayProduct.hasStock").value(true))
                .andExpect(jsonPath("$.user.availablePoint").value(5000));

        verify(orderSheetService).prepareCheckout(new CheckoutRequest(1L, 10L));
    }

    @Test
    @DisplayName("체크아웃 API는 잘못된 상품 ID 파라미터를 거부한다")
    void checkoutRejectsInvalidStayProductId() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/checkout")
                        .header("userId", "1")
                        .param("stayProductId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

        verifyNoInteractions(orderSheetService);
    }

    @Test
    @DisplayName("주문 API는 userId 헤더와 주문서, 상품, 결제수단 정보로 주문을 생성한다")
    void createOrderReturnsConfirmedOrder() throws Exception {
        // given
        var request = new CreateOrderRequest(
                1L,
                "sheet-token",
                10L,
                List.of(
                        new CreateOrderPaymentRequest(PaymentMethod.CARD, 9_000L),
                        new CreateOrderPaymentRequest(PaymentMethod.POINT, 1_000L)
                )
        );
        given(orderService.createOrder(request))
                .willReturn(createOrderResponse());

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderSheetToken": "sheet-token",
                                  "stayProductId": 10,
                                  "paymentDetails": [
                                    {
                                      "paymentMethod": "CARD",
                                      "amount": 9000
                                    },
                                    {
                                      "paymentMethod": "POINT",
                                      "amount": 1000
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(100))
                .andExpect(jsonPath("$.order.orderToken").value("order-token"))
                .andExpect(jsonPath("$.order.orderSheetToken").value("sheet-token"))
                .andExpect(jsonPath("$.order.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.stayProduct.id").value(10))
                .andExpect(jsonPath("$.stayProduct.accommodationName").value("테스트 호텔"))
                .andExpect(jsonPath("$.payment.status").value("APPROVED"))
                .andExpect(jsonPath("$.payment.totalPaymentAmount").value(10_000))
                .andExpect(jsonPath("$.payment.details[0].paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.payment.details[0].paymentAmount").value(9_000))
                .andExpect(jsonPath("$.payment.details[0].status").value("APPROVED"));

        verify(orderService).createOrder(request);
    }

    @Test
    @DisplayName("주문 API는 잘못된 요청 본문을 거부한다")
    void createOrderRejectsInvalidRequestBody() throws Exception {
        // given

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stayProductId": 10,
                                  "paymentDetails": [
                                    {
                                      "paymentMethod": "CARD",
                                      "amount": 9000
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("공통 예외 응답은 비즈니스 예외의 코드와 메시지를 내려준다")
    void apiExceptionReturnsErrorResponse() throws Exception {
        // given
        given(orderSheetService.prepareCheckout(new CheckoutRequest(1L, 10L)))
                .willThrow(new SoldOutException());

        // when & then
        mockMvc.perform(get("/api/checkout")
                        .header("userId", "1")
                        .param("stayProductId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLD_OUT"))
                .andExpect(jsonPath("$.message").value("품절된 상품입니다."));
    }

    private CheckoutResponse checkoutResponse() {
        return CheckoutResponse.builder()
                .orderSheetToken("sheet-token")
                .stayProduct(new CheckoutResponse.StayProductDetail(
                        10L,
                        "테스트 호텔",
                        "디럭스",
                        "특가",
                        20_000L,
                        10_000L,
                        LocalDateTime.of(2026, 5, 25, 12, 0),
                        LocalTime.of(15, 0),
                        LocalTime.of(11, 0),
                        1L,
                        true
                ))
                .user(new CheckoutResponse.UserDetail(5_000L))
                .build();
    }

    private CreateOrderResponse createOrderResponse() {
        return new CreateOrderResponse(
                new CreateOrderResponse.OrderDetail(100L, "order-token", "sheet-token", OrderStatus.CONFIRMED),
                new CreateOrderResponse.StayProductDetail(10L, "테스트 호텔", "디럭스", "특가"),
                new CreateOrderResponse.PaymentInfo(
                        PaymentStatus.APPROVED,
                        10_000L,
                        List.of(
                                new CreateOrderResponse.PaymentDetailInfo(
                                        PaymentMethod.CARD,
                                        9_000L,
                                        PaymentDetailStatus.APPROVED
                                ),
                                new CreateOrderResponse.PaymentDetailInfo(
                                        PaymentMethod.POINT,
                                        1_000L,
                                        PaymentDetailStatus.APPROVED
                                )
                        )
                )
        );
    }
}
