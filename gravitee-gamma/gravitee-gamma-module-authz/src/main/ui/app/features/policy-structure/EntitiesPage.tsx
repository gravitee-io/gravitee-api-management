/**
 * Full Entities page — principals, resources, and the entities.json view.
 * Ported from prototype entities-page.tsx, wired to the real useEntities() hook.
 */
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
    Spinner,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
    cn,
} from '@gravitee/graphene-core';
import {
    Braces,
    Building2,
    ChevronRight,
    Lock,
    Plus,
    Scale,
    Search,
    Shield,
    Upload,
    UploadCloud,
} from 'lucide-react';
import { useMemo, useState, type MouseEvent, type ReactNode } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/Tabs';
import { entityKeyOf, fromBackend } from '../../../lib/entity-adapter';
import { parseGaplSchema } from '../../../lib/gapl-parser';
import { useEntities } from '../../../lib/hooks/useEntities';
import { usePolicies } from '../../../lib/hooks/usePolicies';
import { useSchema } from '../../../lib/hooks/useSchema';
import { buildPolicyEntityRefs, type PolicyRef } from '../../../lib/policy-entity-refs';
import { useEnvironment } from '../../lib/env/EnvironmentContext';
import { AddLocalPrincipalDialog } from './AddLocalPrincipalDialog';
import { EntityDetailSheet } from './EntityDetailSheet';
import { EntityIdChip } from './EntityIdChip';
import { PrincipalImportDialog } from './PrincipalImportDialog';
import { ScimConnectorsDialog } from './ScimConnectorsDialog';
import { CATEGORIES, getEntityCategoryId, type AttrValue, type EntityInstance } from './entity-types';

// ---------- entities.json rendering ------------------------------------------

interface JsonLine {
    text: string;
}

function formatValue(value: AttrValue): string {
    if (typeof value === 'string') return `"${value.replace(/"/g, '\\"')}"`;
    return String(value);
}

function buildEntitiesJson(entities: EntityInstance[]): JsonLine[] {
    const lines: JsonLine[] = [];
    const push = (text: string) => lines.push({ text });

    push('[');

    const byCategory = new Map<string, EntityInstance[]>();
    for (const cat of CATEGORIES) byCategory.set(cat.label, []);
    const categoryLabelFor = (typeName: string) => {
        const cid = getEntityCategoryId(typeName);
        return CATEGORIES.find(c => c.id === cid)?.label ?? 'Other';
    };
    for (const inst of entities) {
        const lbl = categoryLabelFor(inst.uid.type);
        const arr = byCategory.get(lbl) ?? [];
        arr.push(inst);
        byCategory.set(lbl, arr);
    }

    const flat: EntityInstance[] = [];
    const catMarkers = new Map<number, string>();
    const typeMarkers = new Map<number, string>();
    for (const [label, list] of byCategory.entries()) {
        if (list.length === 0) continue;
        const byType = new Map<string, EntityInstance[]>();
        for (const i of list) {
            const arr = byType.get(i.uid.type) ?? [];
            arr.push(i);
            byType.set(i.uid.type, arr);
        }
        let firstInCat = true;
        for (const [typeName, items] of byType.entries()) {
            let firstInType = true;
            for (const inst of items) {
                const idx = flat.length;
                if (firstInCat) {
                    catMarkers.set(idx, label);
                    firstInCat = false;
                }
                if (firstInType) {
                    typeMarkers.set(idx, typeName);
                    firstInType = false;
                }
                flat.push(inst);
            }
        }
    }

    flat.forEach((inst, idx) => {
        const catLabel = catMarkers.get(idx);
        const typeName = typeMarkers.get(idx);
        if (catLabel) push(`  // ===== ${catLabel} =====`);
        if (typeName) push(`  // --- ${typeName} ---`);

        const isLast = idx === flat.length - 1;
        push('  {');
        push(`    "uid": {`);
        push(`      "type": ${formatValue(inst.uid.type)},`);
        push(`      "id": ${formatValue(inst.uid.id)}`);
        push(`    },`);
        push(`    "source": ${formatValue(inst.source)},`);

        const attrEntries = Object.entries(inst.attrs);
        if (attrEntries.length === 0) {
            push(`    "attrs": {},`);
        } else {
            push(`    "attrs": {`);
            attrEntries.forEach(([k, v], i) => {
                push(`      ${formatValue(k)}: ${formatValue(v)}${i === attrEntries.length - 1 ? '' : ','}`);
            });
            push(`    },`);
        }

        if (inst.parents.length === 0) {
            push(`    "parents": []`);
        } else {
            push(`    "parents": [`);
            inst.parents.forEach((par, i) => {
                push(`      {`);
                push(`        "type": ${formatValue(par.type)},`);
                push(`        "id": ${formatValue(par.id)}`);
                push(`      }${i === inst.parents.length - 1 ? '' : ','}`);
            });
            push(`    ]`);
        }
        push(isLast ? '  }' : '  },');
    });

    push(']');
    return lines;
}

