package me.park.nomoreoversell.stayproduct.service;

import me.park.nomoreoversell.exception.StayProductNotFoundException;
import me.park.nomoreoversell.exception.StayProductNotOpenException;
import me.park.nomoreoversell.stayproduct.domain.StayProduct;
import me.park.nomoreoversell.stayproduct.domain.StayProductStatus;
import me.park.nomoreoversell.stayproduct.repository.StayProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StayProductServiceTest {

    @Mock
    private StayProductRepository stayProductRepository;

    @Mock
    private StayProductCache stayProductCache;

    @InjectMocks
    private StayProductService stayProductService;

    @Test
    @DisplayName("숙소 상품이 존재하면 조회 결과를 반환한다")
    void getViewReturnsProductViewWhenProductExists() {
        // given
        var productId = 1L;
        given(stayProductCache.get(productId))
                .willReturn(Optional.empty());
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct()));

        // when
        var result = stayProductService.getView(productId);

        // then
        assertThat(result.accommodationName()).isEqualTo("테스트 호텔");
        assertThat(result.roomName()).isEqualTo("디럭스");
        assertThat(result.ratePlanName()).isEqualTo("특가");
        assertThat(result.originalPrice()).isEqualTo(20_000L);
        assertThat(result.salePrice()).isEqualTo(10_000L);
        assertThat(result.openAt()).isNotNull();
        assertThat(result.status()).isEqualTo(StayProductStatus.OPEN);
        verify(stayProductCache).put(result);
    }

    @Test
    @DisplayName("숙소 상품 캐시가 있으면 DB를 조회하지 않고 캐시 값을 반환한다")
    void getViewReturnsCachedProductViewWithoutRepositoryLookup() {
        // given
        var productId = 1L;
        var cachedProduct = StayProductView.from(stayProduct());
        given(stayProductCache.get(productId))
                .willReturn(Optional.of(cachedProduct));

        // when
        var result = stayProductService.getView(productId);

        // then
        assertThat(result).isEqualTo(cachedProduct);
        verifyNoInteractions(stayProductRepository);
    }

    @Test
    @DisplayName("숙소 상품을 DB에서 직접 조회하면 캐시를 사용하지 않는다")
    void getViewWithoutCacheReturnsProductViewWithoutCacheLookup() {
        // given
        var productId = 1L;
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct()));

        // when
        var result = stayProductService.getViewWithoutCache(productId);

        // then
        assertThat(result.accommodationName()).isEqualTo("테스트 호텔");
        assertThat(result.status()).isEqualTo(StayProductStatus.OPEN);
        verifyNoInteractions(stayProductCache);
    }

    @Test
    @DisplayName("숙소 상품이 없으면 예외를 던진다")
    void getViewThrowsExceptionWhenProductDoesNotExist() {
        // given
        var productId = 1L;
        given(stayProductCache.get(productId))
                .willReturn(Optional.empty());
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> stayProductService.getView(productId));

        // then
        assertThat(thrown)
                .isInstanceOf(StayProductNotFoundException.class)
                .hasMessage("존재하지 않는 숙소 상품입니다.");
    }

    @Test
    @DisplayName("오픈된 숙소 상품이면 주문 가능 상품 조회 결과를 반환한다")
    void getOpenViewReturnsProductViewWhenProductIsOpen() {
        // given
        var productId = 1L;
        given(stayProductCache.get(productId))
                .willReturn(Optional.empty());
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct()));

        // when
        var result = stayProductService.getOpenView(productId);

        // then
        assertThat(result.accommodationName()).isEqualTo("테스트 호텔");
        assertThat(result.status()).isEqualTo(StayProductStatus.OPEN);
    }

    @Test
    @DisplayName("오픈된 숙소 상품을 DB에서 직접 조회하면 주문 가능 상품 조회 결과를 반환한다")
    void getOpenViewWithoutCacheReturnsProductViewWhenProductIsOpen() {
        // given
        var productId = 1L;
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct()));

        // when
        var result = stayProductService.getOpenViewWithoutCache(productId);

        // then
        assertThat(result.accommodationName()).isEqualTo("테스트 호텔");
        assertThat(result.status()).isEqualTo(StayProductStatus.OPEN);
        verifyNoInteractions(stayProductCache);
    }

    @Test
    @DisplayName("숙소 상품이 아직 오픈되지 않았으면 예외를 던진다")
    void getOpenViewThrowsExceptionWhenProductIsNotOpenYet() {
        // given
        var productId = 1L;
        given(stayProductCache.get(productId))
                .willReturn(Optional.empty());
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct(LocalDateTime.now().plusDays(1), StayProductStatus.OPEN)));

        // when
        var thrown = catchThrowable(() -> stayProductService.getOpenView(productId));

        // then
        assertThat(thrown)
                .isInstanceOf(StayProductNotOpenException.class)
                .hasMessage("아직 오픈되지 않은 숙소 상품입니다.");
    }

    @Test
    @DisplayName("숙소 상품이 닫혀 있으면 예외를 던진다")
    void getOpenViewThrowsExceptionWhenProductIsClosed() {
        // given
        var productId = 1L;
        given(stayProductCache.get(productId))
                .willReturn(Optional.empty());
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct(LocalDateTime.now().minusDays(1), StayProductStatus.CLOSED)));

        // when
        var thrown = catchThrowable(() -> stayProductService.getOpenView(productId));

        // then
        assertThat(thrown)
                .isInstanceOf(StayProductNotOpenException.class)
                .hasMessage("아직 오픈되지 않은 숙소 상품입니다.");
    }

    private StayProduct stayProduct() {
        return stayProduct(LocalDateTime.now().minusDays(1), StayProductStatus.OPEN);
    }

    private StayProduct stayProduct(LocalDateTime openAt, StayProductStatus status) {
        return StayProduct.builder()
                .accommodationName("테스트 호텔")
                .roomName("디럭스")
                .ratePlanName("특가")
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .openAt(openAt)
                .status(status)
                .checkinTime(LocalTime.of(15, 0))
                .checkoutTime(LocalTime.of(11, 0))
                .maxPerUser(1L)
                .build();
    }
}
