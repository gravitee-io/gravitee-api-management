/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.NewTenantEntity;
import io.gravitee.rest.api.model.TenantEntity;
import io.gravitee.rest.api.model.TenantReferenceType;
import io.gravitee.rest.api.model.UpdateTenantEntity;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TenantService {
    List<TenantEntity> findByReference(String referenceId, TenantReferenceType referenceType);

    TenantEntity findByIdAndReference(String tenantId, String referenceId, TenantReferenceType referenceType);

    List<TenantEntity> create(List<NewTenantEntity> tenants, String referenceId, TenantReferenceType referenceType);

    List<TenantEntity> update(List<UpdateTenantEntity> tenants, String referenceId, TenantReferenceType referenceType);

    void delete(String tenantId, String referenceId, TenantReferenceType referenceType);
}
