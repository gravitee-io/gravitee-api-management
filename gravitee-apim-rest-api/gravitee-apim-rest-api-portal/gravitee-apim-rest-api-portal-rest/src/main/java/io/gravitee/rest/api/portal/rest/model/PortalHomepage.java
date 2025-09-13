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
package io.gravitee.rest.api.portal.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import lombok.Getter;

/**
 * Simple response payload for Portal Homepage endpoint.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortalHomepage {

    private String id;
    private String name;
    private Date createdAt;
    private Date updatedAt;
    private String content;
    private String type;

    public PortalHomepage setId(String id) {
        this.id = id;
        return this;
    }

    public PortalHomepage setName(String name) {
        this.name = name;
        return this;
    }

    public PortalHomepage setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public PortalHomepage setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public PortalHomepage setContent(String content) {
        this.content = content;
        return this;
    }

    public PortalHomepage setType(String type) {
        this.type = type;
        return this;
    }
}
