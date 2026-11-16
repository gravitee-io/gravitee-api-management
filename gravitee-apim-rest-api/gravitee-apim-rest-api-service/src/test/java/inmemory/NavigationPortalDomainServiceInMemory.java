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

import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.apim.core.portal.domain_service.NavigationPortalDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalUrlDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import java.net.URI;
import java.net.URL;
import lombok.SneakyThrows;

/**
 * The real resolver over in-memory portals, where every environment has a portal URL: an environment
 * without a portal gets its default portal on its first console navigation write.
 */
public class NavigationPortalDomainServiceInMemory extends NavigationPortalDomainService {

    public static final String PORTAL_URL = "https://portal.example.com";

    private final PortalCrudServiceInMemory portalCrudService;
    private int resolveCalls;

    public NavigationPortalDomainServiceInMemory() {
        this(new PortalCrudServiceInMemory());
    }

    public NavigationPortalDomainServiceInMemory(PortalCrudServiceInMemory portalCrudService) {
        super(
            new DefaultPortalDomainService(
                portalCrudService,
                new PortalUrlDomainService(everyEnvironmentHasAPortalUrl()),
                new PortalContentOwnershipDomainService(portalCrudService, new PortalNavigationItemsCrudServiceInMemory())
            ),
            portalCrudService
        );
        this.portalCrudService = portalCrudService;
    }

    public PortalCrudServiceInMemory portalCrudService() {
        return portalCrudService;
    }

    @Override
    public Portal resolve(String organizationId, String environmentId) {
        resolveCalls++;
        return super.resolve(organizationId, environmentId);
    }

    public int resolveCalls() {
        return resolveCalls;
    }

    /** The id of the environment's default portal, once a console write created it. */
    public PortalId defaultPortalIdOf(String environmentId) {
        return portalCrudService.findDefaultByEnvironmentId(environmentId).orElseThrow().getId();
    }

    public PortalId givenDefaultPortal(String organizationId, String environmentId) {
        return portalCrudService.create(Portal.createDefault(environmentId, organizationId, "Developer Portal", portalUrl())).getId();
    }

    public PortalId givenAutomationPortal(String organizationId, String environmentId) {
        return portalCrudService.create(Portal.create(environmentId, organizationId, "Automation Portal", portalUrl())).getId();
    }

    @SneakyThrows
    private static URL portalUrl() {
        return URI.create(PORTAL_URL + "/next").toURL();
    }

    private static InstallationAccessQueryServiceInMemory everyEnvironmentHasAPortalUrl() {
        return new InstallationAccessQueryServiceInMemory() {
            @Override
            public String getPortalUrl(String environmentId) {
                return PORTAL_URL;
            }
        };
    }
}