function highlightJsonLine(text: string): ReactNode {
    const trimmed = text.trimStart();
    if (trimmed.startsWith('//')) {
        return <span className="text-slate-400 dark:text-slate-500">{text}</span>;
    }
    const tokenRe = /("(?:\\"|[^"])*"\s*:)|("(?:\\"|[^"])*")|(-?\d+(?:\.\d+)?)|\b(true|false|null)\b|([\[\]{},:])|(\s+)|(.)/g;
    const nodes: ReactNode[] = [];
    let match: RegExpExecArray | null;
    let key = 0;
    while ((match = tokenRe.exec(text)) !== null) {
        const [, keyTok, strTok, numTok, boolTok, puncTok, wsTok, rawTok] = match;
        if (wsTok) {
            nodes.push(wsTok);
            continue;
        }
        if (keyTok) {
            nodes.push(
                <span key={key++} className="text-sky-600 dark:text-sky-400">
                    {keyTok}
                </span>,
            );
            continue;
        }
        if (strTok) {
            nodes.push(
                <span key={key++} className="text-amber-600 dark:text-amber-400">
                    {strTok}
                </span>,
            );
            continue;
        }
        if (numTok) {
            nodes.push(
                <span key={key++} className="text-emerald-600 dark:text-emerald-400">
                    {numTok}
                </span>,
            );
            continue;
        }
        if (boolTok) {
            nodes.push(
                <span key={key++} className="text-fuchsia-600 dark:text-fuchsia-400">
                    {boolTok}
                </span>,
            );
            continue;
        }
        if (puncTok) {
            nodes.push(
                <span key={key++} className="text-muted-foreground">
                    {puncTok}
                </span>,
            );
            continue;
        }
        if (rawTok) {
            nodes.push(
                <span key={key++} className="text-foreground">
                    {rawTok}
                </span>,
            );
        }
    }
    return <>{nodes}</>;
}

// ---------- Source cell -------------------------------------------------------

function SourceCell({ entity }: { entity: EntityInstance }) {
    if (entity.source === 'scim') {
        const provider = entity.principalProvider ?? 'IdP';
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <Badge variant="secondary" className="cursor-help gap-1">
                        <Shield className="size-3" />
                        SCIM · {provider}
                    </Badge>
                </TooltipTrigger>
                <TooltipContent side="top">Synced from {provider} via SCIM 2.0. Read-only in Authorization.</TooltipContent>
            </Tooltip>
        );
    }
    if (entity.source === 'directory') {
        const provider = entity.principalProvider ?? 'User Directory';
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <Badge variant="secondary" className="cursor-help gap-1">
                        <Building2 className="size-3" />
                        {provider}
                    </Badge>
                </TooltipTrigger>
                <TooltipContent side="top">Imported from the {provider}. Read-only in Authorization.</TooltipContent>
            </Tooltip>
        );
    }
    return <Badge variant="outline">Local</Badge>;
}

// ---------- Relationships -----------------------------------------------------

interface ChildrenIndex {
    byParent: Map<string, Map<string, EntityInstance[]>>;
}

function buildChildrenIndex(entities: EntityInstance[]): ChildrenIndex {
    const byParent = new Map<string, Map<string, EntityInstance[]>>();
    for (const e of entities) {
        for (const p of e.parents) {
            const k = `${p.type}::${p.id}`;
            const byType = byParent.get(k) ?? new Map<string, EntityInstance[]>();
            const arr = byType.get(e.uid.type) ?? [];
            arr.push(e);
            byType.set(e.uid.type, arr);
            byParent.set(k, byType);
        }
    }
    return { byParent };
}

function pluralizeType(type: string, n: number) {
    const map: Record<string, [string, string]> = {
        MCPTool: ['tool', 'tools'],
        MCPPrompt: ['prompt', 'prompts'],
        MCPResource: ['resource', 'resources'],
        Endpoint: ['endpoint', 'endpoints'],
        DataField: ['data field', 'data fields'],
        Topic: ['topic', 'topics'],
        SchemaField: ['schema field', 'schema fields'],
        LLMModel: ['model', 'models'],
        AgentTool: ['tool', 'tools'],
        AgentMemory: ['memory', 'memories'],
        AgentSkill: ['skill', 'skills'],
        AgentKnowledge: ['knowledge base', 'knowledge bases'],
        User: ['user', 'users'],
        Group: ['group', 'groups'],
        ServiceAccount: ['service account', 'service accounts'],
        AgentIdentity: ['agent identity', 'agent identities'],
    };
    const [single, plural] = map[type] ?? [type, `${type}s`];
    return `${n} ${n === 1 ? single : plural}`;
}

