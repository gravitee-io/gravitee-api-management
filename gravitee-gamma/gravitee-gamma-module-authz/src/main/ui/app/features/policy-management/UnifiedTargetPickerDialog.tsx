/**
 * Reusable target picker for the policy-create flow. Same component on
 * every service page (MCP / LLM / API …); the only thing that changes
 * across types is the data filter — the parent passes the entityId
 * prefix it wants to surface and the dialog renders just those rows.
 * Replaces the two earlier per-shape pickers (TargetPickerDialog +
 * AiModelTargetPickerDialog) with one component the parent configures
 * via props.
 */
import { Badge, Button, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, Input, cn } from '@gravitee/graphene-core';
import { Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { EntityResponse, PolicyType } from '../../../lib/api/authz-api.types';

export interface UnifiedTarget {
    readonly id: string;
    readonly name: string;
    readonly type: PolicyType;
    readonly description: string;
}

export interface UnifiedTargetPickerDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly entities: readonly EntityResponse[];
    readonly existingTargetIds: readonly string[];
    /** Service type the picker is bound to (e.g. 'MCP'). Drives the entityId
     *  prefix filter so the dialog only surfaces resources of this type. */
    readonly serviceType: PolicyType;
    /** Display label for the service kind, used in the header copy. */
    readonly serviceLabel: string;
    readonly onSelect: (target: UnifiedTarget) => void;
}

function entityToTarget(entity: EntityResponse, prefix: string, type: PolicyType): UnifiedTarget | undefined {
    if (!entity.uid.startsWith(prefix)) return undefined;
    const rest = entity.uid.slice(prefix.length);
    // Sub-segments (e.g. mcp.bookings.get-booking) belong to the in-editor
    // resource chip picker, not to the top-level target list.
    if (rest.includes('.')) return undefined;
    const name = (entity.attributes?.apiName as string) || (entity.attributes?.name as string) || rest;
    const description = (entity.attributes?.description as string) || '';
    return { id: entity.uid, name, type, description };
}

export function UnifiedTargetPickerDialog({
    open,
    onOpenChange,
    entities,
    existingTargetIds,
    serviceType,
    serviceLabel,
    onSelect,
}: UnifiedTargetPickerDialogProps) {
    const [query, setQuery] = useState('');
    const [selectedId, setSelectedId] = useState<string | null>(null);

    const existingSet = useMemo(() => new Set(existingTargetIds), [existingTargetIds]);
    const prefix = serviceType.toLowerCase() + '.';

    const targets = useMemo(() => {
        const flat: UnifiedTarget[] = [];
        for (const e of entities) {
            const t = entityToTarget(e, prefix, serviceType);
            if (t) flat.push(t);
        }
        flat.sort((a, b) => a.name.localeCompare(b.name));
        return flat;
    }, [entities, prefix, serviceType]);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        return targets.filter(t => {
            if (existingSet.has(t.id)) return false;
            if (!q) return true;
            return t.name.toLowerCase().includes(q) || t.id.toLowerCase().includes(q) || t.description.toLowerCase().includes(q);
        });
    }, [targets, query, existingSet]);

    const handleContinue = () => {
        if (!selectedId) return;
        const t = targets.find(x => x.id === selectedId);
        if (t) onSelect(t);
    };

    const handleOpenChange = (next: boolean) => {
        onOpenChange(next);
        if (!next) {
            setQuery('');
            setSelectedId(null);
        }
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Create {serviceLabel} policy</DialogTitle>
                    <DialogDescription>
                        Pick a {serviceLabel} to secure. Resources that already have a policy are hidden.
                    </DialogDescription>
                </DialogHeader>

                <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                        placeholder={`Search ${serviceLabel.toLowerCase()}s by name, id or description…`}
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        className="pl-8"
                    />
                </div>

                <div className="max-h-[400px] space-y-1 overflow-auto rounded-md border bg-muted/20 p-2">
                    {filtered.length === 0 ? (
                        <div className="px-2 py-6 text-center text-xs text-muted-foreground">
                            {targets.length === 0
                                ? `No ${serviceLabel.toLowerCase()}s yet. Create one in APIM Console first or in Authorization → Entities.`
                                : `Every ${serviceLabel.toLowerCase()} already has a policy.`}
                        </div>
                    ) : (
                        filtered.map(t => {
                            const active = selectedId === t.id;
                            return (
                                <button
                                    key={t.id}
                                    type="button"
                                    onClick={() => setSelectedId(t.id)}
                                    className={cn(
                                        'flex w-full items-center gap-3 rounded-md border bg-card px-2.5 py-1.5 text-left text-xs transition-colors',
                                        active ? 'border-primary bg-primary/5' : 'border-input hover:bg-accent',
                                    )}
                                >
                                    <Badge variant="outline" className="font-mono text-[10px]">
                                        {serviceLabel}
                                    </Badge>
                                    <div className="min-w-0 flex-1">
                                        <div className="truncate font-medium">{t.name}</div>
                                        <code className="block truncate text-[10px] text-muted-foreground">{t.id}</code>
                                        {t.description ? (
                                            <div className="truncate text-[10px] text-muted-foreground/80">{t.description}</div>
                                        ) : null}
                                    </div>
                                </button>
                            );
                        })
                    )}
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => handleOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button onClick={handleContinue} disabled={!selectedId}>
                        Continue
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
