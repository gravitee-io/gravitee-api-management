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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

abstract class AbstractPortalNavigationResourceTest extends AbstractResourceTest {

    protected static final String NAVIGATION_ENVIRONMENT = "environment-id";
    protected static final String NAVIGATION_PORTAL_ID = "11111111-1111-1111-1111-111111111111";

    @Inject
    protected PortalCrudServiceInMemory portalCrudService;

    @BeforeEach
    @SneakyThrows
    void initializeNavigationPortal() {
        var owner = (NavigationItemReference.PortalReference) PortalNavigationItemFixtures.PORTAL_REFERENCE;
        portalCrudService.initWith(
            List.of(
                Portal.of(
                    owner.portalId(),
                    NAVIGATION_ENVIRONMENT,
                    ORGANIZATION,
                    "Developer Portal",
                    PortalNavigationStructure.empty(),
                    null,
                    true,
                    URI.create("https://portal.example.com/next").toURL()
                )
            )
        );
    }

    @AfterEach
    void clearNavigationPortal() {
        portalCrudService.reset();
    }
}
