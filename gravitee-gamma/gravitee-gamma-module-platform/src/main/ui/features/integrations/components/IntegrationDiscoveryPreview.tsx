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

import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    type BadgeVariant,
    Button,
    DataTable,
    type DataTableProps,
    DataTableEmptyState,
    Switch,
} from '@gravitee/graphene-core';
import { SearchIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useState } from 'react';

import type { ColCell } from '../../../shared/utils/dataTableTypes';
import type { IngestionScope, IntegrationPreview, IntegrationPreviewApi, IntegrationPreviewApiState } from '../types/integration';
import { SMALLEST_TABLE_PAGE_SIZE, TABLE_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';

const DEFAULT_PREVIEW_PAGE_SIZE = 10;

const STATE_CONFIG: Record<IntegrationPreviewApiState, { label: string; variant: BadgeVariant }> = {
    NEW: { label: 'Create', variant: 'success' },
    UPDATE: { label: 'Update', variant: 'warning' },
};

function stateConfig(state: IntegrationPreviewApiState) {
    return Object.prototype.hasOwnProperty.call(STATE_CONFIG, state) ? STATE_CONFIG[state] : undefined;
}

function PreviewApiStateBadge({ state }: Readonly<{ state: IntegrationPreviewApiState }>) {
    const config = stateConfig(state);

    const unrecognizedState = config ? undefined : state;
    useEffect(() => {
        if (unrecognizedState) {
            console.warn(
                `Discovered API state "${unrecognizedState}" is not one of ${Object.keys(STATE_CONFIG).join(', ')}; rendering no badge`,
            );
        }
    }, [unrecognizedState]);

    if (!config) {
        return null;
    }
    return <Badge variant={config.variant}>{config.label}</Badge>;
}

function previewApiLabel(api: IntegrationPreviewApi): string {
    return api.version ? `${api.name} (${api.version})` : api.name;
}

const COLUMNS: DataTableProps<IntegrationPreviewApi>['columns'] = [
    {
        id: 'name',
        enableSorting: false,
        header: 'Name',
        cell: ({ row }: ColCell<IntegrationPreviewApi>) => previewApiLabel(row.original),
    },
    {
        id: 'state',
        enableSorting: false,
        header: 'Create or update',
        cell: ({ row }: ColCell<IntegrationPreviewApi>) => <PreviewApiStateBadge state={row.original.state} />,
    },
];

function PartialDiscoveryWarning() {
    return (
        <Alert variant="warning">
            <TriangleAlertIcon className="size-4" aria-hidden />
            <AlertTitle>Partial API Discovery Warning</AlertTitle>
            <AlertDescription>
                <span className="flex flex-col gap-2">
                    <span>
                        We were only able to discover a subset of the APIs listed below due to an extended discovery time. You can still
                        proceed with ingesting all available APIs, but please note the following limitations:
                    </span>
                    <span>The preview may not show all APIs.</span>
                    <span>Selective ingestion options (e.g., only ingest new or update selected) are currently disabled.</span>
                    <span>
                        All APIs, both discovered and undiscovered, will be fully processed. Existing APIs will be updated, and any new ones
                        will be added.
                    </span>
                </span>
            </AlertDescription>
        </Alert>
    );
}

function PreviewCount({ label, value }: Readonly<{ label: string; value: number }>) {
    return (
        <div className="space-y-1">
            <dt className="text-sm text-muted-foreground">{label}</dt>
            <dd className="text-2xl font-semibold">{value}</dd>
        </div>
    );
}

function PreviewApisTable({ apis }: Readonly<{ apis: IntegrationPreviewApi[] }>) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PREVIEW_PAGE_SIZE);
    const pageApis = apis.slice((page - 1) * pageSize, page * pageSize);
    const pagination =
        apis.length > SMALLEST_TABLE_PAGE_SIZE
            ? {
                  page,
                  pageSize,
                  totalCount: apis.length,
                  pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                  onPageChange: setPage,
                  onPageSizeChange: (size: number) => {
                      setPageSize(size);
                      setPage(1);
                  },
              }
            : undefined;

    return <DataTable aria-label="Discovered APIs" columns={COLUMNS} data={pageApis} pagination={pagination} />;
}

type ApiStateSelection = Record<IntegrationPreviewApiState, boolean>;

function initialSelection(preview: IntegrationPreview): ApiStateSelection {
    return { NEW: preview.newCount > 0, UPDATE: preview.updateCount > 0 };
}

function ingestionScope(preview: IntegrationPreview, selection: ApiStateSelection): IngestionScope {
    if (preview.isPartiallyDiscovered) return { kind: 'ALL' };
    return { kind: 'SELECTED', apiIds: preview.apis.filter(api => selection[api.state]).map(api => api.id) };
}

function SelectionSwitch({
    label,
    checked,
    disabled,
    onToggle,
}: Readonly<{ label: string; checked: boolean; disabled: boolean; onToggle: (checked: boolean) => void }>) {
    const id = useId();
    return (
        <div className="flex items-center gap-2">
            <Switch id={id} checked={checked} onCheckedChange={onToggle} disabled={disabled} aria-label={label} />
            <label htmlFor={id} className={`text-sm font-medium ${disabled ? 'cursor-default' : 'cursor-pointer'}`}>
                {label}
            </label>
        </div>
    );
}

export function IntegrationDiscoveryPreview({
    preview,
    onProceed,
    isProceeding,
}: Readonly<{ preview: IntegrationPreview; onProceed: (scope: IngestionScope) => void; isProceeding: boolean }>) {
    const [selection, setSelection] = useState<ApiStateSelection>(() => initialSelection(preview));
    const scope = ingestionScope(preview, selection);
    const proceedDisabled = isProceeding || (scope.kind === 'SELECTED' && scope.apiIds.length === 0);

    const toggle = (state: IntegrationPreviewApiState, checked: boolean) => setSelection(current => ({ ...current, [state]: checked }));

    return (
        <div className="space-y-6 rounded-lg border p-6">
            {preview.isPartiallyDiscovered && <PartialDiscoveryWarning />}
            <dl className="grid grid-cols-3 gap-4">
                <PreviewCount label="Discovered APIs" value={preview.totalCount} />
                <PreviewCount label="New APIs" value={preview.newCount} />
                <PreviewCount label="APIs to update" value={preview.updateCount} />
            </dl>
            {!preview.isPartiallyDiscovered && (
                <div className="flex gap-6">
                    <SelectionSwitch
                        label="New APIs"
                        checked={selection.NEW}
                        disabled={preview.newCount === 0}
                        onToggle={checked => toggle('NEW', checked)}
                    />
                    <SelectionSwitch
                        label="APIs to update"
                        checked={selection.UPDATE}
                        disabled={preview.updateCount === 0}
                        onToggle={checked => toggle('UPDATE', checked)}
                    />
                </div>
            )}
            {preview.totalCount === 0 ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<SearchIcon className="size-8" aria-hidden />}
                        title="No assets found"
                        description="We couldn't find any assets at the provider. It looks like there are no available APIs to create or update at this time."
                    />
                </div>
            ) : (
                <PreviewApisTable apis={preview.apis} />
            )}
            <div className="flex justify-end">
                <Button disabled={proceedDisabled} onClick={() => onProceed(scope)}>
                    Proceed
                </Button>
            </div>
        </div>
    );
}
