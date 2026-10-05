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

import { cn } from '@gravitee/graphene-core';
import { NavLink, Outlet, useParams } from 'react-router-dom';

import { useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import { useIntegrationPermissions } from '../hooks/useIntegrationPermissions';
import { INTEGRATION_CONFIGURATION_PERMISSIONS } from '../utils/integrationPermissions';

function sectionLinkClassName({ isActive }: Readonly<{ isActive: boolean }>) {
    return cn(
        '-mb-px border-b-2 px-0.5 pb-3 text-sm transition-colors',
        isActive ? 'border-foreground font-semibold text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
    );
}

export function IntegrationDetailLayout() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const basePath = useDetailBasePath('integrations', integrationId);
    const { data: permissions } = useIntegrationPermissions(integrationId);
    const canConfigure = INTEGRATION_CONFIGURATION_PERMISSIONS.some(permission => permissions?.includes(permission));

    return (
        <div className="space-y-6">
            <div className="border-b">
                <nav className="flex items-center gap-6" aria-label="Integration sections">
                    <NavLink to={basePath} end className={sectionLinkClassName}>
                        Overview
                    </NavLink>
                    {canConfigure && (
                        <NavLink to={`${basePath}/configuration`} className={sectionLinkClassName}>
                            Configuration
                        </NavLink>
                    )}
                </nav>
            </div>
            <Outlet />
        </div>
    );
}
