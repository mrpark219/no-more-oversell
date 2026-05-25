package me.park.nomoreoversell.ordersheet.service;

import me.park.nomoreoversell.exception.StayProductNotOpenException;
import me.park.nomoreoversell.inventory.service.InventoryService;
import me.park.nomoreoversell.ordersheet.domain.OrderSheet;
import me.park.nomoreoversell.ordersheet.repository.OrderSheetRepository;
import me.park.nomoreoversell.point.service.PointService;
import me.park.nomoreoversell.stayproduct.domain.StayProductStatus;
import me.park.nomoreoversell.stayproduct.service.StayProductService;
import me.park.nomoreoversell.stayproduct.service.StayProductView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderSheetServiceTest {

    @Mock
    private OrderSheetRepository orderSheetRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private StayProductService stayProductService;

    @Mock
    private PointService pointService;

    @Mock
    private CheckoutResponseCache checkoutResponseCache;

    @InjectMocks
    private OrderSheetService orderSheetService;

    @Test
    @DisplayName("체크아웃 진입 시 상품, 재고, 포인트를 조회하고 주문서 토큰을 발급한다")
    void checkoutCreatesOrderSheetToken() {
        // given
        var userId = 1L;
        var productId = 10L;
        given(checkoutResponseCache.get(userId, productId)).willReturn(Optional.empty());
        given(stayProductService.getOpen(productId)).willReturn(stayProduct(productId));
        given(inventoryService.hasStock(productId)).willReturn(true);
        given(pointService.available(userId)).willReturn(5_000L);
        given(orderSheetRepository.save(any(OrderSheet.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = orderSheetService.checkout(new CheckoutRequest(userId, productId));

        // then
        assertThat(response.orderSheetToken()).isNotBlank();
        assertThat(response.stayProduct().id()).isEqualTo(productId);
        assertThat(response.stayProduct().hasStock()).isTrue();
        assertThat(response.user().availablePoint()).isEqualTo(5_000L);

        var captor = ArgumentCaptor.forClass(OrderSheet.class);
        verify(orderSheetRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderSheetToken()).isEqualTo(response.orderSheetToken());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getProductId()).isEqualTo(productId);
        verify(checkoutResponseCache).put(userId, productId, response);
    }

    @Test
    @DisplayName("같은 사용자가 같은 상품 체크아웃에 재진입하면 Redis 캐시의 응답 값을 반환한다")
    void checkoutReturnsCachedResponse() {
        // given
        var userId = 1L;
        var productId = 10L;
        var cachedResponse = checkoutResponse("sheet-token", productId);
        given(checkoutResponseCache.get(userId, productId)).willReturn(Optional.of(cachedResponse));

        // when
        var response = orderSheetService.checkout(new CheckoutRequest(userId, productId));

        // then
        assertThat(response).isEqualTo(cachedResponse);
        verifyNoInteractions(stayProductService, inventoryService, pointService, orderSheetRepository);
    }

    @Test
    @DisplayName("오픈되지 않은 상품은 체크아웃 주문서를 생성하지 않는다")
    void checkoutRejectsNotOpenProduct() {
        // given
        var userId = 1L;
        var productId = 10L;
        given(checkoutResponseCache.get(userId, productId)).willReturn(Optional.empty());
        given(stayProductService.getOpen(productId)).willThrow(new StayProductNotOpenException());

        // when & then
        assertThatThrownBy(() -> orderSheetService.checkout(new CheckoutRequest(userId, productId)))
                .isInstanceOf(StayProductNotOpenException.class);
        verifyNoInteractions(inventoryService, pointService, orderSheetRepository);
    }

    private StayProductView stayProduct(Long productId) {
        return StayProductView.builder()
                .id(productId)
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

    private CheckoutResponse checkoutResponse(String orderSheetToken, Long productId) {
        return CheckoutResponse.builder()
                .orderSheetToken(orderSheetToken)
                .stayProduct(CheckoutResponse.StayProductDetail.from(stayProduct(productId), true))
                .user(new CheckoutResponse.UserDetail(5_000L))
                .build();
    }
}
