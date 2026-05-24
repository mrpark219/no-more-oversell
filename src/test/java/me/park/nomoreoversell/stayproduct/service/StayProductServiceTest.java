package me.park.nomoreoversell.stayproduct.service;

import me.park.nomoreoversell.exception.StayProductNotFoundException;
import me.park.nomoreoversell.stayproduct.doamin.StayProduct;
import me.park.nomoreoversell.stayproduct.doamin.StayProductStatus;
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

@ExtendWith(MockitoExtension.class)
class StayProductServiceTest {

    @Mock
    private StayProductRepository stayProductRepository;

    @InjectMocks
    private StayProductService stayProductService;

    @Test
    @DisplayName("숙소 상품이 존재하면 조회 결과를 반환한다")
    void getReturnsProductViewWhenProductExists() {
        // given
        var productId = 1L;
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.of(stayProduct()));

        // when
        var result = stayProductService.get(productId);

        // then
        assertThat(result.accommodationName()).isEqualTo("테스트 호텔");
        assertThat(result.roomName()).isEqualTo("디럭스");
        assertThat(result.ratePlanName()).isEqualTo("특가");
        assertThat(result.originalPrice()).isEqualTo(20_000L);
        assertThat(result.salePrice()).isEqualTo(10_000L);
        assertThat(result.openAt()).isNotNull();
        assertThat(result.status()).isEqualTo(StayProductStatus.OPEN);
    }

    @Test
    @DisplayName("숙소 상품이 없으면 예외를 던진다")
    void getThrowsExceptionWhenProductDoesNotExist() {
        // given
        var productId = 1L;
        given(stayProductRepository.findById(productId))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> stayProductService.get(productId));

        // then
        assertThat(thrown)
                .isInstanceOf(StayProductNotFoundException.class)
                .hasMessage("존재하지 않는 숙소 상품입니다.");
    }

    private StayProduct stayProduct() {
        return StayProduct.builder()
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
