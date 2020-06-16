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

import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;

import java.util.Date;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AuditService {

    void createApiAuditLog(String apiId, Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, Date createdAt, Object oldValue, Object newValue);
    void createApplicationAuditLog(String applicationId, Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, Date createdAt, Object oldValue, Object newValue);
    void createEnvironmentAuditLog(Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, Date createdAt, Object oldValue, Object newValue);
    void createOrganizationAuditLog(Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, Date createdAt, Object oldValue, Object newValue);

    MetadataPage<AuditEntity> search(AuditQuery query);
}
