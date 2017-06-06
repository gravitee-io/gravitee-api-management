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
package io.gravitee.management.rest.resource.auth;

import org.apache.commons.lang.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum AuthenticationSource {

    GOOGLE("google"),
    GITHUB("github"),
    OAUTH2("oauth2");

    String name;

    AuthenticationSource(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String capitalize() {
        return StringUtils.capitalize(this.name);
    }
}
