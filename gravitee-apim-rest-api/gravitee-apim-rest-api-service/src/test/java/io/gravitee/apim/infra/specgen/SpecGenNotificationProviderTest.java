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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import java.util.List;
import java.util.Map;
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

    @Test
    void should_pass_the_api_id_and_name_so_the_notification_is_about_the_api() {
        var specGenNotificationProvider = new SpecGenNotificationProviderImpl(portalNotificationService);
        var apiSpecGen = buildApiSpecGen();

        specGenNotificationProvider.notify(apiSpecGen, "user-id");

        verify(portalNotificationService).create(
            any(),
            eq(NEW_SPEC_GENERATED),
            eq(List.of("user-id")),
            assertArg(params -> {
                var api = (ApiModel) ((Map<?, ?>) params).get(NotificationParamsBuilder.PARAM_API);
                assertThat(api.getId()).isEqualTo(apiSpecGen.id());
                assertThat(api.getName()).isEqualTo("name");
            })
        );
    }

    private static ApiSpecGen buildApiSpecGen() {
        return new ApiSpecGen(UuidString.generateRandom(), "name", "desc", "1.0.0", ApiType.PROXY, "env");
    }
}
