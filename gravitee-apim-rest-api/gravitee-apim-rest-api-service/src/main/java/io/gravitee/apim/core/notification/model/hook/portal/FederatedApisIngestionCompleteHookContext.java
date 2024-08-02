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
package io.gravitee.apim.core.notification.model.hook.portal;

import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Context of the hook called when the ingestion of federated APIs is complete.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FederatedApisIngestionCompleteHookContext extends PortalHookContext {

    private final String integrationId;

    public FederatedApisIngestionCompleteHookContext(String integrationId) {
        super(PortalHook.FEDERATED_APIS_INGESTION_COMPLETE);
        this.integrationId = integrationId;
    }

    @Override
    protected Map<HookContextEntry, String> getChildProperties() {
        return Map.ofEntries(Map.entry(HookContextEntry.INTEGRATION_ID, integrationId));
    }
}
