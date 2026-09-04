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
package io.gravitee.gamma.rest.core.observability.filter.exception;

import io.gravitee.apim.core.exception.TechnicalDomainException;

/**
 * The observability catalog advertises a filter the platform analytics catalog cannot serve: the
 * two declarations drifted apart. A build-time parity test keeps this unreachable; when it fires
 * anyway the cause is a catalog bug, not a caller mistake, so it maps to HTTP 500 rather than 400.
 *
 * @author GraviteeSource Team
 */
public class FilterCatalogDriftException extends TechnicalDomainException {

    public FilterCatalogDriftException(String filterName, Throwable cause) {
        super(
            "Filter '" + filterName + "' is advertised by the observability catalog but the platform analytics catalog cannot serve it",
            cause
        );
    }
}
