/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { Navigate, useParams } from 'react-router-dom';

import { usePortalsNavigation } from '../../portals/config/navigation';

/** Redirects legacy `/portals/:id/tenants` URLs into settings. */
export function LegacyPortalTenantsRedirect() {
    const { portalId = '', tenantId } = useParams<{ portalId: string; tenantId?: string }>();
    const { portalTenantsPath, portalTenantDetailPath } = usePortalsNavigation();

    const to = tenantId ? portalTenantDetailPath(portalId, tenantId) : portalTenantsPath(portalId);
    return <Navigate to={to} replace />;
}
