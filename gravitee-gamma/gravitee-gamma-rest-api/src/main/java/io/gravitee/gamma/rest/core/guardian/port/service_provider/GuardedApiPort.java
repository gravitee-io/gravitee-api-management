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
package io.gravitee.gamma.rest.core.guardian.port.service_provider;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import java.util.List;
import java.util.Optional;

/**
 * The APIs Guardians are enabled on. The APIM API store is an external service from this domain's point of view.
 *
 * <p>Every read is scoped by environment, so an API of another environment answers as an unknown one.
 *
 * @author GraviteeSource Team
 */
public interface GuardedApiPort {
    Optional<GuardedApi> findById(String environmentId, String apiId);

    /** The APIs of the environment whose current definition runs this Guardian, deployed or not. */
    List<ApiReference> findByGuardian(String environmentId, String guardianId);

    /** Replaces the internal flow of an API that has one; the API needs a redeployment to run it. */
    GuardedApi updateInternalFlow(String apiId, Flow internalFlow);
}
