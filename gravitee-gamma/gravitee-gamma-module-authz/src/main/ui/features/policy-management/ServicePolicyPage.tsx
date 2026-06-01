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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { PlusIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useDeferredValue, useMemo, useState } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { ValidationErrorAlert } from '../../components/ValidationErrorAlert';
import type { PolicyRequest, PolicyResponse, PolicyStatus, PolicyType } from '../../shared/api/authz-api.types';
import type { ChipOption } from '../../shared/chip-option';
import { parseGaplSchema } from '../../shared/gapl-parser';
import { useEntities } from '../../shared/hooks/useEntities';
import { useEntityOptions } from '../../shared/hooks/useEntityOptions';
import { usePolicies } from '../../shared/hooks/usePolicies';
import { useSchema } from '../../shared/hooks/useSchema';
import { PolicyEditorSheet } from './PolicyEditorSheet';
import { PolicyListTable } from './PolicyListTable';

export type CatalogEntryType = 'MCP' | 'AGENT' | 'MODEL' | 'API' | 'EVENT';

export type CatalogEntry = {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly type: CatalogEntryType;
    readonly subResources: readonly never[];
};

const CATALOG_ENTRY_TYPES: readonly CatalogEntryType[] = ['MCP', 'AGENT', 'MODEL', 'API', 'EVENT'];

function toCatalogEntryType(type: PolicyType): CatalogEntryType {
    return CATALOG_ENTRY_TYPES.includes(type as CatalogEntryType) ? (type as CatalogEntryType) : 'API';
}

type StatusFilter = PolicyStatus | 'ALL';

export interface ServicePageConfig {
    readonly type: PolicyType;
    readonly title: string;
    readonly description: string;
    readonly createButtonLabel: string;
    readonly searchPlaceholder: string;
    readonly icon?: React.ComponentType<{ className?: string }>;
    readonly hasTarget: boolean;
    readonly serviceLabel: string;
    readonly resourceGroups?: readonly { key: string; label: string }[];
    readonly resourceOptions?: readonly ChipOption[];
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
}

type SheetState = { kind: 'closed' } | { kind: 'create'; target: CatalogEntry | null } | { kind: 'edit'; policy: PolicyResponse };

const DEFAULT_RESOURCE_GROUPS: readonly { key: string; label: string }[] = [
    { key: 'API', label: 'API' },
    { key: 'MCP', label: 'MCP' },
    { key: 'Model', label: 'Model' },
    { key: 'Agent', label: 'Agent' },
    { key: 'Resource', label: 'Resource' },
];

// Custom policies cover everything not already routed to a dedicated page
// (API / MCP / Model) and never the Action picker. Resources carrying these
// prefixes are excluded from the custom resource picker.
const CUSTOM_RESOURCE_EXCLUDED_PREFIXES = new Set(['api', 'mcp', 'model', 'llm', 'action']);

