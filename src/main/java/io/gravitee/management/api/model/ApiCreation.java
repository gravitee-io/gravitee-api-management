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

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiCreation {

    private String name;
    private String description;
    private String version;

    private URI publicURI;
    private URI targetURI;

    private boolean isPrivate;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

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
}
