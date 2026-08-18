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

import { useCallback, useMemo, useState } from 'react';
import type { DateRange } from 'react-day-picker';

import { useResolvedAuditDateRange } from './useResolvedAuditDateRange';
import { notify } from '../../../shared/notify';
import { AuditExportLimitError } from '../services/auditLogs';
import type {
    AuditDatePreset,
    AuditExportFormat,
    AuditLogRow,
    AuditMetadataPage,
    AuditReferenceType,
    AuditSearchParams,
} from '../types/auditLog';
import { auditLogsToCsv, auditLogsToJson, buildAuditExportFileName, downloadAuditExport } from '../utils/auditExport';
import { toAuditLogRow } from '../utils/auditListFormat';

type ExportAudits = (params: Omit<AuditSearchParams, 'page' | 'size'>) => Promise<AuditMetadataPage>;

/**
 * Filter, pagination, selection and export state shared by the organization and environment
 * Audit pages. The pages differ only in which services they hit, so they inject `exportAudits`
 * and read `params` back to drive their own scope-specific queries.
 */
export function useAuditLogsPageState(exportAudits: ExportAudits) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [event, setEvent] = useState('');
    const [referenceType, setReferenceType] = useState<AuditReferenceType | ''>('');
    const [environmentId, setEnvironmentId] = useState('');
    const [applicationId, setApplicationId] = useState('');
    const [apiId, setApiId] = useState('');
    const [datePreset, setDatePreset] = useState<AuditDatePreset>('');
    const [customRange, setCustomRange] = useState<DateRange | undefined>();
    const [selected, setSelected] = useState<AuditLogRow | null>(null);
    const [exportOpen, setExportOpen] = useState(false);
    const [exporting, setExporting] = useState(false);

    const dateRange = useResolvedAuditDateRange(datePreset, customRange);
    // `environment` is only ever set on the organization page; the environment page has no such
    // picker, so it stays undefined there and `buildAuditQuery` drops it.
    const params: AuditSearchParams = useMemo(
        () => ({
            page,
            size: pageSize,
            event: event || undefined,
            type: referenceType || undefined,
            environment: environmentId || undefined,
            application: applicationId || undefined,
            api: apiId || undefined,
            from: dateRange.from,
            to: dateRange.to,
        }),
        [page, pageSize, event, referenceType, environmentId, applicationId, apiId, dateRange.from, dateRange.to],
    );

    const resetPage = useCallback((action: () => void) => {
        action();
        setPage(1);
    }, []);

    const handleReset = useCallback(() => {
        setEvent('');
        setReferenceType('');
        setEnvironmentId('');
        setApplicationId('');
        setApiId('');
        setDatePreset('');
        setCustomRange(undefined);
        setPage(1);
    }, []);

    const handleExport = useCallback(
        async (format: AuditExportFormat) => {
            setExporting(true);
            try {
                const pageResult = await exportAudits({
                    event: params.event,
                    type: params.type,
                    environment: params.environment,
                    application: params.application,
                    api: params.api,
                    from: params.from,
                    to: params.to,
                });
                const exportRows = (pageResult.content ?? []).map(audit => toAuditLogRow(audit, pageResult.metadata ?? {}));
                const fileName = buildAuditExportFileName(format);
                if (format === 'csv') {
                    downloadAuditExport(auditLogsToCsv(exportRows), fileName, 'text/csv;charset=utf-8');
                } else {
                    downloadAuditExport(auditLogsToJson(exportRows), fileName, 'application/json');
                }
                notify.success('Audit logs exported.');
                setExportOpen(false);
            } catch (error) {
                notify.error(error, error instanceof AuditExportLimitError ? error.message : 'Failed to export audit logs.');
            } finally {
                setExporting(false);
            }
        },
        [exportAudits, params],
    );

    const handleReferenceTypeChange = useCallback(
        (value: AuditReferenceType | '') =>
            resetPage(() => {
                setReferenceType(value);
                setEnvironmentId('');
                setApplicationId('');
                setApiId('');
            }),
        [resetPage],
    );

    const handleDatePresetChange = useCallback(
        (value: AuditDatePreset) =>
            resetPage(() => {
                setDatePreset(value);
                if (value !== 'custom') {
                    setCustomRange(undefined);
                }
            }),
        [resetPage],
    );

    const handleCustomRangeChange = useCallback(
        (value: DateRange | undefined) =>
            resetPage(() => {
                setCustomRange(value);
                if (value?.from || value?.to) {
                    setDatePreset('custom');
                } else {
                    setDatePreset(current => (current === 'custom' ? '' : current));
                }
            }),
        [resetPage],
    );

    return {
        params,
        page,
        setPage,
        pageSize,
        setPageSize,
        event,
        onEventChange: useCallback((value: string) => resetPage(() => setEvent(value)), [resetPage]),
        referenceType,
        onReferenceTypeChange: handleReferenceTypeChange,
        environmentId,
        onEnvironmentIdChange: useCallback((value: string) => resetPage(() => setEnvironmentId(value)), [resetPage]),
        applicationId,
        onApplicationIdChange: useCallback((value: string) => resetPage(() => setApplicationId(value)), [resetPage]),
        apiId,
        onApiIdChange: useCallback((value: string) => resetPage(() => setApiId(value)), [resetPage]),
        datePreset,
        onDatePresetChange: handleDatePresetChange,
        customRange,
        onCustomRangeChange: handleCustomRangeChange,
        selected,
        setSelected,
        exportOpen,
        setExportOpen,
        exporting,
        handleReset,
        handleExport,
    };
}
