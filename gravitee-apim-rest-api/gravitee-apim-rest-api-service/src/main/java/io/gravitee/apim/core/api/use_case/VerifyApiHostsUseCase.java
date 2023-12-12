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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VerifyApiHostsUseCase {

    private final VerifyApiHostsDomainService verifyApiHostsDomainService;

    public Output execute(Input input) {
        if (verifyApiHostsDomainService.checkApiHosts(input.environmentId(), input.apiId(), input.hosts())) {
            return new Output(input.hosts());
        }
        return null;
    }

    public record Input(String environmentId, String apiId, List<String> hosts) {}

    public record Output(List<String> hosts) {}
}
