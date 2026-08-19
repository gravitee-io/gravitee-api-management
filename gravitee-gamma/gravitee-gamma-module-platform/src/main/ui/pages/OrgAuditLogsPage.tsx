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

import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

import { AuditLogsPageView } from '../features/audit-logs/components/AuditLogsPageView';
import { AuditTrailLicenseDialog } from '../features/audit-logs/components/AuditTrailLicenseDialog';
import {
    useAuditEnvironments,
    useAuditEvents,
    useAuditLogs,
    useOrgAuditApis,
    useOrgAuditApplications,
} from '../features/audit-logs/hooks/useAuditLogs';
import { useAuditLogsPageState } from '../features/audit-logs/hooks/useAuditLogsPageState';
import { APIM_AUDIT_TRAIL_FEATURE } from '../features/audit-logs/license/auditTrailLicense';
import { exportOrgAudits } from '../features/audit-logs/services/auditLogs';
import { toAuditLogRow } from '../features/audit-logs/utils/auditListFormat';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

export function OrgAuditLogsPage() {
    const navigate = useNavigate();
    const hasLicense = useHasFeature(APIM_AUDIT_TRAIL_FEATURE);
    const state = useAuditLogsPageState(exportOrgAudits);
    const { referenceType } = state;

    const logsQuery = useAuditLogs('organization', state.params, undefined, hasLicense);
    const eventsQuery = useAuditEvents('organization', undefined, hasLicense);
    const environmentsQuery = useAuditEnvironments(hasLicense && referenceType === 'ENVIRONMENT');
    const applicationsQuery = useOrgAuditApplications(hasLicense && referenceType === 'APPLICATION');
    const apisQuery = useOrgAuditApis(hasLicense && referenceType === 'API');

    const isForbidden = isForbiddenApiError(Boolean(logsQuery.isError), logsQuery.error);
    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: 'organization-audit-',
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
            scope="organization"
            description="Search configuration changes across the organization."
            state={state}
            rows={rows}
            totalCount={logsQuery.data?.totalElements ?? 0}
            loading={logsQuery.isFetching}
            isError={Boolean(logsQuery.isError)}
            eventTypes={eventsQuery.data ?? []}
            environments={environmentsQuery.data ?? []}
            applications={applicationsQuery.data ?? []}
            apis={apisQuery.data ?? []}
        />
    );
}
