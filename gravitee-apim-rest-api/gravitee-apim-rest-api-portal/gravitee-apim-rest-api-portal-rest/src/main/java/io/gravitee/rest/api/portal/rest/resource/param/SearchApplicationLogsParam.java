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
package io.gravitee.rest.api.portal.rest.resource.param;

import io.gravitee.rest.api.portal.rest.model.HttpMethod;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchApplicationLogsParam {

    @NotNull
    @PositiveOrZero
    @Parameter(name = "from", description = "Timestamp used to define the start date of the time window to query")
    private long from;

    @NotNull
    @PositiveOrZero
    @Parameter(name = "to", description = "Timestamp used to define the end date of the time window to query")
    private long to;

    @Parameter(name = "planIds", description = "Filter by plan IDs")
    private Set<String> planIds;

    @Parameter(name = "methods", description = "Filter by HTTP methods")
    private Set<HttpMethod> methods;

    @Parameter(name = "statuses", description = "Filter by HTTP response statuses")
    private Set<
        @Min(value = 100, message = "'statuses' must contain values greater than or equal to 100") @Max(
            value = 599,
            message = "'statuses' must contain values lesser than or equal to 599"
        ) Integer
    > statuses;

    @Parameter(name = "apiIds", description = "Filter by API IDs")
    private Set<String> apiIds;

    @Parameter(name = "requestIds", description = "Filter by request IDs")
    private Set<String> requestIds;

    @Parameter(name = "transactionIds", description = "Filter by transaction IDs")
    private Set<String> transactionIds;

    @Parameter(name = "path", description = "Filter by API path")
    private String path;

    @Parameter(name = "responseTimeRanges", description = "Filter by ranges of request response times")
    private List<ResponseTimeRange> responseTimeRanges;

    @Parameter(name = "bodyText", description = "Filter by text in request or response body")
    private String bodyText;

    public void validate() {
        if (from >= to) {
            throw new BadRequestException("'from' query parameter value must not be greater than 'to'");
        }
    }
}
