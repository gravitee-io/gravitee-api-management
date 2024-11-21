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
package io.gravitee.apim.core.notification.model.hook;

import io.gravitee.rest.api.service.notification.ApiHook;
import java.util.Map;

public class SubscriptionAcceptedApiHookContext extends ApiHookContext {

    private final String applicationId;
    private final String planId;
    private final String subscriptionId;
    private final String applicationPrimaryOwner;

    public SubscriptionAcceptedApiHookContext(
        String apiId,
        String applicationId,
        String planId,
        String subscriptionId,
        String applicationPrimaryOwner
    ) {
        super(ApiHook.SUBSCRIPTION_ACCEPTED, apiId);
        this.applicationId = applicationId;
        this.planId = planId;
        this.subscriptionId = subscriptionId;
        this.applicationPrimaryOwner = applicationPrimaryOwner;
    }

    @Override
    protected Map<HookContextEntry, String> getChildProperties() {
        return Map.of(
            HookContextEntry.APPLICATION_ID,
            applicationId,
            HookContextEntry.PLAN_ID,
            planId,
            HookContextEntry.SUBSCRIPTION_ID,
            subscriptionId,
            HookContextEntry.OWNER,
            applicationPrimaryOwner
        );
    }
}
