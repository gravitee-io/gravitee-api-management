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
package io.gravitee.gamma.module.platform.core.am.port.service_provider;

import io.gravitee.gamma.module.platform.core.am.model.AmModels.Domain;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.Environment;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.GatewayEntrypoint;
import java.util.List;

public interface AmDirectoryClient {
    List<Environment> listEnvironments(String orgId);

    List<Domain> listDomains(String orgId, String envId, String q);

    Domain getDomain(String orgId, String envId, String domainId);

    List<GatewayEntrypoint> listDomainEntrypoints(String orgId, String envId, String domainId);
}
