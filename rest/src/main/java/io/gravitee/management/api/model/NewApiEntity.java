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
package io.gravitee.management.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class NewApiEntity {

    @NotNull
    private String name;

    @NotNull
    private String version;

    @NotNull
    private String description;

    @NotNull
    @JsonProperty("public")
    private URI publicURI;

    @NotNull
    @JsonProperty("target")
    private URI targetURI;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getPublicURI() {
        return publicURI;
    }

    public void setPublicURI(URI publicURI) {
        this.publicURI = publicURI;
    }

    public URI getTargetURI() {
        return targetURI;
    }

    public void setTargetURI(URI targetURI) {
        this.targetURI = targetURI;
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
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Api{");
        sb.append("name='").append(name).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", publicURI=").append(publicURI);
        sb.append(", targetURI=").append(targetURI);
        sb.append('}');
        return sb.toString();
    }
}
