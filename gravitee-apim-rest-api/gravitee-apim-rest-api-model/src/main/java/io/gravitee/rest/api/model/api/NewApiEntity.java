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
package io.gravitee.rest.api.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewApiEntity {

    @NotNull
    @NotEmpty(message = "Api's name must not be empty")
    @Schema(description = "Api's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @NotNull
    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String version;

    @NotNull
    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @NotNull
    @Schema(description = "API's context path.", example = "/my-awesome-api")
    private String contextPath;

    @NotNull
    @Schema(description = "API's first endpoint (target url).", example = "https://local-dc:8081/api")
    private String endpoint;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @Schema(description = "API's paths. A json representation of the design of each path.")
    private List<String> paths;

    @Schema(description = "API's paths. A json representation of the design of each flow.")
    private List<Flow> flows;

    @JsonProperty(value = "flow_mode")
    @Schema(description = "API's flow mode.", example = "BEST_MATCH")
    private FlowMode flowMode;

    @JsonProperty(value = "gravitee")
    @Schema(description = "API's gravitee definition version")
    private String graviteeDefinitionVersion;

    @Schema(description = "API's visibility. Can be PUBLIC or PRIVATE.", example = "PUBLIC", allowableValues = { "PUBLIC", "PRIVATE" })
    private Visibility visibility;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = HtmlSanitizer.sanitize(name);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = HtmlSanitizer.sanitize(description);
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public FlowMode getFlowMode() {
        return flowMode;
    }

    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }

    public String getGraviteeDefinitionVersion() {
        return graviteeDefinitionVersion;
    }

    public void setGraviteeDefinitionVersion(String graviteeDefinitionVersion) {
        this.graviteeDefinitionVersion = graviteeDefinitionVersion;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public String toString() {
        return (
            "NewApiEntity{" +
            "name='" +
            name +
            '\'' +
            ", version='" +
            version +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", contextPath='" +
            contextPath +
            '\'' +
            ", endpoint='" +
            endpoint +
            '\'' +
            ", groups='" +
            groups +
            '\'' +
            ", flowMode='" +
            flowMode +
            '\'' +
            ", visibility='" +
            visibility +
            '\'' +
            '}'
        );
    }
}
