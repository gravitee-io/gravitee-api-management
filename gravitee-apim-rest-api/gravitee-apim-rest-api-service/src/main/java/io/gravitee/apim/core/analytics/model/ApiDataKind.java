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
package io.gravitee.apim.core.analytics.model;

import io.gravitee.rest.api.model.permissions.RolePermission;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What a caller asks to read from the APIs of an environment, which decides the permission that opens each API.
 */
@Getter
@RequiredArgsConstructor
public enum ApiDataKind {
    /** Figures computed over many requests of one API. Opened by API_ANALYTICS, and for the whole environment by ENVIRONMENT_PLATFORM. */
    AGGREGATES(RolePermission.API_ANALYTICS, true),
    /** The requests themselves, which identify callers and carry payloads. Opened by API_LOG on each API only. */
    PER_REQUEST(RolePermission.API_LOG, false);

    private final RolePermission apiPermission;
    private final boolean openedByPlatformPermission;
}
