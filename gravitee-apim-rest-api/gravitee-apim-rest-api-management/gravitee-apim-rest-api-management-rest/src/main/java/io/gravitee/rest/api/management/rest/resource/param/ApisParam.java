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
package io.gravitee.rest.api.management.rest.resource.param;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.ExecutionMode;
import io.swagger.v3.oas.annotations.Parameter;
import javax.ws.rs.QueryParam;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
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

    @Parameter(description = "filter by context path")
    @QueryParam("context-path")
    private String contextPath;

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
    @QueryParam("version")
    private String version;

    @Parameter(description = "filter by execution mode")
    @QueryParam("executionMode")
    private ExecutionMode executionMode;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isPortal() {
        return portal;
    }

    public void setPortal(boolean portal) {
        this.portal = portal;
    }

    @QueryParam("order")
    @Parameter(description = "The field used to sort results. Can be asc or desc (prefix with minus '-') ", example = "-name")
    public void setOrder(String param) {
        if (param != null) {
            order = Order.parse(param);
        }
    }

    public Order getOrder() {
        return order;
    }

    public String getCrossId() {
        return crossId;
    }

    public void setCrossId(String crossId) {
        this.crossId = crossId;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(final ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }
}
