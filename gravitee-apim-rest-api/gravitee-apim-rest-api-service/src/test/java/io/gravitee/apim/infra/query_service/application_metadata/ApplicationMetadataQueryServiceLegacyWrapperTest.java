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
package io.gravitee.apim.infra.query_service.application_metadata;

import static org.mockito.Mockito.verify;

import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.common.UuidString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApplicationMetadataQueryServiceLegacyWrapperTest {

    @Mock
    ApplicationMetadataService applicationMetadataService;

    ApplicationMetadataQueryServiceLegacyWrapper service;
    private static final String APP_ID = UuidString.generateRandom();

    @BeforeEach
    void setUp() {
        service = new ApplicationMetadataQueryServiceLegacyWrapper(applicationMetadataService);
    }

    @Test
    void should_call_legacy_service_to_find_all_applications() {
        service.findAllByApplication(APP_ID);

        verify(applicationMetadataService).findAllByApplication(APP_ID);
    }
}
