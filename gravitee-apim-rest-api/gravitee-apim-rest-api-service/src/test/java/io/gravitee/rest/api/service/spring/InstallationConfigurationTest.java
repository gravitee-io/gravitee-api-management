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
package io.gravitee.rest.api.service.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = InstallationConfiguration.class)
@TestPropertySource(
    properties = {
        "installation.additionalInformation[0].name=cp-id",
        "installation.additionalInformation[0].value=efdab34c",
        "installation.additionalInformation[1].name=provider",
        "installation.additionalInformation[1].value=az",
        "installation.additionalInformation[2].name=region",
        "installation.additionalInformation[2].value=euwest",
        "installation.additionalInformation[3].name=geo",
        "installation.additionalInformation[3].value=europe",
        "installation.additionalInformation[4].name=",
        "installation.additionalInformation[4].value=null",
        "installation.api.proxyPath.management=http://localhost:8083/management",
        "installation.api.url=http://localhost:8084/api",
    }
)
public class InstallationConfigurationTest {

    @Autowired
    InstallationConfiguration installationConfiguration;

    @Test
    public void shouldParseProperties() {
        assertNotNull(installationConfiguration);
        assertEquals("http://localhost:8084/api", installationConfiguration.getApiURL());
        assertEquals("http://localhost:8083/management", installationConfiguration.getManagementProxyPath());
        assertEquals(5, installationConfiguration.getAdditionalInformation().size());
        assertEquals("euwest", installationConfiguration.getAdditionalInformation().get("REGION"));
        assertEquals("europe", installationConfiguration.getAdditionalInformation().get("GEO"));
        assertEquals("az", installationConfiguration.getAdditionalInformation().get("PROVIDER"));
    }
}
