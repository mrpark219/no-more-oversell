package me.park.nomoreoversell.stayproduct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.exception.StayProductNotFoundException;
import me.park.nomoreoversell.exception.StayProductNotOpenException;
import me.park.nomoreoversell.stayproduct.domain.StayProduct;
import me.park.nomoreoversell.stayproduct.repository.StayProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayProductService {

    private final StayProductRepository stayProductRepository;

    @Transactional(readOnly = true)
    public StayProductView get(Long stayProductId) {
        return StayProductView.from(findById(stayProductId));
    }

    @Transactional(readOnly = true)
    public StayProductView getOpen(Long stayProductId) {
        var stayProduct = findById(stayProductId);
        if (!stayProduct.isOpenAt(LocalDateTime.now())) {
            log.info(
                    "숙소 상품이 오픈되지 않았습니다. stayProductId={}, status={}, openAt={}",
                    stayProductId,
                    stayProduct.getStatus(),
                    stayProduct.getOpenAt()
            );
            throw new StayProductNotOpenException();
        }
        return StayProductView.from(stayProduct);
    }

    private StayProduct findById(Long stayProductId) {
        return stayProductRepository.findById(stayProductId)
                .orElseThrow(() -> {
                    log.warn("숙소 상품을 찾을 수 없습니다. stayProductId={}", stayProductId);
                    return new StayProductNotFoundException();
                });
    }
}
