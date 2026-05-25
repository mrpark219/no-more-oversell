package me.park.nomoreoversell.ordersheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.inventory.service.InventoryService;
import me.park.nomoreoversell.ordersheet.domain.OrderSheet;
import me.park.nomoreoversell.ordersheet.repository.OrderSheetRepository;
import me.park.nomoreoversell.point.service.PointService;
import me.park.nomoreoversell.stayproduct.service.StayProductService;
import me.park.nomoreoversell.stayproduct.service.StayProductView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSheetService {

    private final OrderSheetRepository orderSheetRepository;
    private final InventoryService inventoryService;
    private final StayProductService stayProductService;
    private final PointService pointService;
    private final CheckoutResponseCache checkoutResponseCache;

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        return checkoutResponseCache.get(request.userId(), request.stayProductId())
                .orElseGet(() -> createCheckoutResponse(request));
    }

    private CheckoutResponse createCheckoutResponse(CheckoutRequest request) {
        var stayProduct = stayProductService.getOpen(request.stayProductId());
        var hasStock = inventoryService.hasStock(request.stayProductId());
        var availablePoint = pointService.available(request.userId());
        var orderSheet = createOrderSheet(request, stayProduct);

        var response = CheckoutResponse.builder()
                .orderSheetToken(orderSheet.getOrderSheetToken())
                .stayProduct(CheckoutResponse.StayProductDetail.from(stayProduct, hasStock))
                .user(new CheckoutResponse.UserDetail(availablePoint))
                .build();
        checkoutResponseCache.put(request.userId(), request.stayProductId(), response);
        return response;
    }

    private OrderSheet createOrderSheet(CheckoutRequest request, StayProductView stayProduct) {
        var orderSheet = orderSheetRepository.save(OrderSheet.create(
                UUID.randomUUID().toString(),
                request.userId(),
                stayProduct.id(),
                stayProduct.originalPrice(),
                stayProduct.salePrice()
        ));

        log.info(
                "주문서 생성 완료. userId={}, stayProductId={}, orderSheetToken={}",
                request.userId(),
                request.stayProductId(),
                orderSheet.getOrderSheetToken()
        );
        return orderSheet;
    }
}
