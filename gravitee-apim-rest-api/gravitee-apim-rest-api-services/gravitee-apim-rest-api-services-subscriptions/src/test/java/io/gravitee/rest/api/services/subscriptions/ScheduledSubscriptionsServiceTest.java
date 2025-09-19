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
package io.gravitee.rest.api.services.subscriptions;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.subscription.use_case.CloseExpiredSubscriptionsUseCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledSubscriptionsServiceTest {

    @InjectMocks
    ScheduledSubscriptionsService service = new ScheduledSubscriptionsService();

    @Mock
    CloseExpiredSubscriptionsUseCase closeExpiredSubscriptionsUsecase;

    @Test
    public void shouldCloseOutdatedSubscriptions() {
        service.run();

        verify(closeExpiredSubscriptionsUsecase, times(1)).execute(
            new CloseExpiredSubscriptionsUseCase.Input(AuditActor.builder().userId("system").build())
        );
    }
}
