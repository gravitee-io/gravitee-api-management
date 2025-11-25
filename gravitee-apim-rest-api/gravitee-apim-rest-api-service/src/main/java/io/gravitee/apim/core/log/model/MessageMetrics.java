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
package io.gravitee.apim.core.log.model;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MessageMetrics {

    private String timestamp;
    private String apiId;
    private String apiName;
    private String requestId;
    private String clientIdentifier;
    private String correlationId;
    private String operation;
    private String connectorType;
    private String connectorId;
    private String gateway;
    private long contentLength;
    private long count;
    private long errorCount;
    private long countIncrement;
    private long errorCountIncrement;
    private boolean error;
    private long gatewayLatencyMs;
    private Map<String, String> custom = new HashMap<>();
    private Map<String, Object> additionalMetrics = new HashMap<>();
}
