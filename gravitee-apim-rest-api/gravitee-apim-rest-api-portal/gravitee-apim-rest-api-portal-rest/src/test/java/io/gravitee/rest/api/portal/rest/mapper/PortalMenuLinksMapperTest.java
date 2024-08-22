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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import fixtures.core.model.PortalMenuLinkFixtures;
import io.gravitee.rest.api.portal.rest.model.PortalMenuLink;
import java.util.List;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PortalMenuLinksMapperTest {

    @Test
    public void testConvert() {
        PortalMenuLinkMapper portalMenuLinkMapper = new PortalMenuLinkMapper();

        var input = List.of(
            PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id1").build(),
            PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id2").build(),
            PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id3").build()
        );

        var results = portalMenuLinkMapper.map(input);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("id1", results.get(0).getId());
        assertEquals("portalMenuLinkName", results.get(0).getName());
        assertEquals("portalMenuLinkTarget", results.get(0).getTarget());
        assertEquals(PortalMenuLink.TypeEnum.EXTERNAL, results.get(0).getType());
        assertEquals(Integer.valueOf(1), results.get(0).getOrder());
    }
}
