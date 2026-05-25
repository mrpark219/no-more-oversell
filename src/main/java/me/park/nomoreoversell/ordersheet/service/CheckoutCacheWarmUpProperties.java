package me.park.nomoreoversell.ordersheet.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "checkout.cache.warm-up")
public class CheckoutCacheWarmUpProperties {

    private boolean enabled;
    private List<Long> productIds = new ArrayList<>();
}
