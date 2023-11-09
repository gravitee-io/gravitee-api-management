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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.GraviteeLicense;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import java.util.Set;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeLicenseResourceTest extends AbstractResourceTest {

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/license";
    }

    @Test
    public void shouldReturnLicenseWithFeatures() {
        final License license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
        when(license.getTier()).thenReturn("universe");
        when(license.getPacks()).thenReturn(Set.of("observability"));
        when(license.getFeatures()).thenReturn(Set.of("apim-reporter-datadog"));

        var response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        var graviteeLicense = response.readEntity(GraviteeLicense.class);
        assertThat(graviteeLicense).isNotNull();
        assertThat(graviteeLicense.getTier()).isEqualTo("universe");
        assertThat(graviteeLicense.getPacks()).containsExactly("observability");
        assertThat(graviteeLicense.getFeatures()).containsExactly("apim-reporter-datadog");
    }
}
