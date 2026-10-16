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
package io.gravitee.apim.core.subscription.use_case;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDDomainService;
import org.junit.jupiter.api.Test;

class DeleteSubscriptionSpecUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id");
    private static final String PATH_API_ID = "path-api-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";

    private final SubscriptionCRDDomainService subscriptionCRDDomainService = mock(SubscriptionCRDDomainService.class);

    private final DeleteSubscriptionSpecUseCase cut = new DeleteSubscriptionSpecUseCase(subscriptionCRDDomainService);

    @Test
    void should_delete_on_the_path_api() {
        cut.execute(new DeleteSubscriptionSpecUseCase.Input(AUDIT_INFO, PATH_API_ID, SUBSCRIPTION_ID));

        verify(subscriptionCRDDomainService).delete(AUDIT_INFO, PATH_API_ID, SUBSCRIPTION_ID);
    }
}
