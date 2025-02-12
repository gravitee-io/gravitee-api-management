package io.gravitee.apim.core.log.model;

import io.gravitee.common.http.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class BaseConnectionLog {
    private String apiId;
    private String requestId;
    private String timestamp;
    private String applicationId;
    private String planId;
    private String clientIdentifier;
    private String transactionId;
    private HttpMethod method;
    private int status;
    private boolean requestEnded;
    private String entrypointId;
    private String gateway;
    private String path;
    private long gatewayResponseTime;
}
