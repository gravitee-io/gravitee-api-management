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

import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstallationAccessQueryServiceInMemory implements InstallationAccessQueryService {

    @Override
    public List<String> getConsoleUrls() {
        return List.of();
    }

    @Override
    public List<String> getConsoleUrls(final String organizationId) {
        return List.of();
    }

    @Override
    public String getConsoleUrl(final String organizationId) {
        return null;
    }

    @Override
    public String getConsoleAPIUrl(final String organizationId) {
        return null;
    }

    @Override
    public List<String> getPortalUrls() {
        return List.of();
    }

    @Override
    public List<String> getPortalUrls(final String environmentId) {
        return List.of();
    }

    @Override
    public String getPortalUrl(final String environmentId) {
        return null;
    }

    @Override
    public String getPortalAPIUrl(final String environmentId) {
        return null;
    }

    @Override
    public List<RestrictedDomain> getGatewayRestrictedDomains(final String environmentId) {
        return List.of();
    }
}
