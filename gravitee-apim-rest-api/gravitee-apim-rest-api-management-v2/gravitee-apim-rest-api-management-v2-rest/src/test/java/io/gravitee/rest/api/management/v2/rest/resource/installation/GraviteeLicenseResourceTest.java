/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.NodeLicenseService;
import io.gravitee.rest.api.management.v2.rest.model.GraviteeLicense;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import java.util.Set;
import javax.inject.Inject;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeLicenseResourceTest extends AbstractResourceTest {

    @Inject
    NodeLicenseService nodeLicenseService;

    @Override
    protected String contextPath() {
        return "/license";
    }

    @Test
    public void shouldReturnLicenseWithFeatures() {
        when(nodeLicenseService.getTier()).thenReturn("universe");
        when(nodeLicenseService.getPacks()).thenReturn(Set.of("observability"));
        when(nodeLicenseService.getFeatures()).thenReturn(Set.of("apim-reporter-datadog"));

        var response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        var license = response.readEntity(GraviteeLicense.class);
        assertThat(license).isNotNull();
    }
}
