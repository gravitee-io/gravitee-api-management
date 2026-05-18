/**
 * Entity detail side-sheet.
 * Ported from prototype entity-detail-sheet.tsx, adapted to use backend EntityInstance.
 */
import {
    Badge,
    Button,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
    cn,
} from '@gravitee/graphene-core';
import {
    ArrowLeft,
    Bot,
    Braces,
    Building2,
    ChevronRight,
    CircleDot,
    Clock,
    Copy,
    Shield,
    Tag,
    Users,
    UserCircle,
    X,
    Zap,
} from 'lucide-react';
import { useEffect, useMemo, useState, type CSSProperties, type ComponentType, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/Tabs';
import { useEnvironment } from '../../lib/env/EnvironmentContext';
import { entityKeyOf } from '../../../lib/entity-adapter';
import { parseGaplSchema, type ParsedEntity } from '../../../lib/gapl-parser';
import type { UseEntitiesResult } from '../../../lib/hooks/useEntities';
import { useSchema } from '../../../lib/hooks/useSchema';
import type { AttrValue, EntityInstance } from './entity-types';
import { getEntityCategoryId } from './entity-types';

// ---------- Types -------------------------------------------------------------

export interface EntityDetailSheetProps {
    open: boolean;
    onOpenChange: (next: boolean) => void;
    entityKey: string | null;
    allEntities: EntityInstance[];
    remove: UseEntitiesResult['remove'];
}

// ---------- Helpers ----------------------------------------------------------

function formatIso(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') return '';
    try {
        // Backend serialises Instant as epoch seconds (with fractional part).
        const d = typeof value === 'number' ? new Date(value * 1000) : new Date(value);
        if (Number.isNaN(d.getTime())) return String(value);
        return d.toLocaleString(undefined, {
            year: 'numeric',
            month: 'short',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        });
    } catch {
        return String(value);
    }
}

function inferAttrType(value: AttrValue): string {
    if (typeof value === 'boolean') return 'Bool';
    if (typeof value === 'number') return Number.isInteger(value) ? 'Long' : 'Decimal';
    return 'String';
}

function formatGaplValue(value: AttrValue): string {
    if (typeof value === 'string') return `"${value.replace(/"/g, '\\"')}"`;
    if (typeof value === 'boolean') return value ? 'true' : 'false';
    return String(value);
}

function pluralizeType(type: string, n: number): string {
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
    const [s, p] = map[type] ?? [type, `${type}s`];
    return n === 1 ? s : p;
}

function parseEntityKey(key: string): { type: string; id: string } | null {
    const sep = key.indexOf('::');
    if (sep < 0) return null;
    return { type: key.slice(0, sep), id: key.slice(sep + 2) };
}

// ---------- JSON line rendering ----------------------------------------------

interface JsonLine {
    text: string;
}

function buildEntityJson(entity: EntityInstance): JsonLine[] {
    const lines: JsonLine[] = [];
    const push = (text: string) => lines.push({ text });

    push('{');
    push(`  "uid": {`);
    push(`    "type": ${formatGaplValue(entity.uid.type)},`);
    push(`    "id":   ${formatGaplValue(entity.uid.id)}`);
    push(`  },`);

    const attrEntries = Object.entries(entity.attrs);
    if (attrEntries.length === 0) {
        push(`  "attrs": {},`);
    } else {
        push(`  "attrs": {`);
        attrEntries.forEach(([k, v], i) => {
            const suffix = i === attrEntries.length - 1 ? '' : ',';
            push(`    ${formatGaplValue(k)}: ${formatGaplValue(v)}${suffix}`);
        });
        push(`  },`);
    }

    if (entity.parents.length === 0) {
        push(`  "parents": []`);
    } else {
        push(`  "parents": [`);
        entity.parents.forEach((p, i) => {
            const suffix = i === entity.parents.length - 1 ? '' : ',';
            push(`    {`);
            push(`      "type": ${formatGaplValue(p.type)},`);
            push(`      "id":   ${formatGaplValue(p.id)}`);
            push(`    }${suffix}`);
        });
        push(`  ]`);
    }
    push('}');
    return lines;
}

function highlightJsonLine(text: string): ReactNode {
    const tokenRe = /("(?:\\"|[^"])*"\s*:)|("(?:\\"|[^"])*")|(-?\d+(?:\.\d+)?)|\b(true|false|null)\b|([\[\]{},:])|(\s+)|(.)/g;
    const nodes: ReactNode[] = [];
    let m: RegExpExecArray | null;
    let key = 0;
    while ((m = tokenRe.exec(text)) !== null) {
        const [, keyTok, strTok, numTok, boolTok, puncTok, wsTok, rawTok] = m;
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

// ---------- Sub-components ---------------------------------------------------

function Section({
    title,
    icon,
    description,
    action,
    children,
}: {
    title: string;
    icon?: ReactNode;
    description?: string;
    action?: ReactNode;
    children: ReactNode;
}) {
    return (
        <section className="space-y-2">
            <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-1.5 text-sm font-medium">
                    {icon ? <span className="text-muted-foreground">{icon}</span> : null}
                    {title}
                </div>
                {action}
            </div>
            {description ? <p className="text-xs text-muted-foreground">{description}</p> : null}
            {children}
        </section>
    );
}

function EmptyBlock({ children }: { children: ReactNode }) {
    return <div className="rounded-md border border-dashed bg-muted/30 px-3 py-4 text-xs text-muted-foreground">{children}</div>;
}

function CategoryPill({ category }: { category: string | undefined }) {
    if (!category) return null;
    const tone: Record<string, string> = {
        principal: 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-900/40 dark:bg-blue-950/30 dark:text-blue-300',
        mcp: 'border-teal-200 bg-teal-50 text-teal-700 dark:border-teal-900/40 dark:bg-teal-950/30 dark:text-teal-300',
        api: 'border-indigo-200 bg-indigo-50 text-indigo-700 dark:border-indigo-900/40 dark:bg-indigo-950/30 dark:text-indigo-300',
        agent: 'border-orange-200 bg-orange-50 text-orange-700 dark:border-orange-900/40 dark:bg-orange-950/30 dark:text-orange-300',
        llm: 'border-fuchsia-200 bg-fuchsia-50 text-fuchsia-700 dark:border-fuchsia-900/40 dark:bg-fuchsia-950/30 dark:text-fuchsia-300',
        event: 'border-cyan-200 bg-cyan-50 text-cyan-700 dark:border-cyan-900/40 dark:bg-cyan-950/30 dark:text-cyan-300',
        custom: 'border-slate-200 bg-slate-50 text-slate-700 dark:border-slate-900/40 dark:bg-slate-950/30 dark:text-slate-300',
    };
    const label: Record<string, string> = {
        principal: 'Principal',
        mcp: 'MCP',
        api: 'API',
        agent: 'Agent',
        llm: 'LLM',
        event: 'Event',
        custom: 'Custom',
    };
    return (
        <span
            className={cn(
                'inline-flex items-center rounded-md border px-1.5 py-0.5 text-[10.5px] font-medium',
                tone[category] ?? 'bg-muted',
            )}
        >
            {label[category] ?? category}
        </span>
    );
}

function SourceBadge({ entity }: { entity: EntityInstance }) {
    if (entity.source === 'scim') {
        const provider = entity.principalProvider ?? 'IdP';
        return (
            <Badge variant="secondary" className="gap-1">
                <Shield className="size-3" />
                SCIM · {provider}
            </Badge>
        );
    }
    if (entity.source === 'directory') {
        const provider = entity.principalProvider ?? 'User Directory';
        return (
            <Badge variant="secondary" className="gap-1">
                <Building2 className="size-3" />
                {provider}
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="gap-1">
            <CircleDot className="size-3" />
            Local
        </Badge>
    );
}

function AttributeTable({
    attrs,
    attrTypeMap,
    schemaAttrs,
    onCopy,
}: {
    attrs: Record<string, AttrValue>;
    attrTypeMap: Map<string, string>;
    schemaAttrs: string[];
    onCopy: (v: string) => void;
}) {
    const presentKeys = Object.keys(attrs);
    const missingFromSchema = schemaAttrs.filter(k => !(k in attrs));

    if (presentKeys.length === 0 && schemaAttrs.length === 0) {
        return <EmptyBlock>This entity has no attributes.</EmptyBlock>;
    }

    return (
        <div className="overflow-hidden rounded-md border">
            <table className="w-full text-sm">
                <thead className="bg-muted/40 text-[11px] font-medium text-muted-foreground">
                    <tr>
                        <th className="w-[160px] px-3 py-1.5 text-left">Name</th>
                        <th className="w-[100px] px-3 py-1.5 text-left">Type</th>
                        <th className="px-3 py-1.5 text-left">Value</th>
                    </tr>
                </thead>
                <tbody className="divide-y">
                    {presentKeys.map(k => {
                        const v = attrs[k];
                        const schemaType = attrTypeMap.get(k);
                        const actualType = inferAttrType(v);
                        const type = schemaType ?? actualType;
                        return (
                            <tr key={k} className="group">
                                <td className="px-3 py-1.5 align-top font-mono text-[12px]">{k}</td>
                                <td className="px-3 py-1.5 align-top text-[11px] text-muted-foreground">{type}</td>
                                <td className="px-3 py-1.5 align-top">
                                    <div className="flex items-start gap-2">
                                        <code className="break-all font-mono text-[12px] text-foreground">{formatGaplValue(v)}</code>
                                        <button
                                            type="button"
                                            onClick={() => onCopy(String(v))}
                                            className="invisible ml-auto rounded p-1 text-muted-foreground hover:bg-accent group-hover:visible"
                                            aria-label={`Copy value of ${k}`}
                                        >
                                            <Copy className="size-3" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                    {missingFromSchema.map(k => (
                        <tr key={`missing-${k}`} className="text-muted-foreground">
                            <td className="px-3 py-1.5 font-mono text-[12px]">{k}</td>
                            <td className="px-3 py-1.5 text-[11px]">{attrTypeMap.get(k)}</td>
                            <td className="px-3 py-1.5 text-[11px] italic">
                                not set
                                <span className="ml-2 text-[10px] text-muted-foreground/80">(declared by schema)</span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

function RelationRow({
    type,
    id,
    onNavigate,
    entities,
}: {
    type: string;
    id: string;
    onNavigate: (key: string) => void;
    entities: EntityInstance[];
}) {
    const resolved = entities.find(e => e.uid.type === type && e.uid.id === id);
    const displayName = resolved ? (resolved.displayName ?? (resolved.attrs.name as string | undefined) ?? resolved.uid.id) : id;
    const key = entityKeyOf({ type, id });
    const dangling = !resolved;
    return (
        <li>
            <button
                type="button"
                onClick={() => (!dangling ? onNavigate(key) : undefined)}
                disabled={dangling}
                className={cn(
                    'flex w-full items-center gap-3 rounded-md border px-3 py-2 text-left',
                    dangling ? 'cursor-not-allowed opacity-60' : 'hover:bg-accent',
                )}
            >
                <Badge variant="outline" className="font-mono text-[10px]">
                    {type}
                </Badge>
                <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">{displayName}</span>
                    <span className="block truncate font-mono text-[10.5px] text-muted-foreground">{id}</span>
                </span>
                {dangling ? (
                    <Badge variant="destructive" className="h-5 text-[10px]">
                        dangling
                    </Badge>
                ) : (
                    <ChevronRight className="size-4 text-muted-foreground" />
                )}
            </button>
        </li>
    );
}

// ---------- Component --------------------------------------------------------

// ---------- Modal shell styles (parity with ScimConnectorsDialog) -----------

const overlay: CSSProperties = {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(15, 23, 42, 0.55)',
    backdropFilter: 'blur(2px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem',
    zIndex: 10000,
    animation: 'sc-fadein 120ms ease-out',
};
const panel: CSSProperties = {
    backgroundColor: '#fff',
    borderRadius: 12,
    width: 'min(880px, 100%)',
    maxHeight: '88vh',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
    boxShadow: '0 25px 70px -10px rgba(15,23,42,0.35), 0 10px 25px -5px rgba(15,23,42,0.18)',
    animation: 'sc-pop 160ms cubic-bezier(.2,.8,.2,1)',
};
const headerBar: CSSProperties = {
    padding: '1.25rem 1.5rem 1rem',
    borderBottom: '1px solid #e5e7eb',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '0.75rem',
};
const headerIcon: CSSProperties = {
    flex: 'none',
    width: 36,
    height: 36,
    borderRadius: 8,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
};
const closeBtn: CSSProperties = {
    border: 'none',
    background: 'transparent',
    color: '#6b7280',
    cursor: 'pointer',
    padding: 6,
    borderRadius: 6,
};
const body: CSSProperties = { padding: 0, overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column' };
const footerBar: CSSProperties = {
    padding: '0.875rem 1.5rem',
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'space-between',
    gap: '0.5rem',
    backgroundColor: '#fafafa',
    alignItems: 'center',
};

/** Choose icon + tint based on the entity kind so the header matches the rest of the app. */
function headerIconForType(type: string): { Icon: ComponentType<{ size?: number }>; bg: string; color: string } {
    if (type === 'User') return { Icon: UserCircle, bg: '#dcfce7', color: '#15803d' };
    if (type === 'Group') return { Icon: Users, bg: '#dbeafe', color: '#1d4ed8' };
    if (type === 'Action') return { Icon: Zap, bg: '#fef3c7', color: '#b45309' };
    if (type === 'AgentIdentity') return { Icon: Bot, bg: '#fce7f3', color: '#9d174d' };
    if (type === 'ServiceAccount') return { Icon: Shield, bg: '#ede9fe', color: '#5b21b6' };
    // Resource / API / MCP* / fallback
    return { Icon: Building2, bg: '#eef2ff', color: '#4338ca' };
}

export function EntityDetailSheet({ open, onOpenChange, entityKey, allEntities, remove }: EntityDetailSheetProps) {
    const [stack, setStack] = useState<string[]>([]);

    useEffect(() => {
        // Sync the stack with open/entityKey props. Lifting `stack` into
        // the parent or deriving it from props would avoid the synchronous
        // setState here — tracked as tech debt.
        // eslint-disable-next-line react-hooks/set-state-in-effect
        if (open && entityKey) setStack([entityKey]);
        if (!open) setStack([]);
    }, [open, entityKey]);

    const currentKey = stack[stack.length - 1];
    const entity = useMemo(() => {
        if (!currentKey) return null;
        const uid = parseEntityKey(currentKey);
        if (!uid) return null;
        return allEntities.find(e => e.uid.type === uid.type && e.uid.id === uid.id) ?? null;
    }, [currentKey, allEntities]);

    const childrenRefs = useMemo(() => {
        if (!entity) return [] as Array<{ type: string; items: EntityInstance[] }>;
        const byType = new Map<string, EntityInstance[]>();
        for (const e of allEntities) {
            if (e.parents.some(p => p.type === entity.uid.type && p.id === entity.uid.id)) {
                const arr = byType.get(e.uid.type) ?? [];
                arr.push(e);
                byType.set(e.uid.type, arr);
            }
        }
        return Array.from(byType.entries()).map(([type, items]) => ({ type, items }));
    }, [entity, allEntities]);

    const navigateTo = (key: string) => setStack(s => [...s, key]);
    const goBack = () => setStack(s => (s.length > 1 ? s.slice(0, -1) : s));

    // Pull the authoritative schema from the server. The shape of each entity
    // (attribute list + types) comes from parsing the stored GAPL `schemaText` —
    // not from a hardcoded UI table. If no schema is set on this env, schemaDef
    // is undefined and the attribute table renders only what's present on the
    // entity record (no "not set (declared by schema)" rows).
    const environmentId = useEnvironment();
    const { schema } = useSchema(environmentId);
    const parsedSchema = useMemo(
        () => (schema?.schemaText ? parseGaplSchema(schema.schemaText) : null),
        [schema?.schemaText],
    );
    const schemaDef: ParsedEntity | undefined = entity && parsedSchema
        ? parsedSchema.entities.find(e => e.name === entity.uid.type)
        : undefined;
    const attrTypeMap = new Map<string, string>();
    if (schemaDef) for (const a of schemaDef.attributes) attrTypeMap.set(a.name, a.type);

    const copy = (value: string) => {
        if (typeof navigator !== 'undefined' && navigator.clipboard) {
            navigator.clipboard.writeText(value).catch(() => void 0);
        }
    };

    const handleDelete = async () => {
        if (!entity?._backendId) return;
        if (!window.confirm(`Delete entity "${entity.uid.type}::${entity.uid.id}"?`)) return;
        try {
            await remove(entity._backendId);
            onOpenChange(false);
        } catch {
            // Error shown externally
        }
    };

    const parentCount = entity?.parents.length ?? 0;
    const childCount = childrenRefs.reduce((n, c) => n + c.items.length, 0);

    // Body scroll lock + Esc handling while the modal is open.
    useEffect(() => {
        if (!open) return;
        const prev = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                // Esc first pops navigation stack (drill-into-related), then closes the modal.
                if (stack.length > 1) {
                    goBack();
                    e.stopPropagation();
                } else {
                    onOpenChange(false);
                }
            }
        };
        document.addEventListener('keydown', onKey);
        return () => {
            document.body.style.overflow = prev;
            document.removeEventListener('keydown', onKey);
        };
    }, [open, onOpenChange, stack.length]);

    if (!open) return null;

    const titleText = entity ? entity.displayName ?? (entity.attrs.name as string | undefined) ?? entity.uid.id : 'Entity';
    const { Icon: HeaderIcon, bg: iconBg, color: iconColor } = headerIconForType(entity?.uid.type ?? '');

    return createPortal(
        <>
            <style>{`
                @keyframes sc-fadein { from { opacity: 0; } to { opacity: 1; } }
                @keyframes sc-pop {
                    from { opacity: 0; transform: translateY(8px) scale(0.98); }
                    to   { opacity: 1; transform: translateY(0) scale(1); }
                }
                .ed-close:hover { background: #f3f4f6; color: #111827; }
                .ed-back:hover { background: rgba(255,255,255,0.6); }
            `}</style>
            <div role="presentation" onClick={() => onOpenChange(false)} style={overlay}>
                <div
                    role="dialog"
                    aria-modal="true"
                    aria-label={titleText}
                    onClick={e => e.stopPropagation()}
                    style={panel}
                >
                    {entity ? (
                        <>
                            {/* Header */}
                            <div style={headerBar}>
                                {stack.length > 1 ? (
                                    <button
                                        type="button"
                                        onClick={goBack}
                                        className="ed-back"
                                        style={{ ...headerIcon, cursor: 'pointer', border: 'none', backgroundColor: '#f1f5f9', color: '#475569' }}
                                        aria-label="Back"
                                        title="Back"
                                    >
                                        <ArrowLeft size={18} />
                                    </button>
                                ) : (
                                    <div style={{ ...headerIcon, backgroundColor: iconBg, color: iconColor }}>
                                        <HeaderIcon size={18} />
                                    </div>
                                )}
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 4 }}>
                                        <Badge variant="outline" className="font-mono text-[11px]">
                                            {entity.uid.type}
                                        </Badge>
                                        <CategoryPill category={getEntityCategoryId(entity.uid.type)} />
                                        <span style={{ flex: 1 }} />
                                        <SourceBadge entity={entity} />
                                    </div>
                                    <h2
                                        style={{
                                            margin: 0,
                                            fontSize: 18,
                                            fontWeight: 600,
                                            color: '#0f172a',
                                            lineHeight: 1.3,
                                            whiteSpace: 'nowrap',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                        }}
                                    >
                                        {titleText}
                                    </h2>
                                    <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8, marginTop: 6 }}>
                                        {entity.createdAt ? (
                                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 11, color: '#6b7280' }}>
                                                <Clock size={12} />
                                                Created {formatIso(entity.createdAt)}
                                            </span>
                                        ) : null}
                                        {entity.updatedAt && entity.updatedAt !== entity.createdAt ? (
                                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 11, color: '#6b7280' }}>
                                                <Clock size={12} />
                                                Updated {formatIso(entity.updatedAt)}
                                            </span>
                                        ) : null}
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => onOpenChange(false)}
                                    className="ed-close"
                                    style={closeBtn}
                                    aria-label="Close"
                                    title="Close"
                                >
                                    <X size={18} />
                                </button>
                            </div>

                            {/* Body */}
                            <div style={body}>
                            <Tabs defaultValue="overview" className="flex h-full flex-col">
                                <div className="border-b px-5">
                                    <TabsList>
                                        <TabsTrigger value="overview">Overview</TabsTrigger>
                                        <TabsTrigger value="relationships">
                                            Relationships
                                            <Badge variant="secondary" className="ml-2 h-4 px-1 text-[10px]">
                                                {parentCount + childCount}
                                            </Badge>
                                        </TabsTrigger>
                                        <TabsTrigger value="shape">GAPL shape</TabsTrigger>
                                    </TabsList>
                                </div>

                                {/* Overview */}
                                <TabsContent value="overview" className="m-0 space-y-4 p-5">
                                    <Section
                                        title="Attributes"
                                        icon={<Tag className="size-4" />}
                                        description={
                                            schemaDef
                                                ? `Aligned with the ${schemaDef.name} entity defined in the GAPL schema.`
                                                : 'Attributes stored on the entity record.'
                                        }
                                    >
                                        <AttributeTable
                                            attrs={entity.attrs}
                                            attrTypeMap={attrTypeMap}
                                            schemaAttrs={schemaDef?.attributes.map(a => a.name) ?? []}
                                            onCopy={copy}
                                        />
                                    </Section>

                                    {/* Provenance moved to the header (Source/Provider badge + Created/Updated chips). */}
                                </TabsContent>

                                {/* Relationships */}
                                <TabsContent value="relationships" className="m-0 space-y-5 p-5">
                                    <Section
                                        title={`Member of (${parentCount})`}
                                        icon={<ChevronRight className="size-4 rotate-180" />}
                                        description="Parent entities this entity belongs to. Follows the GAPL `in [...]` relation."
                                    >
                                        {entity.parents.length === 0 ? (
                                            <EmptyBlock>No parent relations.</EmptyBlock>
                                        ) : (
                                            <ul className="space-y-1.5">
                                                {entity.parents.map(p => (
                                                    <RelationRow
                                                        key={`${p.type}:${p.id}`}
                                                        type={p.type}
                                                        id={p.id}
                                                        onNavigate={navigateTo}
                                                        entities={allEntities}
                                                    />
                                                ))}
                                            </ul>
                                        )}
                                    </Section>

                                    <Section
                                        title={`Referenced by (${childCount})`}
                                        icon={<ChevronRight className="size-4" />}
                                        description="Entities that declare this entity as a parent."
                                    >
                                        {childrenRefs.length === 0 ? (
                                            <EmptyBlock>No entities reference this one.</EmptyBlock>
                                        ) : (
                                            <div className="space-y-3">
                                                {childrenRefs.map(({ type, items }) => (
                                                    <div key={type} className="space-y-1.5">
                                                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                                            <Badge variant="outline" className="font-mono text-[10px]">
                                                                {type}
                                                            </Badge>
                                                            <span>
                                                                {items.length} {pluralizeType(type, items.length)}
                                                            </span>
                                                        </div>
                                                        <ul className="space-y-1.5">
                                                            {items.map(child => (
                                                                <RelationRow
                                                                    key={`${child.uid.type}:${child.uid.id}`}
                                                                    type={child.uid.type}
                                                                    id={child.uid.id}
                                                                    onNavigate={navigateTo}
                                                                    entities={allEntities}
                                                                />
                                                            ))}
                                                        </ul>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </Section>
                                </TabsContent>

                                {/* GAPL shape */}
                                <TabsContent value="shape" className="m-0 space-y-3 p-5">
                                    <Section
                                        title="GAPL entity"
                                        icon={<Braces className="size-4" />}
                                        description="The canonical document the Policy Decision Point evaluates against."
                                        action={
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                className="h-7 gap-1.5 text-xs"
                                                onClick={() =>
                                                    copy(
                                                        JSON.stringify(
                                                            { uid: entity.uid, attrs: entity.attrs, parents: entity.parents },
                                                            null,
                                                            2,
                                                        ),
                                                    )
                                                }
                                            >
                                                <Copy className="size-3" />
                                                Copy JSON
                                            </Button>
                                        }
                                    >
                                        <div className="overflow-hidden rounded-md border bg-muted/20">
                                            <div className="flex items-center justify-between border-b bg-muted/40 px-3 py-1.5 text-[11px] font-mono text-muted-foreground">
                                                <span>
                                                    <span className="text-amber-600 dark:text-amber-400">
                                                        &quot;{entity.uid.type}&quot;
                                                    </span>
                                                    ::entity
                                                </span>
                                                <span>entity.gapl.json</span>
                                            </div>
                                            <pre className="overflow-auto py-2 font-mono text-[12px] leading-5">
                                                {buildEntityJson(entity).map((line, i) => (
                                                    <div key={i} className="flex gap-4 px-4 hover:bg-accent/40">
                                                        <span className="w-6 shrink-0 select-none text-right tabular-nums text-muted-foreground/40">
                                                            {i + 1}
                                                        </span>
                                                        <code className="whitespace-pre">{highlightJsonLine(line.text)}</code>
                                                    </div>
                                                ))}
                                            </pre>
                                        </div>
                                    </Section>
                                </TabsContent>
                            </Tabs>
                            </div>

                            {/* Footer: destructive action for editable principals only */}
                            {entity!._backendId && entity!.source === 'local' ? (
                                <div style={footerBar}>
                                    <span style={{ fontSize: 11, color: '#9ca3af' }}>
                                        Backend id{' '}
                                        <code style={{ fontFamily: 'ui-monospace, SFMono-Regular, monospace' }}>{entity!._backendId}</code>
                                    </span>
                                    <Button variant="outline" onClick={handleDelete} style={{ color: '#b91c1c', borderColor: '#fecaca' }}>
                                        Delete entity
                                    </Button>
                                </div>
                            ) : null}
                        </>
                    ) : (
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '3rem 2rem', fontSize: 14, color: '#6b7280' }}>
                            Entity not found.
                        </div>
                    )}
                </div>
            </div>
        </>,
        document.body,
    );
}
