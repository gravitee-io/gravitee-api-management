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

import { useEnvironment, useHasFeature } from '@gravitee/gamma-modules-sdk';
import { useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

import { AuditLogsPageView } from '../features/audit-logs/components/AuditLogsPageView';
import { AuditTrailLicenseDialog } from '../features/audit-logs/components/AuditTrailLicenseDialog';
import { useAuditApis, useAuditApplications, useAuditEvents, useAuditLogs } from '../features/audit-logs/hooks/useAuditLogs';
import { useAuditLogsPageState } from '../features/audit-logs/hooks/useAuditLogsPageState';
import { APIM_AUDIT_TRAIL_FEATURE } from '../features/audit-logs/license/auditTrailLicense';
import { exportEnvAudits } from '../features/audit-logs/services/auditLogs';
import type { AuditSearchParams } from '../features/audit-logs/types/auditLog';
import { toAuditLogRow } from '../features/audit-logs/utils/auditListFormat';
import { AUDIT_PERMISSION_PREFIXES } from '../features/audit-logs/utils/auditPermissions';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

export function EnvAuditLogsPage() {
    const navigate = useNavigate();
    const environment = useEnvironment();
    const environmentId = environment?.id ?? '';
    const hasLicense = useHasFeature(APIM_AUDIT_TRAIL_FEATURE);

    const exportAudits = useCallback(
        (params: Omit<AuditSearchParams, 'page' | 'size'>) => exportEnvAudits(environmentId, params),
        [environmentId],
    );
    const state = useAuditLogsPageState(exportAudits);
    const { referenceType } = state;

    const logsQuery = useAuditLogs('environment', state.params, environmentId, hasLicense);
    const eventsQuery = useAuditEvents('environment', environmentId, hasLicense);
    const applicationsQuery = useAuditApplications(environmentId, hasLicense && referenceType === 'APPLICATION');
    const apisQuery = useAuditApis(environmentId, hasLicense && referenceType === 'API');

    const isForbidden = isForbiddenApiError(Boolean(logsQuery.isError), logsQuery.error);
    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: AUDIT_PERMISSION_PREFIXES,
        redirectTo: '../applications',
    });

    const rows = useMemo(
        () => (logsQuery.data?.content ?? []).map(audit => toAuditLogRow(audit, logsQuery.data?.metadata ?? {})),
        [logsQuery.data],
    );

    if (!hasLicense) {
        return <AuditTrailLicenseDialog open onOpenChange={open => !open && navigate('../applications')} />;
    }

    if (isForbidden) {
        return null;
    }

    return (
        <AuditLogsPageView
            scope="environment"
            description="Search configuration changes for this environment."
            state={state}
            rows={rows}
            totalCount={logsQuery.data?.totalElements ?? 0}
            loading={logsQuery.isLoading}
            isError={Boolean(logsQuery.isError)}
            eventTypes={eventsQuery.data ?? []}
            applications={applicationsQuery.data ?? []}
            apis={apisQuery.data ?? []}
        />
    );
}
