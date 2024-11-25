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

import io.gravitee.rest.api.service.notification.ApplicationHook;
import java.util.Map;

public class SubscriptionRejectedApplicationHookContext extends ApplicationHookContext {

    private final String apiId;
    private final String planId;
    private final String subscriptionId;
    private final String applicationPrimaryOwner;

    public SubscriptionRejectedApplicationHookContext(
        String applicationId,
        String apiId,
        String planId,
        String subscriptionId,
        String applicationPrimaryOwner
    ) {
        super(ApplicationHook.SUBSCRIPTION_REJECTED, applicationId);
        this.apiId = apiId;
        this.planId = planId;
        this.subscriptionId = subscriptionId;
        this.applicationPrimaryOwner = applicationPrimaryOwner;
    }

    @Override
    protected Map<HookContextEntry, String> getChildProperties() {
        return Map.of(
            HookContextEntry.API_ID,
            apiId,
            HookContextEntry.PLAN_ID,
            planId,
            HookContextEntry.SUBSCRIPTION_ID,
            subscriptionId,
            HookContextEntry.OWNER,
            applicationPrimaryOwner
        );
    }
}
