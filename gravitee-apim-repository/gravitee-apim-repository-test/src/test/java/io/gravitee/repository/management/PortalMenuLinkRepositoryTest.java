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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.model.PortalMenuLink.PortalMenuLinkType.EXTERNAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.repository.management.model.PortalMenuLink;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PortalMenuLinkRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalmenulink-tests/";
    }

    @Test
    public void shouldFindByIdAndEnvironment() throws Exception {
        Optional<PortalMenuLink> portalLink = portalMenuLinkRepository.findByIdAndEnvironmentId("menuLink1", "environment1");

        assertTrue(portalLink.isPresent());
    }

    @Test
    public void shouldNotFindByIdAndEnvironment() throws Exception {
        Optional<PortalMenuLink> portalLink = portalMenuLinkRepository.findByIdAndEnvironmentId("menuLink1", "environment2");

        assertFalse(portalLink.isPresent());
    }

    @Test
    public void shouldFindByEnvironment() throws Exception {
        List<PortalMenuLink> portalLinks = portalMenuLinkRepository.findByEnvironmentIdSortByOrder("environment1");

        assertNotNull(portalLinks);
        assertEquals(3, portalLinks.size());

        assertEquals("menuLink1", portalLinks.get(0).getId());
        assertEquals("menuLink5", portalLinks.get(1).getId());
        assertEquals("menuLink2", portalLinks.get(2).getId());
    }

    @Test
    public void shouldFindNothingByEnvironment() throws Exception {
        List<PortalMenuLink> portalLinks = portalMenuLinkRepository.findByEnvironmentIdSortByOrder("unknownEnvironment");

        assertNotNull(portalLinks);
        assertEquals(0, portalLinks.size());
    }

    @Test
    public void shouldDeleteByEnv() throws Exception {
        portalMenuLinkRepository.deleteByEnvironmentId("environment1");

        Set<PortalMenuLink> portalLinks = portalMenuLinkRepository.findAll();
        assertNotNull(portalLinks);
        assertEquals(2, portalLinks.size());

        PortalMenuLink link3 = new PortalMenuLink();
        link3.setId("menuLink3");
        link3.setName("Menu one");
        link3.setEnvironmentId("environment2");
        link3.setType(EXTERNAL);
        link3.setTarget("https://env2.target.one");
        link3.setOrder(1);

        PortalMenuLink link4 = new PortalMenuLink();
        link4.setId("menuLink4");
        link4.setName("Menu one");
        link4.setEnvironmentId("environment3");
        link4.setType(EXTERNAL);
        link4.setTarget("https://env3.target.one");
        link4.setOrder(1);

        assertTrue(portalLinks.containsAll(Set.of(link3, link4)));
    }
}