function RelationshipsCell({ entity, childrenIndex }: { entity: EntityInstance; childrenIndex: ChildrenIndex }) {
    const outgoing = entity.parents;
    const incomingByType = childrenIndex.byParent.get(`${entity.uid.type}::${entity.uid.id}`);

    if (outgoing.length === 0 && !incomingByType?.size) {
        return <span className="text-xs text-muted-foreground">—</span>;
    }

    return (
        <div className="flex flex-wrap items-center gap-1">
            {outgoing.map(p => (
                <Tooltip key={`in:${p.type}:${p.id}`}>
                    <TooltipTrigger asChild>
                        <Badge variant="secondary" className="h-5 max-w-[200px] px-1.5 text-[10px]">
                            <span className="mr-1 text-muted-foreground">in</span>
                            <span className="truncate font-mono">
                                {p.type}::{p.id.split('.').slice(-1)[0]}
                            </span>
                        </Badge>
                    </TooltipTrigger>
                    <TooltipContent side="top" className="font-mono text-[11px]">
                        Member of {p.type}::&quot;{p.id}&quot;
                    </TooltipContent>
                </Tooltip>
            ))}
            {incomingByType
                ? Array.from(incomingByType.entries()).map(([type, list]) => (
                      <Tooltip key={`contains:${type}`}>
                          <TooltipTrigger asChild>
                              <Badge variant="outline" className="h-5 px-1.5 text-[10px] text-muted-foreground">
                                  <span className="mr-1 text-muted-foreground/70">contains</span>
                                  <span className="font-medium text-foreground">{pluralizeType(type, list.length)}</span>
                              </Badge>
                          </TooltipTrigger>
                          <TooltipContent side="top" className="max-w-[320px] font-mono text-[11px]">
                              <div className="mb-1 text-muted-foreground">{type} referencing this entity:</div>
                              <ul className="space-y-0.5">
                                  {list.slice(0, 8).map(c => (
                                      <li key={`${c.uid.type}:${c.uid.id}`} className="truncate">
                                          {c.displayName ?? (c.attrs.name as string | undefined) ?? c.uid.id}
                                      </li>
                                  ))}
                                  {list.length > 8 ? <li className="text-muted-foreground">+{list.length - 8} more…</li> : null}
                              </ul>
                          </TooltipContent>
                      </Tooltip>
                  ))
                : null}
        </div>
    );
}

// ---------- Policies cell -----------------------------------------------------

const POLICY_TYPE_TONE: Record<string, string> = {
    MCP: 'bg-blue-500/10 text-blue-700 dark:text-blue-300',
    API: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
    AGENT: 'bg-purple-500/10 text-purple-700 dark:text-purple-300',
    LLM: 'bg-amber-500/10 text-amber-700 dark:text-amber-300',
    EVENT: 'bg-rose-500/10 text-rose-700 dark:text-rose-300',
    CUSTOM: 'bg-slate-500/10 text-slate-700 dark:text-slate-300',
};

