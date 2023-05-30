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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.Feature;
import io.gravitee.node.api.license.License;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GraviteeLicenseServiceTest {

    @Mock
    private Node node;

    @Mock
    private License license;

    @Mock
    private Feature feature;

    private GraviteeLicenseService licenseService;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        licenseService = new GraviteeLicenseServiceImpl(node);
    }

    @Test
    public void shouldReturnNullTierWithNoLicense() {
        assertThat(licenseService.getLicense().getTier()).isNull();
    }

    @Test
    public void shouldReturnEmptyPacksWithNoLicense() {
        assertThat(licenseService.getLicense().getPacks()).isEmpty();
    }

    @Test
    public void shouldReturnEmptyFeaturesWithNoLicense() {
        assertThat(licenseService.getLicense().getFeatures()).isEmpty();
    }

    @Test
    public void shouldReturnTierFromLicense() {
        when(feature.getString()).thenReturn("tier-planet");
        when(license.feature("tier")).thenReturn(Optional.of(feature));
        when(node.license()).thenReturn(license);
        assertThat(licenseService.getLicense().getTier()).isEqualTo("tier-planet");
    }

    @Test
    public void shouldReturnPacksFromLicense() {
        when(feature.getString()).thenReturn("pack-observability,pack-event-native");
        when(license.feature("packs")).thenReturn(Optional.of(feature));
        when(node.license()).thenReturn(license);
        assertThat(licenseService.getLicense().getPacks()).containsExactlyInAnyOrder("pack-observability", "pack-event-native");
    }

    @Test
    public void shouldReturnFeaturesFromLicense() {
        when(license.features()).thenReturn(Map.of("feature-debug-mode", "included", "feature-datadog-reporter", "included"));
        when(node.license()).thenReturn(license);
        assertThat(licenseService.getLicense().getFeatures()).containsExactlyInAnyOrder("feature-debug-mode", "feature-datadog-reporter");
    }
}
