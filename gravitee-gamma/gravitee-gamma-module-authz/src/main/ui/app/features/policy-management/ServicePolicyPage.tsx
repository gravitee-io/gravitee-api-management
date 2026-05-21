import {
    Alert,
    AlertDescription,
    AlertTitle,
    Button,
    Card,
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
import { Plus, RefreshCcw } from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import type { ApiError } from '../../../lib/api/authz-api-client';
import type { CatalogEntry, PolicyRequest, PolicyResponse, PolicyStatus, PolicyType } from '../../../lib/api/authz-api.types';
import { parseGaplSchema } from '../../../lib/gapl-parser';
import { useEntities } from '../../../lib/hooks/useEntities';
import { useEntityOptions } from '../../../lib/hooks/useEntityOptions';
import { usePolicies } from '../../../lib/hooks/usePolicies';
import { useSchema } from '../../../lib/hooks/useSchema';
import { useEnvironment } from '../../lib/env/EnvironmentContext';
import type { AiProviderEntry } from './AiModelTargetPickerDialog';
import { PolicyEditorSheet } from './PolicyEditorSheet';
import { PolicyListTable } from './PolicyListTable';
import type { ChipOption } from './PolicyStatementCard';
import { UnifiedTargetPickerDialog, type UnifiedTarget } from './UnifiedTargetPickerDialog';

export interface ServicePageConfig {
    readonly type: PolicyType;
    readonly title: string;
    readonly description: string;
    readonly createButtonLabel: string;
    readonly searchPlaceholder: string;
    /** Icon for the page header */
    readonly icon?: React.ComponentType<{ className?: string }>;
    /** If false, the target column is hidden and no target picker is shown (Custom policies). */
    readonly hasTarget: boolean;
    /** Target picker dialog variant. 'ai-model' uses the two-step AI Model picker. */
    readonly targetPickerVariant?: 'default' | 'ai-model';
    readonly targetPickerTitle?: string;
    readonly targetPickerDescription?: string;
    readonly targetPickerEmptyState?: string;
    readonly targetPickerSearchPlaceholder?: string;
    /** Service label displayed in the editor (e.g. 'MCP', 'Agent', 'AI Model') */
    readonly serviceLabel: string;
    /** Resource groups for the statement resource picker. */
    readonly resourceGroups?: readonly { key: string; label: string }[];
    /** Static resource options for Custom policies (no catalog). */
    readonly resourceOptions?: readonly ChipOption[];
    /** Condition snippet chips shown under the condition textarea. */
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
    /** Function to build provider list from catalog entries (for ai-model picker). */
    readonly buildProviders?: (entries: readonly CatalogEntry[]) => readonly AiProviderEntry[];
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
    // The `/catalog` endpoint was retired during the canonical /gamma/authz
    // migration. Pages that used to drive their target picker from a curated
    // catalog now synthesize the same shape from the entity list, filtered
    // by the entityId prefix that classifies each entity (mcp.*, agent.*,
    // llm.*, api.*, event.*). For CUSTOM pages (hasTarget=false) the picker
    // is never opened, so the synthesized list stays empty.
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
                    // Canonical entities don't carry sub-resources — the picker
                    // shows just the entity, no nested actions or endpoints.
                    subResources: [],
                };
            });
        return { entries, isLoading: entities.isLoading, error: entities.error };
    }, [config.hasTarget, config.type, entities.data, entities.isLoading, entities.error]);

    const [sheet, setSheet] = useState<SheetState>({ kind: 'closed' });
    const [unifiedPickerOpen, setUnifiedPickerOpen] = useState(false);
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

    // Resource options for the Custom policy editor. Target-bound pages
    // (MCP/LLM/API/AGENT) seed a single chip from the picked entity and rely
    // on `config.resourceOptions` (which is normally undefined for those
    // pages — the picker is the only entry point). Custom policies have no
    // target, so the resource picker must be a live catalog. We source it
    // from the entity collection, excluding principals and the Actions
    // catalog so the dropdown stays focused on things a policy can target.
    const customResourceOptions = useMemo((): readonly ChipOption[] => {
        if (config.hasTarget) return [];
        const items: ChipOption[] = [];
        for (const e of entities.data?.data ?? []) {
            const kindRaw = (e.attributes?._kind as string | undefined) ?? '';
            const kind = kindRaw.toLowerCase();
            const firstSeg = e.uid.includes('.') ? e.uid.slice(0, e.uid.indexOf('.')).toLowerCase() : '';
            // skip principals
            if (PRINCIPAL_KINDS.has(kind) || PRINCIPAL_KINDS.has(firstSeg)) continue;
            // skip actions
            if (kind === 'action' || firstSeg === 'action') continue;
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
            items.push({ id: e.uid, label, group, description });
        }
        items.sort((a, b) => (a.group === b.group ? a.label.localeCompare(b.label) : a.group.localeCompare(b.group)));
        return items;
    }, [config.hasTarget, entities.data]);

    const effectiveConfig = useMemo<ServicePageConfig>(() => {
        if (config.hasTarget) return config;
        return {
            ...config,
            resourceOptions: customResourceOptions,
            resourceGroups: [
                { key: 'API', label: 'API' },
                { key: 'MCP', label: 'MCP' },
                { key: 'LLM', label: 'LLM' },
                { key: 'Agent', label: 'Agent' },
                { key: 'Resource', label: 'Resource' },
            ],
        };
    }, [config, customResourceOptions]);

    const existingTargetIds = useMemo(() => (policies.data?.data ?? []).map(p => p.target?.id ?? '').filter(Boolean), [policies.data]);

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
        if (!config.hasTarget) {
            setSheet({ kind: 'create', target: null });
            return;
        }
        setUnifiedPickerOpen(true);
    };

    const onUnifiedPick = (target: UnifiedTarget) => {
        setUnifiedPickerOpen(false);
        // Open editor inline with a CatalogEntry synthesized from the picked
        // unified target. Editor uses the current page's config (resource
        // groups / snippets) — the simplest cross-type flow that avoids extra
        // routing or config-switching machinery.
        // PolicyType includes 'CUSTOM' which CatalogServiceType doesn't —
        // narrow with a fallback since the unified picker only emits service
        // types (MCP/LLM/API/AGENT), never CUSTOM.
        const type = target.type === 'CUSTOM' ? 'API' : target.type;
        setSheet({
            kind: 'create',
            target: { id: target.id, name: target.name, description: target.description, type, subResources: [] },
        });
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

    const providers = useMemo(() => {
        if (!config.buildProviders) return [];
        return config.buildProviders(catalog.entries);
    }, [config.buildProviders, catalog.entries]);

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
                        <RefreshCcw className="size-4" />
                    </Button>
                    <Button type="button" onClick={startCreate}>
                        <Plus size={16} aria-hidden /> {config.hasTarget ? 'Create policy' : config.createButtonLabel}
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
                <KpiCard label="Policies" value={kpis.total} loading={policies.isLoading} />
                <KpiCard label="Deployed" value={kpis.deployed} loading={policies.isLoading} tone="emerald" />
                <KpiCard label="Draft" value={kpis.draft} loading={policies.isLoading} tone="muted" />
                {config.hasTarget ? <KpiCard label="Unique targets" value={kpis.uniqueTargets} loading={policies.isLoading} /> : null}
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

            {config.hasTarget ? (
                <UnifiedTargetPickerDialog
                    open={unifiedPickerOpen}
                    onOpenChange={setUnifiedPickerOpen}
                    entities={entities.data?.data ?? []}
                    existingTargetIds={existingTargetIds}
                    serviceType={config.type}
                    serviceLabel={config.serviceLabel}
                    onSelect={onUnifiedPick}
                />
            ) : null}

            <PolicyEditorSheet
                config={effectiveConfig}
                open={sheet.kind !== 'closed'}
                policy={sheet.kind === 'edit' ? sheet.policy : null}
                initialTarget={
                    sheet.kind === 'create'
                        ? sheet.target
                        : sheet.kind === 'edit' && sheet.policy.target
                          ? // Re-hydrate the catalog entry by id so the resource picker has the
                            // proper subResources (tools/endpoints/etc) to choose from. Falls back
                            // to a synthetic stub if the entry is no longer in the catalog.
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
                onChangeTarget={
                    config.hasTarget
                        ? () => {
                              setSheet({ kind: 'closed' });
                              setUnifiedPickerOpen(true);
                          }
                        : undefined
                }
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

// Small presentational tile for the KPI strip. Kept local to this page since
// no other page renders the same shape — promote it if a second caller appears.
function KpiCard({
    label,
    value,
    loading,
    tone,
}: {
    readonly label: string;
    readonly value: number;
    readonly loading: boolean;
    readonly tone?: 'emerald' | 'muted';
}) {
    const valueClass = tone === 'emerald' ? 'text-2xl text-emerald-600' : tone === 'muted' ? 'text-2xl text-muted-foreground' : 'text-2xl';
    return (
        <Card role="listitem" className="p-4" aria-label={label}>
            <div className={valueClass} aria-live="polite">
                {loading ? '—' : value}
            </div>
            <p className="text-xs text-muted-foreground">{label}</p>
        </Card>
    );
}
