/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource.v4.param;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.rest.api.management.rest.resource.param.Order;
import io.swagger.v3.oas.annotations.Parameter;
import javax.ws.rs.QueryParam;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ApisParam {

    @Parameter(description = "filter by category id")
    @QueryParam("category")
    private String category;

    @Parameter(description = "filter by group id")
    @QueryParam("group")
    private String group;

    @Parameter(description = "true if you only want Top APIs. default: false")
    @QueryParam("top")
    private boolean top;

    @Parameter(description = "filter by label")
    @QueryParam("label")
    private String label;

    @Parameter(description = "filter by state: STARTED or STOPPED")
    @QueryParam("state")
    private String state;

    @Parameter(description = "filter by visibility: PUBLIC or PRIVATE")
    @QueryParam("visibility")
    private String visibility;

    @Parameter(description = "filter by version")
<<<<<<< HEAD
    @QueryParam("apiVersion")
    private String apiVersion;
=======
    @QueryParam("version")
    private String version;
>>>>>>> 2a8318ccf0 (feat(definition): add api definition v4 classes)

    @Parameter(description = "filter by full API Name")
    @QueryParam("name")
    private String name;

    @Parameter(description = "filter by tag")
    @QueryParam("tag")
    private String tag;

    @QueryParam("portal")
    private boolean portal;

    @JsonIgnore
    private Order order;

    @Parameter(description = "filter by crossId")
    @QueryParam("crossId")
    private String crossId;

    @QueryParam("order")
    @Parameter(description = "The field used to sort results. Can be asc or desc (prefix with minus '-') ", example = "-name")
    public void setOrder(String param) {
        if (param != null) {
            order = Order.parse(param);
        }
    }
}
