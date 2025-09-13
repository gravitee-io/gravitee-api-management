/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.log.model;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.model.Audit;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class Log {

    public enum AuditEvent implements Audit.AuditEvent {
        LOG_READ,
    }

    private String id;
    private long timestamp;
    private String transactionId;
    private String uri;
    private HttpMethod method;
    private int status;
    private long responseTime;
    private long apiResponseTime;
    private long requestContentLength;
    private long responseContentLength;
    private String plan;
    private String api;
    private String application;
    private String localAddress;
    private String remoteAddress;
    private String endpoint;
    private String tenant;
    private String message;
    private String gateway;
    private String host;
    private String user;
    private String securityType;
    private String securityToken;
    private String errorKey;
    private String errorComponentName;
    private String errorComponentType;
    private List<LogDiagnostic> warnings;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Log log = (Log) o;

        return id.equals(log.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
