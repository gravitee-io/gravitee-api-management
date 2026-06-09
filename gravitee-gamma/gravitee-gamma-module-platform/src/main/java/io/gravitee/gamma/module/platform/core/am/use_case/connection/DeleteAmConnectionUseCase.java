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
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteAmConnectionUseCase {

    private final AmConnectionRepository amConnectionRepository;

    public record Input(String orgId) {}

    public record Output() {}

    public Output execute(Input input) {
        amConnectionRepository.deleteByOrg(input.orgId());
        return new Output();
    }
}
