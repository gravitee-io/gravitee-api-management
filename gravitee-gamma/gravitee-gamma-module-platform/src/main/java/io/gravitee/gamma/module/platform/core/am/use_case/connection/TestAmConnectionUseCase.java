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
package io.gravitee.gamma.module.platform.core.am.use_case.connection;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.AmConnectionTestResult;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmConnectionTester;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// Merges partial inbound fields with the stored connection so the UI can test a new baseUrl
// while reusing the saved token.
@UseCase
@RequiredArgsConstructor
public class TestAmConnectionUseCase {

    private final AmConnectionRepository amConnectionRepository;
    private final AmConnectionTester amConnectionTester;

    public record Input(String orgId, String inboundBaseUrl, String inboundAccessToken) {}

    public record Output(AmConnectionTestResult result) {}

    public Output execute(Input input) {
        Optional<AmConnection> stored = amConnectionRepository.findByOrg(input.orgId());
        String baseUrl = (input.inboundBaseUrl() != null && !input.inboundBaseUrl().isBlank())
            ? input.inboundBaseUrl().replaceAll("/+$", "")
            : stored.map(AmConnection::baseUrl).orElse("");
        String accessToken = input.inboundAccessToken() != null
            ? (input.inboundAccessToken().isEmpty() ? null : input.inboundAccessToken())
            : stored.map(AmConnection::serviceAccountAccessToken).orElse(null);

        String environmentId = stored.map(AmConnection::environmentId).orElse(null);
        String defaultDomainId = stored.map(AmConnection::defaultDomainId).orElse(null);
        String defaultDomainHrid = stored.map(AmConnection::defaultDomainHrid).orElse(null);
        String gatewayUrl = stored.map(AmConnection::gatewayUrl).orElse(null);
        return new Output(
            amConnectionTester.test(
                input.orgId(),
                new AmConnection(baseUrl, accessToken, environmentId, defaultDomainId, defaultDomainHrid, gatewayUrl)
            )
        );
    }
}
