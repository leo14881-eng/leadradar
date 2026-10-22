package com.leadradar.merchant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "leadradar")
public record MerchantProperties(List<Merchant> merchants) {
    public MerchantProperties {
        merchants = merchants == null ? List.of() : List.copyOf(merchants);
    }
}
