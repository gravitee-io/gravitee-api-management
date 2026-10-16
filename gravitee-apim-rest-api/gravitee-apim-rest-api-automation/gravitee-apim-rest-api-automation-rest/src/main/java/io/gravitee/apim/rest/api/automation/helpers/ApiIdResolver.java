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
package io.gravitee.apim.rest.api.automation.helpers;

import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Turns the {@code apiHrid} path segment into an API id. The permission filter resolves it once and stores the
 * id it authorized under {@link #AUTHORIZED_API_ID_PROPERTY}, so resources act on that same API by reading
 * {@link #authorizedApiId(ContainerRequestContext)} instead of parsing the request again.
 *
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiIdResolver {

    public static final String AUTHORIZED_API_ID_PROPERTY = ApiIdResolver.class.getName() + ".authorizedApiId";

    public static String resolve(ExecutionContext executionContext, String apiHrid, boolean hridContainsUUID) {
        return hridContainsUUID ? apiHrid : HRIDToUUID.api().context(executionContext).hrid(apiHrid).id();
    }

    public static String authorizedApiId(ContainerRequestContext requestContext) {
        return Objects.requireNonNull(
            (String) requestContext.getProperty(AUTHORIZED_API_ID_PROPERTY),
            "No API id was authorized for this request: the resource method needs an API-scoped @Permission"
        );
    }
}
