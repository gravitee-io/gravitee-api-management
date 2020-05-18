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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.PortalConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.impl.ConfigServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.gravitee.rest.api.model.parameters.Key.COMPANY_NAME;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceTest {

    @InjectMocks
    private ConfigServiceImpl configService = new ConfigServiceImpl();

    @Mock
    private ParameterService mockParameterService;
    @Mock
    private ReCaptchaService reCaptchaService;

    @Mock
    private ConfigurableEnvironment environment;
    @Mock
    private NewsletterService newsletterService;

    @Test
    public void shouldGetPortalConfig() {

        Map<String, List<String>> params = new HashMap<>();
        params.put(COMPANY_NAME.key(), singletonList("ACME"));
        params.put(Key.AUTHENTICATION_FORCELOGIN_ENABLED.key(), singletonList("true"));
        params.put(Key.AUTHENTICATION_OAUTH2_SCOPE.key(), Arrays.asList("scope1", "scope2"));
        params.put(Key.SCHEDULER_NOTIFICATIONS.key(), singletonList("11"));
        params.put(Key.PORTAL_ANALYTICS_ENABLED.key(), singletonList("true"));
        when(mockParameterService.findAll(any(List.class))).thenReturn(params);
        when(reCaptchaService.getSiteKey()).thenReturn("my-site-key");
        when(reCaptchaService.isEnabled()).thenReturn(true);

        PortalConfigEntity portalConfig = configService.getPortalConfig();

        assertNotNull(portalConfig);
        assertEquals("company name", "ACME", portalConfig.getCompany().getName());
        assertEquals("force login", true, portalConfig.getAuthentication().getForceLogin().isEnabled());
        assertEquals("scopes", 2, portalConfig.getAuthentication().getOauth2().getScope().size());
        assertEquals("scheduler notifications", Integer.valueOf(11), portalConfig.getScheduler().getNotificationsInSeconds());
        assertEquals("analytics", Boolean.TRUE, portalConfig.getPortal().getAnalytics().isEnabled());
        assertEquals("recaptcha siteKey", "my-site-key", portalConfig.getReCaptcha().getSiteKey());
        assertEquals("recaptcha enabled", Boolean.TRUE, portalConfig.getReCaptcha().getEnabled());
    }

    @Test
    public void shouldCreatePortalConfig() {
        PortalConfigEntity portalConfigEntity = new PortalConfigEntity();
        portalConfigEntity.getCompany().setName("ACME");
        when(mockParameterService.save(COMPANY_NAME, "ACME")).thenReturn(new Parameter());

        configService.save(portalConfigEntity);

        verify(mockParameterService, times(1)).save(COMPANY_NAME, "ACME");
    }
}