function PoliciesCell({ refs, isLoading }: { refs: PolicyRef[]; isLoading: boolean }) {
    if (isLoading) {
        return (
            <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                <Spinner aria-hidden />
                <span>loading…</span>
            </span>
        );
    }
    if (refs.length === 0) {
        return <span className="text-xs text-muted-foreground">0 policies</span>;
    }

    const principalCount = refs.filter(r => r.clauses.includes('principal')).length;
    const resourceCount = refs.filter(r => r.clauses.includes('resource')).length;

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <button
                    type="button"
                    className="inline-flex items-center gap-1.5 rounded-md border bg-card px-2 py-0.5 text-xs hover:bg-accent"
                >
                    <Scale className="size-3 text-muted-foreground" />
                    <span className="font-medium tabular-nums">{refs.length}</span>
                    <span className="text-muted-foreground">{refs.length === 1 ? 'policy' : 'policies'}</span>
                </button>
            </TooltipTrigger>
            <TooltipContent side="top" className="max-w-[360px] p-0">
                <div className="border-b px-3 py-2 text-[11px] text-muted-foreground">
                    Referenced in {refs.length} {refs.length === 1 ? 'policy' : 'policies'}
                    {principalCount > 0 || resourceCount > 0 ? (
                        <>
                            {' '}
                            · {principalCount > 0 ? `${principalCount} as principal` : ''}
                            {principalCount > 0 && resourceCount > 0 ? ', ' : ''}
                            {resourceCount > 0 ? `${resourceCount} as resource` : ''}
                        </>
                    ) : null}
                </div>
                <ul className="max-h-[220px] space-y-0.5 overflow-auto px-2 py-2 text-sm">
                    {refs.slice(0, 12).map(({ policy, clauses }) => {
                        const asLabel = clauses.length === 1 ? clauses[0] : clauses.slice().sort().join(' · ');
                        return (
                            <li key={policy.id} className="flex items-start gap-2 rounded px-1.5 py-1">
                                <span
                                    className={cn(
                                        'mt-0.5 shrink-0 rounded px-1 text-[10px] font-medium',
                                        POLICY_TYPE_TONE[policy.type] ?? 'bg-muted',
                                    )}
                                >
                                    {policy.type}
                                </span>
                                <span className="min-w-0 flex-1">
                                    <span className="block truncate">{policy.name}</span>
                                    <span className="block text-[10px] text-muted-foreground">
                                        as {asLabel}
                                        {policy.target ? ` · ${policy.target.label}` : ''}
                                        {' · '}
                                        {policy.status.toLowerCase()}
                                    </span>
                                </span>
                            </li>
                        );
                    })}
                    {refs.length > 12 ? <li className="px-1.5 py-1 text-[11px] text-muted-foreground">+{refs.length - 12} more…</li> : null}
                </ul>
            </TooltipContent>
        </Tooltip>
    );
}

// ---------- Entities table ----------------------------------------------------

interface EntityTableProps {
    entities: EntityInstance[];
    childrenIndex: ChildrenIndex;
    policyIndex: Map<string, PolicyRef[]>;
    policiesLoading: boolean;
    emptyState: ReactNode;
    showSource: boolean;
    onOpenEntity: (key: string) => void;
    onDelete: (entity: EntityInstance) => void;
}

