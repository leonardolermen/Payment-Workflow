package com.payflow.apigateway.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component("apiGatewayProperties")
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<String> publicPaths = new ArrayList<>();
}
