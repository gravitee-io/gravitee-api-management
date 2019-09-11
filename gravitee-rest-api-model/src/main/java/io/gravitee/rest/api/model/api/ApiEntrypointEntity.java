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
package io.gravitee.rest.api.model.api;

import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiEntrypointEntity {

    private Set<String> tags;

    private final String target;

    private final String host;

    public ApiEntrypointEntity(Set<String> tags, String target, String host) {
        this.tags = tags;
        this.target = target;
        this.host = host;
    }

    public ApiEntrypointEntity(Set<String> tags, String target) {
        this(tags, target, null);
    }

    public ApiEntrypointEntity(String target, String host) {
        this(null, target, host);
    }

    public ApiEntrypointEntity(String target) {
        this(target, null);
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getTarget() {
        return target;
    }

    public String getHost() {
        return host;
    }
}
