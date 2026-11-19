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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import jakarta.ws.rs.BadRequestException;
import java.util.function.Function;

/** Parses the ids a Portal API request names; a malformed id is the caller's mistake, answered with 400 before any lookup. */
final class IdParams {

    private IdParams() {}

    static PortalId portalId(String name, String value) {
        return parse(name, value, PortalId::of);
    }

    static PortalNavigationItemId navigationItemId(String name, String value) {
        return parse(name, value, PortalNavigationItemId::of);
    }

    static PortalCategoryId categoryId(String name, String value) {
        return parse(name, value, PortalCategoryId::of);
    }

    private static <T> T parse(String name, String value, Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("[" + name + "] is not a valid id", e);
        }
    }
}