function EntitiesTable({
    entities,
    childrenIndex,
    policyIndex,
    policiesLoading,
    emptyState,
    showSource,
    onOpenEntity,
    onDelete,
}: EntityTableProps) {
    if (entities.length === 0) {
        return <div className="rounded-xl border bg-card p-10 text-center">{emptyState}</div>;
    }
    return (
        <div className="overflow-hidden rounded-xl border bg-card">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead className="w-[160px]">Type</TableHead>
                        <TableHead className="w-[360px]">Entity ID</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead className="w-[150px]">Policies</TableHead>
                        <TableHead>Relationships</TableHead>
                        {showSource ? <TableHead className="w-[170px]">Source</TableHead> : null}
                        <TableHead className="w-[80px] text-right" />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {entities.map(e => {
                        const displayName = e.displayName ?? (e.attrs.name as string | undefined) ?? e.uid.id;
                        const readOnly = e.source !== 'local';
                        const key = entityKeyOf(e.uid);
                        return (
                            <TableRow key={`${e.uid.type}:${e.uid.id}`}>
                                <TableCell>
                                    <Badge variant="outline" className="font-mono text-[11px]">
                                        {e.uid.type}
                                    </Badge>
                                </TableCell>
                                <TableCell>
                                    <EntityIdChip value={e.uid.id} maxWidthClassName="max-w-[320px]" />
                                </TableCell>
                                <TableCell className="font-medium">
                                    <button
                                        type="button"
                                        onClick={() => onOpenEntity(key)}
                                        className="group inline-flex max-w-full items-center gap-1.5 rounded text-left hover:text-primary"
                                    >
                                        <span className="truncate">{displayName}</span>
                                        <span className="opacity-0 transition-opacity group-hover:opacity-100">
                                            <ChevronRight className="size-3.5 text-muted-foreground" />
                                        </span>
                                    </button>
                                </TableCell>
                                <TableCell>
                                    <PoliciesCell refs={policyIndex.get(entityKeyOf(e.uid)) ?? []} isLoading={policiesLoading} />
                                </TableCell>
                                <TableCell>
                                    <RelationshipsCell entity={e} childrenIndex={childrenIndex} />
                                </TableCell>
                                {showSource ? (
                                    <TableCell>
                                        <SourceCell entity={e} />
                                    </TableCell>
                                ) : null}
                                <TableCell className="text-right">
                                    {readOnly ? (
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <span className="inline-flex size-6 items-center justify-center text-muted-foreground">
                                                    <Lock className="size-3.5" />
                                                </span>
                                            </TooltipTrigger>
                                            <TooltipContent side="left">
                                                {e.source === 'scim'
                                                    ? `Read-only. Manage this principal in ${e.principalProvider ?? 'the IdP'}.`
                                                    : `Read-only. Manage this principal in ${e.principalProvider ?? 'the User Directory'}.`}
                                            </TooltipContent>
                                        </Tooltip>
                                    ) : (
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="h-6 px-2 text-xs text-muted-foreground hover:text-destructive"
                                            onClick={() => onDelete(e)}
                                            aria-label={`Delete ${e.uid.type}::${e.uid.id}`}
                                        >
                                            Delete
                                        </Button>
                                    )}
                                </TableCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
        </div>
    );
}

// ---------- Filter bar --------------------------------------------------------

type SourceFilter = 'all' | 'local' | 'scim' | 'directory';

function useEntityFilter(entities: EntityInstance[]) {
    const [query, setQuery] = useState('');
    const [typeFilter, setTypeFilter] = useState<'all' | string>('all');
    const [sourceFilter, setSourceFilter] = useState<SourceFilter>('all');

    const typesPresent = useMemo(() => {
        const s = new Set<string>();
        for (const e of entities) s.add(e.uid.type);
        return Array.from(s).sort();
    }, [entities]);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        return entities.filter(e => {
            if (typeFilter !== 'all' && e.uid.type !== typeFilter) return false;
            if (sourceFilter !== 'all' && e.source !== sourceFilter) return false;
            if (!q) return true;
            if (e.uid.id.toLowerCase().includes(q)) return true;
            if (e.uid.type.toLowerCase().includes(q)) return true;
            const name = (e.displayName ?? (e.attrs.name as string | undefined) ?? '').toLowerCase();
            return name.includes(q);
        });
    }, [entities, query, typeFilter, sourceFilter]);

    return { query, setQuery, typeFilter, setTypeFilter, sourceFilter, setSourceFilter, typesPresent, filtered };
}

function FilterBar({
    query,
    onQuery,
    typesPresent,
    typeFilter,
    onTypeFilter,
    sourceFilter,
    onSourceFilter,
    sourceOptions,
}: {
    query: string;
    onQuery: (v: string) => void;
    typesPresent: string[];
    typeFilter: 'all' | string;
    onTypeFilter: (v: 'all' | string) => void;
    sourceFilter: SourceFilter;
    onSourceFilter: (v: SourceFilter) => void;
    sourceOptions: Array<{ value: SourceFilter; label: string }>;
}) {
    return (
        <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
                <Search className="absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                <input
                    value={query}
                    onChange={e => onQuery(e.target.value)}
                    placeholder="Search by name, Entity ID, or type…"
                    className="h-9 w-80 rounded-md border border-input bg-background pl-8 pr-3 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                />
            </div>
            <select
                value={typeFilter}
                onChange={e => onTypeFilter(e.target.value)}
                className="h-9 rounded-md border border-input bg-transparent px-2 text-sm"
            >
                <option value="all">All types</option>
                {typesPresent.map(t => (
                    <option key={t} value={t}>
                        {t}
                    </option>
                ))}
            </select>
            {sourceOptions.length > 0 ? (
                <select
                    value={sourceFilter}
                    onChange={e => onSourceFilter(e.target.value as SourceFilter)}
                    className="h-9 rounded-md border border-input bg-transparent px-2 text-sm"
                >
                    {sourceOptions.map(opt => (
                        <option key={opt.value} value={opt.value}>
                            {opt.label}
                        </option>
                    ))}
                </select>
            ) : null}
        </div>
    );
}

// ---------- Pagination helper -------------------------------------------------

interface EntitiesPaginationProps {
    readonly page: number;
    readonly perPage: number;
    readonly total: number;
    readonly onPageChange: (page: number) => void;
}

/**
 * Compound `<Pagination>` from Graphene. Computes page count locally and renders
 * up to 7 page links with an ellipsis collapsing the middle when there are more.
 */
function EntitiesPagination({ page, perPage, total, onPageChange }: EntitiesPaginationProps) {
    const pageCount = Math.max(1, Math.ceil(total / perPage));
    const from = total === 0 ? 0 : (page - 1) * perPage + 1;
    const to = Math.min(page * perPage, total);
    const pageNumbers = computePageWindow(page, pageCount);
    const goTo = (target: number) => (event: MouseEvent<HTMLAnchorElement>) => {
        event.preventDefault();
        if (target >= 1 && target <= pageCount && target !== page) onPageChange(target);
    };

    return (
        <Pagination className="justify-end py-2">
            <PaginationContent>
                <PaginationItem>
                    <span className="px-2 text-xs text-muted-foreground">
                        {from}–{to} of {total}
                    </span>
                </PaginationItem>
                <PaginationItem>
                    <PaginationPrevious
                        href="#"
                        aria-disabled={page <= 1}
                        className={page <= 1 ? 'pointer-events-none opacity-50' : undefined}
                        onClick={goTo(page - 1)}
                    />
                </PaginationItem>
                {pageNumbers.map((p, i) =>
                    p === 'ellipsis' ? (
                        <PaginationItem key={`e-${i}`}>
                            <PaginationEllipsis />
                        </PaginationItem>
                    ) : (
                        <PaginationItem key={p}>
                            <PaginationLink href="#" isActive={p === page} onClick={goTo(p)}>
                                {p}
                            </PaginationLink>
                        </PaginationItem>
                    ),
                )}
                <PaginationItem>
                    <PaginationNext
                        href="#"
                        aria-disabled={page >= pageCount}
                        className={page >= pageCount ? 'pointer-events-none opacity-50' : undefined}
                        onClick={goTo(page + 1)}
                    />
                </PaginationItem>
            </PaginationContent>
        </Pagination>
    );
}

/** Returns the list of page numbers / ellipsis markers to render (≤ 7 entries). */
function computePageWindow(current: number, count: number): Array<number | 'ellipsis'> {
    if (count <= 7) return Array.from({ length: count }, (_, i) => i + 1);
    if (current <= 4) return [1, 2, 3, 4, 5, 'ellipsis', count];
    if (current >= count - 3) return [1, 'ellipsis', count - 4, count - 3, count - 2, count - 1, count];
    return [1, 'ellipsis', current - 1, current, current + 1, 'ellipsis', count];
}

// ---------- Page --------------------------------------------------------------

export function EntitiesPage() {
    const environmentId = useEnvironment();
    // Fetch a large batch in one go: backend pagination would cut off after 10 entities,
    // and we filter actions / kind-locally in JS — without this, the page would only show
    // 2-3 principals while the rest sit on page 2/3 of the backend response.
    const { data, isLoading, error, page, perPage, setPage, create, remove, reload } = useEntities(environmentId, 500);
    // Load all policies (up to 1000) so we can compute entity↔policy refs client-side.
    const { data: policiesData, isLoading: policiesLoading } = usePolicies(environmentId, { initialPerPage: 1000 });
    // Schema is parsed locally to derive action / principal-kind / resource-kind counts.
    const { schema } = useSchema(environmentId);

    const [jsonImportOpen, setJsonImportOpen] = useState(false);
    const [scimConnectorsOpen, setScimConnectorsOpen] = useState(false);
    const [addPrincipalOpen, setAddPrincipalOpen] = useState(false);
    const [openEntityKey, setOpenEntityKey] = useState<string | null>(null);

    // Convert all backend entities to frontend instances.
    // Actions live on their own page (Policy structure → Actions); hide them here
    // so they don't pollute the Principals / Resources tabs.
    const allEntities = useMemo(() => {
        if (!data) return [];
        return data.data.map(e => fromBackend(e)).filter(e => e.uid.type !== 'Action');
    }, [data]);

    const principals = useMemo(() => allEntities.filter(e => getEntityCategoryId(e.uid.type) === 'principal'), [allEntities]);
    const resources = useMemo(() => allEntities.filter(e => getEntityCategoryId(e.uid.type) !== 'principal'), [allEntities]);

    const childrenIndex = useMemo(() => buildChildrenIndex(allEntities), [allEntities]);

    // Cross-reference: entity → policies that mention it.
    const policyIndex = useMemo(() => buildPolicyEntityRefs(allEntities, policiesData?.data ?? []), [allEntities, policiesData]);

    // Derive KPI counts from the schema text. The parser is tolerant — missing
    // schema collapses to all-zero counts.
    const schemaCounts = useMemo(() => {
        if (!schema?.schemaText) return { actions: 0, principalKinds: 0, resourceKinds: 0 };
        const parsed = parseGaplSchema(schema.schemaText);
        let principalKinds = 0;
        let resourceKinds = 0;
        for (const e of parsed.entities) {
            const cat = getEntityCategoryId(e.name);
            if (cat === 'principal') principalKinds += 1;
            else resourceKinds += 1;
        }
        return { actions: parsed.actions.length, principalKinds, resourceKinds };
    }, [schema]);

    const totalEntities = data?.total ?? allEntities.length;

    const principalFilter = useEntityFilter(principals);
    const resourceFilter = useEntityFilter(resources);

    const jsonLines = useMemo(() => buildEntitiesJson(allEntities), [allEntities]);

    const handleDelete = async (e: EntityInstance) => {
        if (!e._backendId) return;
        if (!window.confirm(`Delete entity "${e.uid.type}::${e.uid.id}"?`)) return;
        try {
            await remove(e._backendId);
        } catch {
            // ignore — reload will clear stale state
        }
    };

    return (
        <div className="space-y-6" data-testid="page-entities">
            {/* Header */}
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1>Entities</h1>
                    <p className="text-muted-foreground">
                        Principals and resources the Policy Engine evaluates against the <span className="font-mono">schema.gapl</span>{' '}
                        contract.
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <Button variant="outline" onClick={() => setJsonImportOpen(true)}>
                        <Upload className="mr-2 size-4" />
                        Import JSON
                    </Button>
                    <Button variant="outline" onClick={() => setScimConnectorsOpen(true)}>
                        <UploadCloud className="mr-2 size-4" />
                        SCIM Connectors
                    </Button>
                    <Button onClick={() => setAddPrincipalOpen(true)}>
                        <Plus className="mr-2 size-4" />
                        Add Principal
                    </Button>
                </div>
            </div>

            {/* KPI badges */}
            {!isLoading && !error && (
                <div className="flex flex-wrap items-center gap-2" aria-label="Entity statistics">
                    <Badge variant="secondary" className="px-2.5 py-1 text-xs">
                        <span className="font-medium tabular-nums">{totalEntities}</span>
                        <span className="ml-1 text-muted-foreground">entities</span>
                    </Badge>
                    <Badge variant="secondary" className="px-2.5 py-1 text-xs">
                        <span className="font-medium tabular-nums">{schemaCounts.actions}</span>
                        <span className="ml-1 text-muted-foreground">actions</span>
                    </Badge>
                    <Badge variant="secondary" className="px-2.5 py-1 text-xs">
                        <span className="font-medium tabular-nums">{schemaCounts.principalKinds}</span>
                        <span className="ml-1 text-muted-foreground">principal kinds</span>
                    </Badge>
                    <Badge variant="secondary" className="px-2.5 py-1 text-xs">
                        <span className="font-medium tabular-nums">{schemaCounts.resourceKinds}</span>
                        <span className="ml-1 text-muted-foreground">resource kinds</span>
                    </Badge>
                </div>
            )}

            {/* Loading */}
            {isLoading && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Spinner aria-hidden />
                    <span>Loading entities…</span>
                </div>
            )}

            {/* Error */}
            {!isLoading && error && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load entities</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {/* Tabs */}
            {!isLoading && !error && (
                <Tabs defaultValue="principals" className="space-y-4">
                    <TabsList>
                        <TabsTrigger value="principals">
                            Principals
                            <Badge variant="secondary" className="ml-2 h-4 px-1 text-[10px]">
                                {principals.length}
                            </Badge>
                        </TabsTrigger>
                        <TabsTrigger value="resources">
                            Resources
                            <Badge variant="secondary" className="ml-2 h-4 px-1 text-[10px]">
                                {resources.length}
                            </Badge>
                        </TabsTrigger>
                        <TabsTrigger value="json">entities.json</TabsTrigger>
                    </TabsList>

                    {/* Principals tab */}
                    <TabsContent value="principals" className="space-y-3">
                        <div className="rounded-md border border-dashed bg-muted/30 px-3 py-2 text-xs text-muted-foreground">
                            Principals (users, groups, service accounts, agent identities) are created locally or imported via JSON. Local
                            principals are fully editable.
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <FilterBar
                                query={principalFilter.query}
                                onQuery={principalFilter.setQuery}
                                typesPresent={principalFilter.typesPresent}
                                typeFilter={principalFilter.typeFilter}
                                onTypeFilter={principalFilter.setTypeFilter}
                                sourceFilter={principalFilter.sourceFilter}
                                onSourceFilter={principalFilter.setSourceFilter}
                                sourceOptions={[
                                    { value: 'all', label: 'All sources' },
                                    { value: 'scim', label: 'SCIM' },
                                    { value: 'directory', label: 'User Directory' },
                                    { value: 'local', label: 'Local' },
                                ]}
                            />
                            <Button variant="outline" onClick={() => setAddPrincipalOpen(true)}>
                                <Plus className="mr-2 size-4" />
                                Add local principal
                            </Button>
                        </div>
                        <EntitiesTable
                            entities={principalFilter.filtered}
                            childrenIndex={childrenIndex}
                            policyIndex={policyIndex}
                            policiesLoading={policiesLoading}
                            showSource={true}
                            onOpenEntity={setOpenEntityKey}
                            onDelete={handleDelete}
                            emptyState={
                                allEntities.length === 0 ? (
                                    <Empty>
                                        <EmptyHeader>
                                            <EmptyTitle>No principals yet</EmptyTitle>
                                            <EmptyDescription>Add a local principal or import from JSON.</EmptyDescription>
                                        </EmptyHeader>
                                    </Empty>
                                ) : (
                                    <>
                                        <div className="text-sm font-medium">No principals match your filters</div>
                                        <div className="mt-1 text-sm text-muted-foreground">
                                            Try clearing the search or changing the type/source filter.
                                        </div>
                                    </>
                                )
                            }
                        />
                    </TabsContent>

                    {/* Resources tab */}
                    <TabsContent value="resources" className="space-y-3">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <FilterBar
                                query={resourceFilter.query}
                                onQuery={resourceFilter.setQuery}
                                typesPresent={resourceFilter.typesPresent}
                                typeFilter={resourceFilter.typeFilter}
                                onTypeFilter={resourceFilter.setTypeFilter}
                                sourceFilter={resourceFilter.sourceFilter}
                                onSourceFilter={resourceFilter.setSourceFilter}
                                sourceOptions={[
                                    { value: 'all', label: 'All sources' },
                                    { value: 'local', label: 'Local' },
                                ]}
                            />
                        </div>
                        <EntitiesTable
                            entities={resourceFilter.filtered}
                            childrenIndex={childrenIndex}
                            policyIndex={policyIndex}
                            policiesLoading={policiesLoading}
                            showSource={true}
                            onOpenEntity={setOpenEntityKey}
                            onDelete={handleDelete}
                            emptyState={
                                <Empty>
                                    <EmptyHeader>
                                        <EmptyTitle>No resources yet</EmptyTitle>
                                        <EmptyDescription>Register resources via JSON import or SCIM sync.</EmptyDescription>
                                    </EmptyHeader>
                                </Empty>
                            }
                        />
                    </TabsContent>

                    {/* entities.json tab */}
                    <TabsContent value="json">
                        <div className="overflow-hidden rounded-xl border bg-card">
                            <div className="flex items-center justify-between border-b bg-muted/50 px-4 py-2">
                                <div className="flex items-center gap-2 text-sm font-mono">
                                    <Braces className="size-4 text-muted-foreground" />
                                    entities.json
                                    <Badge variant="outline" className="ml-2 text-[10px]">
                                        Read-only snapshot
                                    </Badge>
                                </div>
                                <div className="text-xs text-muted-foreground">{allEntities.length} entities</div>
                            </div>
                            {allEntities.length === 0 ? (
                                <div className="flex items-center justify-center p-10 text-sm text-muted-foreground">No entities yet.</div>
                            ) : (
                                <div className="max-h-[calc(100vh-26rem)] overflow-auto bg-background">
                                    <pre className="px-0 py-2 font-mono text-[12px] leading-5">
                                        {jsonLines.map((line, i) => (
                                            <div key={i} className={cn('flex gap-4 px-4 hover:bg-accent/40')}>
                                                <span className="w-10 shrink-0 select-none text-right tabular-nums text-muted-foreground/40">
                                                    {i + 1}
                                                </span>
                                                <code className="flex-1 whitespace-pre">{highlightJsonLine(line.text)}</code>
                                            </div>
                                        ))}
                                    </pre>
                                </div>
                            )}
                        </div>
                    </TabsContent>
                </Tabs>
            )}

            {/* Pagination — shown under the active tab list */}
            {!isLoading && !error && data && data.total > perPage && (
                <EntitiesPagination page={page} perPage={perPage} total={data.total} onPageChange={setPage} />
            )}

            {/* Dialogs */}
            <PrincipalImportDialog open={jsonImportOpen} onOpenChange={setJsonImportOpen} create={create} onImported={() => reload()} />
            <ScimConnectorsDialog open={scimConnectorsOpen} onOpenChange={setScimConnectorsOpen} onChanged={() => reload()} />

            <AddLocalPrincipalDialog open={addPrincipalOpen} onOpenChange={setAddPrincipalOpen} create={create} allEntities={allEntities} />

            <EntityDetailSheet
                open={openEntityKey !== null}
                onOpenChange={next => {
                    if (!next) setOpenEntityKey(null);
                }}
                entityKey={openEntityKey}
                allEntities={allEntities}
                remove={remove}
            />
        </div>
    );
}
