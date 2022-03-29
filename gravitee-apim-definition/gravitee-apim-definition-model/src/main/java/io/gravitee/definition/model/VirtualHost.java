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
package io.gravitee.definition.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VirtualHost implements Serializable {

    private String host;

    private String path;

    private boolean overrideEntrypoint;

    public VirtualHost() {}

    public VirtualHost(String path) {
        this.path = path;
    }

    public VirtualHost(String host, String path) {
        this.host = host;
        this.path = path;
    }

    public VirtualHost(String host, String path, boolean overrideEntrypoint) {
        this.host = host;
        this.path = path;
        this.overrideEntrypoint = overrideEntrypoint;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isOverrideEntrypoint() {
        return overrideEntrypoint;
    }

    public void setOverrideEntrypoint(boolean overrideEntrypoint) {
        this.overrideEntrypoint = overrideEntrypoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualHost that = (VirtualHost) o;
        return overrideEntrypoint == that.overrideEntrypoint && Objects.equals(host, that.host) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, path, overrideEntrypoint);
    }
}
