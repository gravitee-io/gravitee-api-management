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
package io.gravitee.apim.infra.specgen;

import static io.gravitee.rest.api.service.notification.ApiHook.NEW_SPEC_GENERATED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.notification.ApiHook;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SpecGenNotificationProviderTest {

    @Mock
    PortalNotificationService portalNotificationService;

    @Test
    void must_notify() {
        var specGenNotificationProvider = new SpecGenNotificationProviderImpl(portalNotificationService);
        final String userId = UuidString.generateRandom();
        specGenNotificationProvider.notify(buildApiSpecGen(), userId);

        verify(portalNotificationService, times(1)).create(any(), eq(NEW_SPEC_GENERATED), eq(List.of(userId)), any());
    }

    @NotNull
    private static ApiSpecGen buildApiSpecGen() {
        return new ApiSpecGen(UuidString.generateRandom(), "name", "desc", "1.0.0", ApiType.PROXY, "env");
    }
}
