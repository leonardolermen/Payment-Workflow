package com.payflow.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component("apiGatewayProperties")
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<String> publicPaths = new ArrayList<>();
}
