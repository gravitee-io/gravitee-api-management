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
package inmemory;

import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;

public class InstallationTypeDomainServiceInMemory implements InstallationTypeDomainService {

    private InstallationType type = InstallationType.STANDALONE;

    public void set(InstallationType type) {
        this.type = type;
    }

    public void reset() {
        type = InstallationType.STANDALONE;
    }

    @Override
    public InstallationType get() {
        return type;
    }

    @Override
    public boolean isMultiTenant() {
        return type == InstallationType.MULTI_TENANT;
    }
}
