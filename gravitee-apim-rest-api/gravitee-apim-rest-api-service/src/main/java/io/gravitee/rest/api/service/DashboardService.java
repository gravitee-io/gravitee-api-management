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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.DashboardType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.DashboardReferenceType;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DashboardService {
    List<DashboardEntity> findAll();
    List<DashboardEntity> findAllByReference(DashboardReferenceType referenceType, String referenceId);
    List<DashboardEntity> findByReferenceAndType(DashboardReferenceType referenceType, String referenceId, DashboardType type);
    DashboardEntity findByReferenceAndId(DashboardReferenceType referenceType, String referenceId, String dashboardId);
    DashboardEntity create(ExecutionContext executionContext, NewDashboardEntity dashboard);

    /**
     * Initializes the default dashboards for an environment.
     * @param executionContext containing the environment to create dashboards for.
     */
    void initialize(ExecutionContext executionContext);

    DashboardEntity update(
        ExecutionContext executionContext,
        DashboardReferenceType referenceType,
        String referenceId,
        UpdateDashboardEntity dashboard
    );
    void delete(ExecutionContext executionContext, DashboardReferenceType referenceType, String referenceId, String dashboardId);
}
