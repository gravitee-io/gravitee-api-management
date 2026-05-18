import { useState } from 'react';
import type { ParsedAction, ParsedEntity, ParsedSchema } from '../../../lib/gapl-parser';
import { CATEGORIES, getEntityCategoryId } from './entity-types';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

export interface SchemaOutlineProps {
    parsed: ParsedSchema;
    activeLine?: number;
    onJump: (line: number) => void;
}

// ---------------------------------------------------------------------------
// Section collapse/expand state per category
// ---------------------------------------------------------------------------

function ChevronIcon({ open }: { open: boolean }) {
    return (
        <svg
            className={`size-3 shrink-0 text-muted-foreground transition-transform ${open ? 'rotate-90' : ''}`}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
        >
            <polyline points="9 18 15 12 9 6" />
        </svg>
    );
}

function DotIcon() {
    return (
        <svg className="size-2.5 shrink-0 text-muted-foreground/60" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
            <circle cx="12" cy="12" r="6" />
        </svg>
    );
}

// ---------------------------------------------------------------------------
// Entity row
// ---------------------------------------------------------------------------

function EntityRow({ entity, active, onJump }: { entity: ParsedEntity; active: boolean; onJump: (line: number) => void }) {
    const attrCount = entity.attributes.length;
    const parentCount = entity.parents.length;

    return (
        <li>
            <button
                type="button"
                onClick={() => onJump(entity.line)}
                className={`flex w-full items-center gap-2 rounded-md px-2 py-1 text-left text-xs font-mono text-foreground hover:bg-accent ${active ? 'bg-accent' : ''}`}
                aria-current={active ? 'true' : undefined}
            >
                <DotIcon />
                <span className="truncate">{entity.name}</span>
                <span className="ml-auto shrink-0 text-xs text-muted-foreground">
                    {attrCount > 0 && `${attrCount} attr${attrCount !== 1 ? 's' : ''}`}
                    {attrCount > 0 && parentCount > 0 && ' · '}
                    {parentCount > 0 && `in [${entity.parents.join(', ')}]`}
                </span>
            </button>
        </li>
    );
}

// ---------------------------------------------------------------------------
// Action row
// ---------------------------------------------------------------------------

function ActionRow({ action, active, onJump }: { action: ParsedAction; active: boolean; onJump: (line: number) => void }) {
    const count = action.principals.length + action.resources.length;

    return (
        <li>
            <button
                type="button"
                onClick={() => onJump(action.line)}
                className={`flex w-full items-center gap-2 rounded-md px-2 py-1 text-left text-xs font-mono text-foreground hover:bg-accent ${active ? 'bg-accent' : ''}`}
                aria-current={active ? 'true' : undefined}
            >
                <DotIcon />
                <span className="truncate">&quot;{action.name}&quot;</span>
                <span className="ml-auto shrink-0 text-xs text-muted-foreground">
                    {count} type{count !== 1 ? 's' : ''}
                </span>
            </button>
        </li>
    );
}

// ---------------------------------------------------------------------------
// Collapsible section (one per category + actions)
// ---------------------------------------------------------------------------

function OutlineSection({
    label,
    tone,
    count,
    children,
    defaultOpen = true,
}: {
    label: string;
    tone: string;
    count: number;
    children: React.ReactNode;
    defaultOpen?: boolean;
}) {
    const [open, setOpen] = useState(defaultOpen);
    if (count === 0) return null;

    return (
        <div className="px-2">
            <button
                type="button"
                className="flex w-full items-center gap-1.5 rounded-md px-2 py-1.5 text-left hover:bg-muted"
                onClick={() => setOpen(o => !o)}
            >
                <ChevronIcon open={open} />
                <span className={`text-xs font-semibold uppercase tracking-wide ${tone}`}>{label}</span>
                <span className="ml-auto text-xs text-muted-foreground">{count}</span>
            </button>
            {open && <ul className="mt-0.5 space-y-px pl-4">{children}</ul>}
        </div>
    );
}

// ---------------------------------------------------------------------------
// SchemaOutline
// ---------------------------------------------------------------------------

export function SchemaOutline({ parsed, activeLine, onJump }: SchemaOutlineProps) {
    const { entities, actions } = parsed;

    const hasContent = entities.length > 0 || actions.length > 0;

    return (
        <div className="flex flex-col gap-1 py-2">
            {!hasContent && <p className="px-4 py-2 text-xs text-muted-foreground">No entities or actions defined yet</p>}

            {CATEGORIES.map(cat => {
                const catEntities = entities.filter(e => getEntityCategoryId(e.name) === cat.id);
                if (catEntities.length === 0) return null;

                return (
                    <OutlineSection key={cat.id} label={cat.label} tone={cat.tone} count={catEntities.length}>
                        {catEntities.map(entity => (
                            <EntityRow key={entity.name} entity={entity} active={activeLine === entity.line} onJump={onJump} />
                        ))}
                    </OutlineSection>
                );
            })}

            {/* Entities not matched to any known category go to "Custom" fallback */}
            {(() => {
                const unknownEntities = entities.filter(e => getEntityCategoryId(e.name) === undefined);
                if (unknownEntities.length === 0) return null;
                return (
                    <OutlineSection label="Custom" tone="text-slate-600 dark:text-slate-400" count={unknownEntities.length}>
                        {unknownEntities.map(entity => (
                            <EntityRow key={entity.name} entity={entity} active={activeLine === entity.line} onJump={onJump} />
                        ))}
                    </OutlineSection>
                );
            })()}

            {actions.length > 0 && (
                <OutlineSection label="Actions" tone="text-yellow-600 dark:text-yellow-400" count={actions.length}>
                    {actions.map(action => (
                        <ActionRow key={action.name} action={action} active={activeLine === action.line} onJump={onJump} />
                    ))}
                </OutlineSection>
            )}
        </div>
    );
}