export function ServicePolicyPage({ config }: { readonly config: ServicePageConfig }) {
    const env = useEnvironment();
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
    const [search, setSearch] = useState('');
    const deferredSearch = useDeferredValue(search);
    const [sheet, setSheet] = useState<SheetState>({ kind: 'closed' });
    const [submitError, setSubmitError] = useState<Error | null>(null);
    const [pendingDelete, setPendingDelete] = useState<PolicyResponse | null>(null);
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<Error | null>(null);

    const envId = env?.id ?? '';
    const policies = usePolicies(envId, {
        type: config.type,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
    });
    const schema = useSchema(envId);
    // Catalog: targets of this policy type (mcp.*, llm.*, api.*). Scoped to the
    // service prefix instead of "fetch all" so envs with 10k+ entities still load fast.
    const catalogEntities = useEntities(envId, 200, {
        kind: 'RESOURCE',
        ...(config.hasTarget ? { entityIdPrefix: `${config.type.toLowerCase()}.` } : {}),
    });
    // Action chip options. Separate scoped fetch so it never bloats with catalog rows.
    const actionEntities = useEntities(envId, 200, { kind: 'RESOURCE', entityIdPrefix: 'action.' });

    const allPolicies = useMemo<readonly PolicyResponse[]>(() => policies.data?.data ?? [], [policies.data]);

    const filteredPolicies = useMemo(() => {
        const needle = deferredSearch.trim().toLowerCase();
        if (!needle) return allPolicies;
        return allPolicies.filter(p => p.name.toLowerCase().includes(needle));
    }, [allPolicies, deferredSearch]);

    const total = policies.data?.total ?? allPolicies.length;
    const isPaginated = total > allPolicies.length;
    const pageSuffix = isPaginated ? ' (this page)' : '';

    const kpis = useMemo(
        () => ({
            total,
            deployed: allPolicies.filter(p => p.status === 'DEPLOYED').length,
            draft: allPolicies.filter(p => p.status === 'DRAFT').length,
            uniqueTargets: new Set(allPolicies.map(p => p.target?.id).filter((id): id is string => Boolean(id))).size,
        }),
        [allPolicies, total],
    );

    const catalog = useMemo(() => {
        if (!config.hasTarget) {
            return { entries: [] as CatalogEntry[], isLoading: false, error: undefined as string | undefined };
        }
        const all = catalogEntities.data?.data ?? [];
        const entries: CatalogEntry[] = all.map(e => {
            const attrs = e.attributes ?? {};
            const name =
                typeof attrs.displayName === 'string' && attrs.displayName
                    ? attrs.displayName
                    : typeof attrs.name === 'string' && attrs.name
                      ? attrs.name
                      : e.uid;
            const description = typeof attrs.description === 'string' ? attrs.description : '';
            return {
                id: e.uid,
                name,
                description,
                type: toCatalogEntryType(config.type),
                subResources: [],
            };
        });
        return { entries, isLoading: catalogEntities.isLoading, error: catalogEntities.error };
    }, [config.hasTarget, config.type, catalogEntities.data, catalogEntities.isLoading, catalogEntities.error]);

    const actionOptions = useMemo((): readonly ChipOption[] => {
        const seen = new Set<string>();
        const merged: ChipOption[] = [];
        if (schema.schema?.schemaText) {
            const parsed = parseGaplSchema(schema.schema.schemaText);
            for (const a of parsed.actions) {
                const id = `Action::"${a.name}"`;
                if (!seen.has(id)) {
                    seen.add(id);
                    merged.push({ id, label: a.name, group: 'Action' });
                }
            }
        }
        for (const e of actionEntities.data?.data ?? []) {
            const name = (e.attributes?.name as string) || e.uid.replace(/^action\./, '');
            if (!name) continue;
            const id = `Action::"${name}"`;
            if (seen.has(id)) continue;
            seen.add(id);
            const description = typeof e.attributes?.description === 'string' ? (e.attributes.description as string) : undefined;
            merged.push({ id, label: name, group: 'Action', description });
        }
        return merged;
    }, [schema.schema, actionEntities.data]);

    const { options: principalOptions } = useEntityOptions(envId, {
        typeFilter: ['User', 'Group', 'ServiceAccount', 'AgentIdentity'],
    });

    const emptyActionsHint = useMemo<string | undefined>(() => {
        if (actionOptions.length > 0) return undefined;
        return 'No actions defined yet. Add some under Policy Structure → Actions, or declare them in the schema.';
    }, [actionOptions.length]);

    const emptyPrincipalsHint = useMemo<string | undefined>(() => {
        if (principalOptions.length > 0) return undefined;
        return 'No principals available. Add Users, Groups, Service Accounts or Agent Identities under Policy Structure → Entities.';
    }, [principalOptions.length]);

    const serviceResourceOptions = useMemo((): readonly ChipOption[] => {
        // catalogEntities is already scoped server-side to this service prefix
        // (kind=RESOURCE + entityIdPrefix=<type>.) — no need to re-filter out
        // principals or other service types here.
        const items: ChipOption[] = [];
        const typePrefix = config.type.toLowerCase();
        for (const e of catalogEntities.data?.data ?? []) {
            const attrs = e.attributes ?? {};
            const firstSeg = e.uid.includes('.') ? e.uid.slice(0, e.uid.indexOf('.')).toLowerCase() : '';
            if (config.hasTarget && firstSeg !== typePrefix) continue;
            if (!config.hasTarget && CUSTOM_RESOURCE_EXCLUDED_PREFIXES.has(firstSeg)) continue;
            const label =
                typeof attrs.displayName === 'string' && attrs.displayName
                    ? (attrs.displayName as string)
                    : typeof attrs.name === 'string' && attrs.name
                      ? (attrs.name as string)
                      : e.uid.includes('.')
                        ? e.uid.slice(e.uid.indexOf('.') + 1)
                        : e.uid;
            const group =
                firstSeg === 'mcp'
                    ? 'MCP'
                    : firstSeg === 'model' || firstSeg === 'llm'
                      ? 'Model'
                      : firstSeg === 'agent'
                        ? 'Agent'
                        : firstSeg === 'api'
                          ? 'API'
                          : 'Resource';
            const description = typeof attrs.description === 'string' ? (attrs.description as string) : undefined;
            const labelForUid = e.uid.includes('.') ? e.uid.slice(e.uid.indexOf('.') + 1) : e.uid;
            items.push({ id: `${group}::"${labelForUid}"`, label, group, description });
        }
        items.sort((a, b) => (a.group === b.group ? a.label.localeCompare(b.label) : a.group.localeCompare(b.group)));
        return items;
    }, [config.hasTarget, config.type, catalogEntities.data]);

    const effectiveConfig = useMemo<ServicePageConfig>(
        () => ({
            ...config,
            resourceOptions: serviceResourceOptions,
            resourceGroups: config.resourceGroups ?? DEFAULT_RESOURCE_GROUPS,
        }),
        [config, serviceResourceOptions],
    );

    const startCreate = useCallback(() => {
        setSubmitError(null);
        setSheet({ kind: 'create', target: null });
    }, []);

    const submit = useCallback(
        async (req: PolicyRequest) => {
            try {
                if (sheet.kind === 'edit') {
                    await policies.update(sheet.policy.id, req);
                } else {
                    await policies.create(req);
                }
                setSheet({ kind: 'closed' });
                setSubmitError(null);
            } catch (e) {
                const err = e instanceof Error ? e : new Error('Save failed');
                setSubmitError(err);
                throw err;
            }
        },
        [policies, sheet],
    );

    const onEdit = useCallback((p: PolicyResponse) => {
        setSubmitError(null);
        setSheet({ kind: 'edit', policy: p });
    }, []);

    const confirmDelete = async () => {
        if (!pendingDelete) return;
        setDeleting(true);
        setDeleteError(null);
        try {
            await policies.remove(pendingDelete.id);
            setPendingDelete(null);
        } catch (err) {
            setDeleteError(err instanceof Error ? err : new Error(String(err)));
        } finally {
            setDeleting(false);
        }
    };

    const closeDeleteDialog = () => {
        setPendingDelete(null);
        setDeleteError(null);
    };

    const handleReload = useCallback(() => {
        policies.reload();
        catalogEntities.reload();
        actionEntities.reload();
    }, [policies, catalogEntities, actionEntities]);

    const Icon = config.icon;
    const totalCount = policies.data?.total ?? 0;
    const hasNoPolicies = !policies.isLoading && totalCount === 0 && !deferredSearch && statusFilter === 'ALL';
    const hasNoMatches = !policies.isLoading && filteredPolicies.length === 0 && totalCount > 0;
    const catalogError = config.hasTarget ? catalog.error : undefined;

    return (
        <div className="flex flex-col gap-4" data-testid={`page-policies-${config.type.toLowerCase()}`}>
            <header className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                    {Icon ? <Icon className="mt-1 size-5 text-muted-foreground" /> : null}
                    <div>
                        <h1 className="text-xl font-semibold">{config.title}</h1>
                        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{config.description}</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <Button type="button" variant="ghost" size="icon" onClick={handleReload} aria-label="Refresh">
                        <RefreshCwIcon aria-hidden className="size-4" />
                    </Button>
                    <Button type="button" onClick={startCreate}>
                        <PlusIcon aria-hidden className="size-4" />
                        {config.hasTarget ? 'Create policy' : config.createButtonLabel}
                    </Button>
                </div>
            </header>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4" aria-label="Key metrics">
                <KpiTile label="Policies" value={kpis.total} loading={policies.isLoading} />
                <KpiTile label={`Deployed${pageSuffix}`} value={kpis.deployed} loading={policies.isLoading} tone="success" />
                <KpiTile label={`Draft${pageSuffix}`} value={kpis.draft} loading={policies.isLoading} tone="muted" />
                {config.hasTarget ? (
                    <KpiTile label={`Unique targets${pageSuffix}`} value={kpis.uniqueTargets} loading={policies.isLoading} />
                ) : null}
            </div>

            <div className="flex flex-wrap items-center gap-2">
                <Input
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder={config.searchPlaceholder || 'Search policies…'}
                    className="max-w-sm"
                    aria-label="Search policies"
                />
                <Select value={statusFilter} onValueChange={v => setStatusFilter(v as StatusFilter)}>
                    <SelectTrigger className="w-44" aria-label="Filter by status">
                        <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">All statuses</SelectItem>
                        <SelectItem value="DRAFT">Draft</SelectItem>
                        <SelectItem value="DEPLOYED">Deployed</SelectItem>
                        <SelectItem value="DISABLED">Disabled</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {policies.error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load policies</AlertTitle>
                    <AlertDescription>{policies.error}</AlertDescription>
                </Alert>
            )}

            {catalogError !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load catalog</AlertTitle>
                    <AlertDescription>{catalogError}</AlertDescription>
                </Alert>
            )}

            {hasNoPolicies ? (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No policies yet</EmptyTitle>
                        <EmptyDescription>Create a policy to define who can do what.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            ) : hasNoMatches ? (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No policies match your filters</EmptyTitle>
                        <EmptyDescription>Try adjusting the search or status filter.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            ) : (
                <PolicyListTable
                    config={config}
                    policies={filteredPolicies}
                    totalCount={totalCount}
                    page={policies.page}
                    perPage={policies.perPage}
                    isLoading={policies.isLoading}
                    onPageChange={policies.setPage}
                    onPerPageChange={policies.setPerPage}
                    onEdit={onEdit}
                    onDelete={setPendingDelete}
                />
            )}

            <PolicyEditorSheet
                config={effectiveConfig}
                open={sheet.kind !== 'closed'}
                policy={sheet.kind === 'edit' ? sheet.policy : null}
                initialTarget={
                    sheet.kind === 'create'
                        ? sheet.target
                        : sheet.kind === 'edit' && sheet.policy.target
                          ? (catalog.entries.find(e => e.id === sheet.policy.target!.id) ?? {
                                id: sheet.policy.target.id,
                                name: sheet.policy.target.label,
                                description: '',
                                type: toCatalogEntryType(config.type),
                                subResources: [],
                            })
                          : null
                }
                submitError={submitError}
                principalOptions={principalOptions}
                actionOptions={actionOptions}
                emptyPrincipalsHint={emptyPrincipalsHint}
                emptyActionsHint={emptyActionsHint}
                onOpenChange={o => {
                    if (!o) {
                        setSheet({ kind: 'closed' });
                    }
                }}
                onSubmit={submit}
            />

            <Dialog open={pendingDelete !== null} onOpenChange={open => !open && closeDeleteDialog()}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Delete policy?</DialogTitle>
                        <DialogDescription>
                            {pendingDelete ? `"${pendingDelete.name}" will be permanently removed. This action cannot be undone.` : ''}
                        </DialogDescription>
                    </DialogHeader>
                    {deleteError ? <ValidationErrorAlert error={deleteError} title="Delete failed" /> : null}
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={closeDeleteDialog} disabled={deleting}>
                            Cancel
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            onClick={confirmDelete}
                            disabled={deleting}
                            aria-label={pendingDelete ? `Confirm delete ${pendingDelete.name}` : 'Confirm delete'}
                        >
                            {deleting ? 'Deleting…' : deleteError ? 'Retry' : 'Delete'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
