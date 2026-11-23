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
package io.gravitee.gamma.rest.resources;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.rest.api.management.rest.provider.TechnicalDomainExceptionMapper;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.CustomLog;

/**
 * Answers a {@link TechnicalDomainException} as the management API does, and logs it: the layers raising one
 * wrap the cause and rethrow without logging, so this boundary is where the failure is recorded, once.
 *
 * @author GraviteeSource Team
 */
@Provider
@CustomLog
public class GammaTechnicalDomainExceptionMapper extends TechnicalDomainExceptionMapper {

    @Override
    public Response toResponse(TechnicalDomainException exception) {
        log.error("Answering a technical failure with a 500: {}", exception.getMessage(), exception);
        return super.toResponse(exception);
    }
}
