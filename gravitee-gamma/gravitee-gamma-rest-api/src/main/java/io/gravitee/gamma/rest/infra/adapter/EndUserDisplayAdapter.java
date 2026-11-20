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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.analytics_engine.domain_service.EndUserDisplaySettingDomainService;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.EndUserDisplayPort;
import lombok.RequiredArgsConstructor;

/**
 * Reads {@code logging.user.displayed}, the organization setting the Console logs already honour, through the platform
 * service the Management API v2 filter catalog reads it with.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class EndUserDisplayAdapter implements EndUserDisplayPort {

    private final EndUserDisplaySettingDomainService endUserDisplaySetting;

    @Override
    public boolean displaysEndUser(String organizationId) {
        return endUserDisplaySetting.isDisplayed(organizationId);
    }
}
