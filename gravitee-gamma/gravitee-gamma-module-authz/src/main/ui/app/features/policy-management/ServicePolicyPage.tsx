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
    Spinner,
} from '@gravitee/graphene-core';
import { PlusIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';
import { KpiTile } from '../../../components/KpiTile';
import type { ApiError } from '../../../lib/api/authz-api-client';
import type { CatalogEntry, PolicyRequest, PolicyResponse, PolicyStatus, PolicyType } from '../../../lib/api/authz-api.types';
import { parseGaplSchema } from '../../../lib/gapl-parser';
import { useEntities } from '../../../lib/hooks/useEntities';
import { useEntityOptions } from '../../../lib/hooks/useEntityOptions';
import { usePolicies } from '../../../lib/hooks/usePolicies';
import { useSchema } from '../../../lib/hooks/useSchema';
import { useEnvironment } from '../../lib/env/EnvironmentContext';
import { PolicyEditorSheet } from './PolicyEditorSheet';
import { PolicyListTable } from './PolicyListTable';
import type { ChipOption } from './PolicyStatementCard';

export interface ServicePageConfig {
    readonly type: PolicyType;
    readonly title: string;
    readonly description: string;
    readonly createButtonLabel: string;
    readonly searchPlaceholder: string;
    /** Icon for the page header */
    readonly icon?: React.ComponentType<{ className?: string }>;
    /** If false, the target column is hidden (Custom policies). */
    readonly hasTarget: boolean;
    /** Service label displayed in the editor (e.g. 'MCP', 'Agent', 'AI Model') */
    readonly serviceLabel: string;
    /** Resource groups for the statement resource chip combobox. */
    readonly resourceGroups?: readonly { key: string; label: string }[];
    /** Static resource options for Custom policies (no catalog). */
    readonly resourceOptions?: readonly ChipOption[];
    /** Condition snippet chips shown under the condition textarea. */
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
}

type SheetState = { kind: 'closed' } | { kind: 'create'; target: CatalogEntry | null } | { kind: 'edit'; policy: PolicyResponse };

const PRINCIPAL_KINDS: ReadonlySet<string> = new Set([
    'user',
    'group',
    'serviceaccount',
    'service-account',
    'service_account',
    'agent',
    'agentidentity',
]);

export function ServicePolicyPage({ config }: { readonly config: ServicePageConfig }) {
    const env = useEnvironment();

    const policies = usePolicies(env, { type: config.type });
    const schema = useSchema(env);
    // We synthesize a CatalogEntry list from the entity collection, filtered
    // by the entityId prefix that classifies each entity (mcp.*, agent.*,
    // llm.*, api.*, event.*). The list is consumed only when re-hydrating an
    // edited policy's target — the in-editor chip combobox builds its
    // options directly from `entities` so the catalog stub stays minimal.
    const entities = useEntities(env, /* initialPerPage */ 1000);
    const catalog = useMemo(() => {
        if (!config.hasTarget) {
            return { entries: [] as CatalogEntry[], isLoading: false, error: undefined as string | undefined };
        }
        const prefix = config.type.toLowerCase() + '.';
        const all = entities.data?.data ?? [];
        const entries: CatalogEntry[] = all
            .filter(e => e.uid.toLowerCase().startsWith(prefix))
            .map(e => {
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
                    type: config.type as 'MCP' | 'AGENT' | 'LLM' | 'API' | 'EVENT',
                    subResources: [],
                };
            });
        return { entries, isLoading: entities.isLoading, error: entities.error };
    }, [config.hasTarget, config.type, entities.data, entities.isLoading, entities.error]);

    const [sheet, setSheet] = useState<SheetState>({ kind: 'closed' });
    const [submitError, setSubmitError] = useState<Error | null>(null);
    const [statusFilter, setStatusFilter] = useState<PolicyStatus | 'ALL'>('ALL');
    const [search, setSearch] = useState('');
    // Pending delete: tracked here so we can render a Graphene Dialog confirm
    // (browser-native window.confirm() is unstyleable, blocks the event loop, and
    // is intercepted by some test runners — replacing it with a Dialog gives us
    // accessible markup and consistent visuals).
    const [pendingDelete, setPendingDelete] = useState<PolicyResponse | null>(null);
    const [deleting, setDeleting] = useState(false);

    // Action options come from two sources merged on action name:
    //   1. The parsed schema (`action "..." appliesTo {...}` declarations).
    //   2. Action entities created via the Actions page (entityId `action.<slug>`
    //      with `_kind=action` and the original name in `attributes.name`).
    // The picker historically only read from the schema; users defining
    // actions in the UI saw an empty list when editing a policy. Both sources
    // are deduped by canonical id (`Action::"<name>"`) which is also the form
    // the GAPL roundtrip parser produces when hydrating selected chips.
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
        for (const e of entities.data?.data ?? []) {
            const kind = (e.attributes?._kind ?? '') as string;
            const isAction = kind.toLowerCase() === 'action' || (typeof e.uid === 'string' && e.uid.startsWith('action.'));
            if (!isAction) continue;
            const name = (e.attributes?.name as string) || e.uid.replace(/^action\./, '');
            if (!name) continue;
            const id = `Action::"${name}"`;
            if (seen.has(id)) continue;
            seen.add(id);
            const description = typeof e.attributes?.description === 'string' ? (e.attributes.description as string) : undefined;
            merged.push({ id, label: name, group: 'Action', description });
        }
        return merged;
    }, [schema.schema, entities.data]);

    // Principal options are sourced live from the entities collection.
    //
    // For target-bound policy types (MCP / AGENT / LLM / API / EVENT) we
    // narrow to the four canonical principal types so the combobox stays
    // focused. Custom policies, by definition, are not bound to any service
    // shape — narrowing there hides perfectly valid principals whose type
    // happens to fall outside the canonical set, so we list everything and
    // let the user pick.
    const { options: principalOptions } = useEntityOptions(env, {
        typeFilter: config.hasTarget ? ['User', 'Group', 'ServiceAccount', 'AgentIdentity'] : undefined,
    });

    // Empty-state hints for the chip pickers: the page knows why a list is
    // empty (schema not loaded, no entities yet, …) so we phrase it for the
    // user instead of letting the picker fall back to a bare "No results."
    const emptyActionsHint = useMemo<string | undefined>(() => {
        if (actionOptions.length > 0) return undefined;
        return 'No actions defined yet. Add some under Policy Structure → Actions, or declare them in the schema.';
    }, [actionOptions.length]);

    const emptyPrincipalsHint = useMemo<string | undefined>(() => {
        if (principalOptions.length > 0) return undefined;
        return 'No principals available. Add Users, Groups, Service Accounts or Agent Identities under Policy Structure → Entities.';
    }, [principalOptions.length]);

    // Resource options for the in-editor chip combobox.
    //
    // For target-bound pages (MCP / LLM / API / AGENT) we narrow to entities
    // whose `entityId` starts with the page's type prefix — the MCPs page
    // only surfaces `mcp.*`, the LLMs page only `llm.*`, etc. Sub-segments
    // (`mcp.bookings.get-booking`) stay in the list so users can scope a
    // statement to a specific tool or endpoint inline.
    //
    // Custom policies are not bound to any service shape — list every
    // non-principal, non-action entity grouped by canonical kind.
    const serviceResourceOptions = useMemo((): readonly ChipOption[] => {
        const items: ChipOption[] = [];
        const typePrefix = config.type.toLowerCase();
        for (const e of entities.data?.data ?? []) {
            const kindRaw = (e.attributes?._kind as string | undefined) ?? '';
            const kind = kindRaw.toLowerCase();
            const firstSeg = e.uid.includes('.') ? e.uid.slice(0, e.uid.indexOf('.')).toLowerCase() : '';
            // skip principals
            if (PRINCIPAL_KINDS.has(kind) || PRINCIPAL_KINDS.has(firstSeg)) continue;
            // skip actions
            if (kind === 'action' || firstSeg === 'action') continue;
            // Target-bound pages: only entities matching this page's type.
            if (config.hasTarget && firstSeg !== typePrefix) continue;
            const attrs = e.attributes ?? {};
            const label =
                typeof attrs.displayName === 'string' && attrs.displayName
                    ? (attrs.displayName as string)
                    : typeof attrs.name === 'string' && attrs.name
                      ? (attrs.name as string)
                      : e.uid.includes('.')
                        ? e.uid.slice(e.uid.indexOf('.') + 1)
                        : e.uid;
            // Group label follows the entityId prefix. `api.*` covers both
            // APIM-managed APIs (synced by the listener) and locally-defined
            // Resources (toBackend collapses Resource → api), so they share
            // the API bucket. Anything unrecognised falls back to "Resource"
            // to keep the picker from dropping options silently.
            const group =
                firstSeg === 'mcp'
                    ? 'MCP'
                    : firstSeg === 'llm'
                      ? 'LLM'
                      : firstSeg === 'agent'
                        ? 'Agent'
                        : firstSeg === 'api'
                          ? 'API'
                          : 'Resource';
            const description = typeof attrs.description === 'string' ? (attrs.description as string) : undefined;
            // Canonical GAPL form (`MCP::"bookings"`) so chip IDs round-trip
            // with parseGaplToStatements after a switch-to-visual or reload.
            const labelForUid = e.uid.includes('.') ? e.uid.slice(e.uid.indexOf('.') + 1) : e.uid;
            items.push({ id: `${group}::"${labelForUid}"`, label, group, description });
        }
        items.sort((a, b) => (a.group === b.group ? a.label.localeCompare(b.label) : a.group.localeCompare(b.group)));
        return items;
    }, [config.hasTarget, config.type, entities.data]);

    const effectiveConfig = useMemo<ServicePageConfig>(
        () => ({
            ...config,
            resourceOptions: serviceResourceOptions,
            resourceGroups: config.resourceGroups ?? [
                { key: 'API', label: 'API' },
                { key: 'MCP', label: 'MCP' },
                { key: 'LLM', label: 'LLM' },
                { key: 'Agent', label: 'Agent' },
                { key: 'Resource', label: 'Resource' },
            ],
        }),
        [config, serviceResourceOptions],
    );

    // KPI summary cards rendered above the filters. Mirrors the prototype's
    // four-tile row (Policies / Deployed / Draft / Unique targets). The
    // "Unique targets" tile is omitted for CUSTOM-style pages where policies
    // are not bound to a catalog target.
    const kpis = useMemo(() => {
        const all = policies.data?.data ?? [];
        const total = policies.data?.total ?? all.length;
        const deployed = all.filter(p => p.status === 'DEPLOYED').length;
        const draft = all.filter(p => p.status === 'DRAFT').length;
        const uniqueTargets = new Set(all.map(p => p.target?.id).filter((id): id is string => Boolean(id))).size;
        return { total, deployed, draft, uniqueTargets };
    }, [policies.data]);

    const filteredPolicies = useMemo(() => {
        const all = policies.data?.data ?? [];
        return all.filter(p => {
            if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
            if (search.trim() && !p.name.toLowerCase().includes(search.trim().toLowerCase())) return false;
            return true;
        });
    }, [policies.data, statusFilter, search]);

    const startCreate = () => {
        setSubmitError(null);
        // No more target-pre-picker step: open the editor directly and let the
        // user pick the resource(s) from the in-editor chip combobox, which is
        // filtered to this page's service type (e.g. mcp.* on MCPs page).
        setSheet({ kind: 'create', target: null });
    };

    const submit = async (req: PolicyRequest) => {
        try {
            if (sheet.kind === 'edit') {
                await policies.update(sheet.policy.id, req);
            } else {
                await policies.create(req);
            }
            setSheet({ kind: 'closed' });
            setSubmitError(null);
        } catch (e) {
            setSubmitError(e instanceof Error ? e : new Error('Save failed'));
        }
    };

    const onDelete = (p: PolicyResponse) => {
        // Open the confirm dialog; the actual deletion runs in `confirmDelete`
        // when the user clicks the destructive action.
        setSubmitError(null);
        setPendingDelete(p);
    };

    const confirmDelete = async () => {
        if (!pendingDelete) return;
        setDeleting(true);
        try {
            await policies.remove(pendingDelete.id);
            setPendingDelete(null);
        } catch (e) {
            setSubmitError(e instanceof Error ? e : new Error('Delete failed'));
            setPendingDelete(null);
        } finally {
            setDeleting(false);
        }
    };

    const handleReload = useCallback(() => {
        policies.reload();
        entities.reload();
    }, [policies, entities]);

    const isLoading = policies.isLoading || (config.hasTarget && catalog.isLoading);

    return (
        <div
            style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}
            data-testid={`page-policies-${config.type.toLowerCase()}`}
        >
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">{config.title}</h1>
                    <p className="text-sm text-muted-foreground mt-1">{config.description}</p>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <Button type="button" variant="outline" size="sm" onClick={handleReload} aria-label="Refresh">
                        <RefreshCwIcon aria-hidden className="size-4" />
                    </Button>
                    <Button type="button" onClick={startCreate}>
                        <PlusIcon aria-hidden className="size-4" /> {config.hasTarget ? 'Create policy' : config.createButtonLabel}
                    </Button>
                </div>
            </header>

            {/* KPI summary tiles */}
            <div
                role="list"
                aria-label="Policy summary"
                style={{
                    display: 'grid',
                    gridTemplateColumns: `repeat(${config.hasTarget ? 4 : 3}, minmax(0, 1fr))`,
                    gap: '1rem',
                }}
            >
                <KpiTile label="Policies" value={kpis.total} loading={policies.isLoading} />
                <KpiTile label="Deployed" value={kpis.deployed} loading={policies.isLoading} tone="success" />
                <KpiTile label="Draft" value={kpis.draft} loading={policies.isLoading} tone="muted" />
                {config.hasTarget ? <KpiTile label="Unique targets" value={kpis.uniqueTargets} loading={policies.isLoading} /> : null}
            </div>

            {/* Filters */}
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                <Input
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder={config.searchPlaceholder || 'Search policies…'}
                    className="max-w-xs"
                    aria-label="Search policies"
                />
                <Select value={statusFilter} onValueChange={v => setStatusFilter(v as PolicyStatus | 'ALL')}>
                    <SelectTrigger className="w-32" aria-label="Filter by status">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">All statuses</SelectItem>
                        <SelectItem value="DRAFT">Draft</SelectItem>
                        <SelectItem value="DEPLOYED">Deployed</SelectItem>
                        <SelectItem value="DISABLED">Disabled</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {isLoading && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Spinner aria-hidden />
                    <span>Loading…</span>
                </div>
            )}

            {!isLoading && policies.error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load policies</AlertTitle>
                    <AlertDescription>{policies.error}</AlertDescription>
                </Alert>
            )}

            {!isLoading && catalog.error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load catalog</AlertTitle>
                    <AlertDescription>{catalog.error}</AlertDescription>
                </Alert>
            )}

            {!isLoading && policies.error === undefined && policies.data !== null && policies.data.total === 0 && (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No policies yet</EmptyTitle>
                        <EmptyDescription>Create a policy to define who can do what.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            )}

            {!isLoading &&
                policies.error === undefined &&
                policies.data !== null &&
                policies.data.total > 0 &&
                filteredPolicies.length === 0 && (
                    <Empty>
                        <EmptyHeader>
                            <EmptyTitle>No policies match your filters</EmptyTitle>
                            <EmptyDescription>Try adjusting the search or status filter.</EmptyDescription>
                        </EmptyHeader>
                    </Empty>
                )}

            {!isLoading && policies.error === undefined && filteredPolicies.length > 0 && (
                <PolicyListTable
                    config={config}
                    policies={filteredPolicies}
                    allData={policies.data!}
                    page={policies.page}
                    perPage={policies.perPage}
                    onPageChange={policies.setPage}
                    onEdit={p => {
                        setSubmitError(null);
                        setSheet({ kind: 'edit', policy: p });
                    }}
                    onDelete={onDelete}
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
                          ? // Re-hydrate the catalog entry by id so the editor header can show
                            // the policy's target name. Falls back to a synthetic stub when the
                            // entry is no longer in the catalog (e.g. the resource was deleted).
                            (catalog.entries.find(e => e.id === sheet.policy.target!.id) ?? {
                                id: sheet.policy.target.id,
                                name: sheet.policy.target.label,
                                description: '',
                                type: config.type as 'MCP' | 'AGENT' | 'LLM' | 'API' | 'EVENT',
                                subResources: [],
                            })
                          : null
                }
                submitError={submitError as ApiError | null}
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

            {/* Delete confirmation. Replaces window.confirm() for accessibility,
                stylability and reliable test interaction. */}
            <Dialog
                open={pendingDelete !== null}
                onOpenChange={open => {
                    if (!open) setPendingDelete(null);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Delete policy?</DialogTitle>
                        <DialogDescription>
                            {pendingDelete ? `"${pendingDelete.name}" will be permanently removed. This action cannot be undone.` : ''}
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setPendingDelete(null)} disabled={deleting}>
                            Cancel
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            onClick={confirmDelete}
                            disabled={deleting}
                            aria-label={pendingDelete ? `Confirm delete ${pendingDelete.name}` : 'Confirm delete'}
                        >
                            {deleting ? 'Deleting…' : 'Delete'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}

