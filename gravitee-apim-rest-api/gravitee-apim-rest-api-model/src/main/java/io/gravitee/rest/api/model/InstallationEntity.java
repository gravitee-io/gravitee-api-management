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
package io.gravitee.rest.api.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class InstallationEntity {

    /**
     * Auto generated id.
     * This id is generated at the first startup time.
     */
    private String id;

    /**
     * This is the URL to cockpit installation
     */
    private String cockpitURL;

    /**
     * Additional information about this installation.
     */
    private Map<String, String> additionalInformation = new HashMap<>();

    /**
     * Creation date.
     */
    private Date createdAt;

    /**
     * Last update date.
     */
    private Date updatedAt;

    public InstallationEntity() {}

    public InstallationEntity(InstallationEntity other) {
        this.id = other.id;
        this.cockpitURL = other.cockpitURL;
        this.additionalInformation = other.additionalInformation;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCockpitURL() {
        return cockpitURL;
    }

    public void setCockpitURL(String cockpitURL) {
        this.cockpitURL = cockpitURL;
    }

    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
