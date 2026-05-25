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
    private final StayProductCache stayProductCache;

    @Transactional(readOnly = true)
    public StayProductView get(Long stayProductId) {
        return stayProductCache.get(stayProductId)
                .orElseGet(() -> getFromRepositoryAndCache(stayProductId));
    }

    @Transactional(readOnly = true)
    public StayProductView getOpen(Long stayProductId) {
        var stayProduct = get(stayProductId);
        validateOpen(stayProductId, stayProduct);
        return stayProduct;
    }

    @Transactional(readOnly = true)
    public StayProductView getFromRepository(Long stayProductId) {
        return StayProductView.from(findById(stayProductId));
    }

    @Transactional(readOnly = true)
    public StayProductView getOpenFromRepository(Long stayProductId) {
        var stayProduct = getFromRepository(stayProductId);
        validateOpen(stayProductId, stayProduct);
        return stayProduct;
    }

    @Transactional(readOnly = true)
    public void warmUp(Long stayProductId) {
        get(stayProductId);
    }

    @Transactional(readOnly = true)
    public StayProductView getFromRepositoryAndCache(Long stayProductId) {
        var view = getFromRepository(stayProductId);
        stayProductCache.put(view);
        return view;
    }

    private void validateOpen(Long stayProductId, StayProductView stayProduct) {
        if (!stayProduct.isOpenAt(LocalDateTime.now())) {
            log.info(
                    "숙소 상품이 오픈되지 않았습니다. stayProductId={}, status={}, openAt={}",
                    stayProductId,
                    stayProduct.status(),
                    stayProduct.openAt()
            );
            throw new StayProductNotOpenException();
        }
    }

    private StayProduct findById(Long stayProductId) {
        return stayProductRepository.findById(stayProductId)
                .orElseThrow(() -> {
                    log.warn("숙소 상품을 찾을 수 없습니다. stayProductId={}", stayProductId);
                    return new StayProductNotFoundException();
                });
    }
}
