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
package io.gravitee.apim.infra.domain_service.theme;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ThemePortalNextAssetsDomainServiceInMemory;
import io.gravitee.apim.core.theme.domain_service.ThemePortalNextAssetsDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ThemePortalNextAssetsDomainServiceImplTest {

    ThemePortalNextAssetsDomainService service;

    @BeforeEach
    void setUp() {
        service = new ThemePortalNextAssetsDomainServiceInMemory();
    }

    @Test
    void should_get_default_logo() {
        var result = service.getPortalNextLogo();
        assertThat(result).isEqualTo("logo.png");
    }

    @Test
    void should_get_default_favicon() {
        var result = service.getPortalNextFavicon();
        assertThat(result).isEqualTo("favicon.png");
    }
}
