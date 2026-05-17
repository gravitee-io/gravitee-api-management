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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Skeleton } from '@gravitee/graphene-core';
import { Outlet, useLocation, useParams } from 'react-router-dom';

import { ApplicationDetailAccessDenied } from './ApplicationDetailAccessDenied';
import { getApplicationDetailTabPermissions } from '../../../../config/applicationDetailNavigation';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import { useApplicationDetailContext } from '../../context/ApplicationDetailContext';

function useApplicationDetailTabPath(): string {
    const { applicationId } = useParams<{ applicationId: string }>();
    const location = useLocation();
    const basePath = useDetailBasePath('applications', applicationId);

    if (!basePath || !location.pathname.startsWith(basePath)) {
        return '';
    }

    const remainder = location.pathname.slice(basePath.length).replace(/^\//, '');
    return remainder.split('/')[0] ?? '';
}

export function ApplicationDetailProtectedOutlet() {
    const { permissionsReady, permissionsError } = useApplicationDetailContext();
    const tabPath = useApplicationDetailTabPath();
    const requiredPermissions = getApplicationDetailTabPermissions(tabPath);
    const canAccessTab = useHasPermission({ anyOf: requiredPermissions ?? [] });
    const isAccessDenied = Boolean(requiredPermissions?.length) && !canAccessTab;

    if (permissionsError || !permissionsReady) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    if (isAccessDenied) {
        return <ApplicationDetailAccessDenied />;
    }

    return <Outlet />;
}
