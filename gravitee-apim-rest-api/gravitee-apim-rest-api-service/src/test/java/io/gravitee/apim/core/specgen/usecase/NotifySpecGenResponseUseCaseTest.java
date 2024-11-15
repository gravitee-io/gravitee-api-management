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
package io.gravitee.apim.core.specgen.usecase;

import static assertions.CoreAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import inmemory.ApiSpecGenQueryServiceInMemory;
import inmemory.SpecGenNotificationProviderInMemory;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.use_case.NotifySpecGenResponseUseCase;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
class NotifySpecGenResponseUseCaseTest {

    private static final String CURRENT_ENV = "current-env";
    private static final String API_ID = UuidString.generateRandom();
    private static final String USER_ID = UuidString.generateRandom();

    private SpecGenNotificationProviderInMemory provider;
    private ApiSpecGenQueryServiceInMemory queryService;
    private NotifySpecGenResponseUseCase useCase;

    @BeforeEach
    void setUp() {
        queryService = new ApiSpecGenQueryServiceInMemory();
        queryService.initWith(List.of(new ApiSpecGen(API_ID, "api-3", "some description", "some-version", ApiType.PROXY, CURRENT_ENV)));

        provider = new SpecGenNotificationProviderInMemory();
        provider.initWith(List.of());

        useCase = new NotifySpecGenResponseUseCase(queryService, provider);
        GraviteeContext.setCurrentEnvironment(CURRENT_ENV);
    }

    @Test
    void must_notify_user() {
        useCase.notify(API_ID, USER_ID);

        await().atMost(2, TimeUnit.SECONDS).until(() -> !provider.storage().isEmpty());
        assertThat(provider.storage().get(0).getValue()).isEqualTo(1);
    }

    @Test
    void must_not_notify_user_with_non_existing_api() {
        useCase.notify(UuidString.generateRandom(), USER_ID);

        await().atMost(2, TimeUnit.SECONDS).until(() -> provider.storage().isEmpty());
    }

    @Test
    void must_not_notify_user_with_null_user() {
        useCase.notify(UuidString.generateRandom(), null);

        await().atMost(2, TimeUnit.SECONDS).until(() -> provider.storage().isEmpty());
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }
}
