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
package io.gravitee.rest.api.management.rest.resource.param;

import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.rest.param.SortableFields;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.QueryParam;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketsParam {

    private static final SortableFields SORTABLE_FIELDS = new SortableFields("created_at", "api", "application", "subject");

    @QueryParam("apiId")
    @Parameter(description = "The api identifier used to filter tickets")
    private String api;

    @QueryParam("applicationId")
    @Parameter(description = "The application identifier used to filter tickets")
    private String application;

    @QueryParam("order")
    @Parameter(
        description = "The field used to sort results. Can be asc or desc (prefix with minus '-') ",
        example = "-subject",
        schema = @Schema(
            allowableValues = { "created_at", "-created_at", "api", "-api", "application", "-application", "subject", "-subject" }
        )
    )
    private String order;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Sortable getOrder() {
        return SORTABLE_FIELDS.toSortable(order);
    }
}
