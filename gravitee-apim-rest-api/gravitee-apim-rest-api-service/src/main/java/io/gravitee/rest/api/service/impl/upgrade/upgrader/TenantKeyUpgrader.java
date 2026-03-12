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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.api.TenantRepository;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class TenantKeyUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private TenantRepository tenantRepository;

    @Override
    public boolean upgrade() {
        try {
            tenantRepository
                .findAll()
                .forEach(tenant -> {
                    log.info("Migrate tenant {}", tenant.getName());
                    tenant.setKey(tenant.getId());
                    try {
                        tenantRepository.update(tenant);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            return true;
        } catch (Exception e) {
            log.error("TenantKeyUpgrader failed. Fail to upgrade tenant key");
            return false;
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.TENANT_KEY_UPGRADER;
    }
}
