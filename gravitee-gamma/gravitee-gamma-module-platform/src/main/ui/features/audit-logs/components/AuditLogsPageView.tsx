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

import { AuditExportDialog, AuditLogsFilters } from './AuditLogsFilters';
import { AuditLogsTable } from './AuditLogsTable';
import type { useAuditLogsPageState } from '../hooks/useAuditLogsPageState';
import type { AuditGroupedRefs, AuditLogRow, AuditNamedRef, AuditScope } from '../types/auditLog';

export interface AuditLogsPageViewProps {
    readonly scope: AuditScope;
    readonly description: string;
    readonly state: ReturnType<typeof useAuditLogsPageState>;
    readonly rows: readonly AuditLogRow[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly isError: boolean;
    readonly eventTypes: readonly string[];
    readonly environments?: readonly AuditNamedRef[];
    readonly applications: readonly AuditNamedRef[] | readonly AuditGroupedRefs[];
    readonly apis: readonly AuditNamedRef[] | readonly AuditGroupedRefs[];
}

function AuditPageHeading({ description }: Readonly<{ description: string }>) {
    return (
        <div className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight">Audit</h1>
            <p className="text-sm text-muted-foreground">{description}</p>
        </div>
    );
}

/** Shared chrome for both Audit pages: heading, filter toolbar, results table and export dialog. */
export function AuditLogsPageView({
    scope,
    description,
    state,
    rows,
    totalCount,
    loading,
    isError,
    eventTypes,
    environments,
    applications,
    apis,
}: AuditLogsPageViewProps) {
    const filters = (
        <AuditLogsFilters
            scope={scope}
            eventTypes={eventTypes}
            event={state.event}
            onEventChange={state.onEventChange}
            referenceType={state.referenceType}
            onReferenceTypeChange={state.onReferenceTypeChange}
            environments={environments}
            environmentId={state.environmentId}
            onEnvironmentIdChange={state.onEnvironmentIdChange}
            applications={applications}
            applicationId={state.applicationId}
            onApplicationIdChange={state.onApplicationIdChange}
            apis={apis}
            apiId={state.apiId}
            onApiIdChange={state.onApiIdChange}
            datePreset={state.datePreset}
            onDatePresetChange={state.onDatePresetChange}
            customRange={state.customRange}
            onCustomRangeChange={state.onCustomRangeChange}
            onReset={state.handleReset}
            onExport={() => state.setExportOpen(true)}
        />
    );

    return (
        <div className="space-y-4">
            <AuditPageHeading description={description} />
            {isError ? <p className="text-sm text-destructive">Failed to load audit logs. Please try again.</p> : null}
            <AuditLogsTable
                rows={isError ? [] : rows}
                loading={!isError && loading}
                page={state.page}
                pageSize={state.pageSize}
                totalCount={isError ? 0 : totalCount}
                onPageChange={state.setPage}
                onPageSizeChange={size => {
                    state.setPageSize(size);
                    state.setPage(1);
                }}
                selected={state.selected}
                onSelectRow={state.setSelected}
                onCloseDetail={() => state.setSelected(null)}
                toolbar={filters}
            />
            <AuditExportDialog
                open={state.exportOpen}
                exporting={state.exporting}
                onOpenChange={state.setExportOpen}
                onConfirm={state.handleExport}
            />
        </div>
    );
}
