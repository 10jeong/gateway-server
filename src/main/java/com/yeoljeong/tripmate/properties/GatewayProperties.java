package com.yeoljeong.tripmate.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

@Getter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private List<String> whitelist;

    /*
    * application.yaml에 작성된 whitelist를 가져와 list에 저장합니다.
    * */
    @ConstructorBinding
    public GatewayProperties(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
