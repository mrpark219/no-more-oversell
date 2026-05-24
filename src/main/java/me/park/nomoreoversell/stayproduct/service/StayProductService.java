package me.park.nomoreoversell.stayproduct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.exception.StayProductNotFoundException;
import me.park.nomoreoversell.stayproduct.repository.StayProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayProductService {

    private final StayProductRepository stayProductRepository;

    @Transactional(readOnly = true)
    public StayProductView get(Long stayProductId) {
        return stayProductRepository.findById(stayProductId)
                .map(StayProductView::from)
                .orElseThrow(() -> {
                    log.warn("숙소 상품을 찾을 수 없습니다. stayProductId={}", stayProductId);
                    return new StayProductNotFoundException();
                });
    }
}
