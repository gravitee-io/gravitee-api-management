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
package io.gravitee.rest.api.management.v2.rest.resource.api.metrics.param;

import io.gravitee.rest.api.management.v2.rest.model.ConnectorType;
import io.gravitee.rest.api.management.v2.rest.model.MessageOperation;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class SearchMessageMetricsParam {

    public static final String OPERATION_PARAM_NAME = "operation";
    public static final String CONNECTOR_TYPE_PARAM_NAME = "connectorType";
    public static final String CONNECTOR_ID_PARAM_NAME = "connectorId";
    public static final String REQUEST_ID_PARAM_NAME = "requestId";

    @EnumValue(MessageOperation.class)
    @QueryParam(OPERATION_PARAM_NAME)
    String operation;

    @EnumValue(ConnectorType.class)
    @QueryParam(CONNECTOR_TYPE_PARAM_NAME)
    String connectorType;

    @QueryParam(CONNECTOR_ID_PARAM_NAME)
    String connectorId;

    @QueryParam(REQUEST_ID_PARAM_NAME)
    String requestId;
}
