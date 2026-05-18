import {
    Badge,
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    cn,
} from '@gravitee/graphene-core';
import { Search, ShieldCheck } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { CatalogEntry } from '../../../lib/api/authz-api.types';

export interface TargetPickerDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly catalog: readonly CatalogEntry[];
    readonly existingTargetIds: readonly string[];
    readonly title: string;
    readonly description: string;
    readonly emptyState?: string;
    readonly searchPlaceholder?: string;
    readonly onSelect: (entry: CatalogEntry) => void;
}

export function TargetPickerDialog({
    open,
    onOpenChange,
    catalog,
    existingTargetIds,
    title,
    description,
    emptyState = 'Every catalog entry already has a policy. Edit an existing one from the list.',
    searchPlaceholder = 'Search catalog…',
    onSelect,
}: TargetPickerDialogProps) {
    const [query, setQuery] = useState('');
    const [selectedId, setSelectedId] = useState<string | null>(null);

    const existingSet = useMemo(() => new Set(existingTargetIds), [existingTargetIds]);

    const available = useMemo(() => {
        const q = query.trim().toLowerCase();
        return catalog
            .filter(item => !existingSet.has(item.id))
            .filter(item => !q || item.name.toLowerCase().includes(q) || item.description.toLowerCase().includes(q));
    }, [catalog, existingSet, query]);

    const handleContinue = () => {
        if (!selectedId) return;
        const item = catalog.find(c => c.id === selectedId);
        if (item) onSelect(item);
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
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>{description}</DialogDescription>
                </DialogHeader>

                <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                        placeholder={searchPlaceholder}
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        className="pl-8"
                        aria-label="Search catalog"
                    />
                </div>

                <div className="max-h-[420px] overflow-y-auto rounded-md border">
                    {available.length === 0 ? (
                        <div className="p-6 text-center text-sm text-muted-foreground">{emptyState}</div>
                    ) : (
                        <ul className="divide-y">
                            {available.map(item => {
                                const selected = selectedId === item.id;
                                const toolCount = item.subResources.filter(s => s.kind === 'tool' || s.kind === 'MCPTool').length;
                                const promptCount = item.subResources.filter(s => s.kind === 'prompt' || s.kind === 'MCPPrompt').length;
                                const resourceCount = item.subResources.filter(
                                    s => s.kind === 'resource' || s.kind === 'MCPResource',
                                ).length;
                                const otherCounts = countSubResourcesByKind(item.subResources);

                                return (
                                    <li key={item.id}>
                                        <button
                                            type="button"
                                            onClick={() => setSelectedId(item.id)}
                                            className={cn(
                                                'flex w-full items-start gap-3 px-4 py-3 text-left transition hover:bg-muted/40',
                                                selected && 'bg-primary/5',
                                            )}
                                        >
                                            <div className="min-w-0 flex-1">
                                                <div className="flex items-center gap-2">
                                                    <p className="truncate text-sm font-medium">{item.name}</p>
                                                    {item.badges?.map(b => (
                                                        <Badge
                                                            key={b}
                                                            variant="outline"
                                                            className="h-4 px-1.5 capitalize"
                                                            style={{ fontSize: '10px' }}
                                                        >
                                                            {b}
                                                        </Badge>
                                                    ))}
                                                </div>
                                                {item.description ? (
                                                    <p className="truncate text-xs text-muted-foreground">{item.description}</p>
                                                ) : null}
                                                {item.subResources.length > 0 ? (
                                                    <div
                                                        className="mt-1 flex flex-wrap gap-2 text-muted-foreground"
                                                        style={{ fontSize: '11px' }}
                                                    >
                                                        {formatSubResourceCounts(toolCount, promptCount, resourceCount, otherCounts)}
                                                    </div>
                                                ) : null}
                                            </div>
                                            {selected ? <ShieldCheck className="mt-1 size-4 text-primary" /> : null}
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
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

function countSubResourcesByKind(subResources: readonly { kind: string }[]): Record<string, number> {
    const counts: Record<string, number> = {};
    for (const s of subResources) {
        counts[s.kind] = (counts[s.kind] ?? 0) + 1;
    }
    return counts;
}

function formatSubResourceCounts(
    toolCount: number,
    promptCount: number,
    resourceCount: number,
    otherCounts: Record<string, number>,
): string {
    const parts: string[] = [];
    if (toolCount > 0) parts.push(`${toolCount} ${toolCount === 1 ? 'tool' : 'tools'}`);
    if (promptCount > 0) parts.push(`${promptCount} ${promptCount === 1 ? 'prompt' : 'prompts'}`);
    if (resourceCount > 0) parts.push(`${resourceCount} ${resourceCount === 1 ? 'resource' : 'resources'}`);
    for (const [kind, count] of Object.entries(otherCounts)) {
        if (
            kind === 'tool' ||
            kind === 'MCPTool' ||
            kind === 'prompt' ||
            kind === 'MCPPrompt' ||
            kind === 'resource' ||
            kind === 'MCPResource'
        )
            continue;
        parts.push(`${count} ${kind.toLowerCase()}`);
    }
    return parts.join(' · ');
}
