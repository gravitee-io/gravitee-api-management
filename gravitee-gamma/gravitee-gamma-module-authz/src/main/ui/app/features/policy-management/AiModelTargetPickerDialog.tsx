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
import { Brain, Building2, ChevronDown, ChevronRight, Search, ShieldCheck } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import type { CatalogEntry } from '../../../lib/api/authz-api.types';

export interface AiProviderEntry {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
    readonly models: readonly {
        readonly id: string;
        readonly name: string;
        readonly description?: string;
        readonly badges?: readonly string[];
    }[];
}

interface AiModelTargetPickerDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly providers: readonly AiProviderEntry[];
    readonly existingTargetIds: readonly string[];
    readonly onSelect: (entry: CatalogEntry) => void;
}

type SelectionKind = 'provider' | 'model';

interface Selection {
    kind: SelectionKind;
    providerId: string;
    modelId?: string;
}

export function AiModelTargetPickerDialog({ open, onOpenChange, providers, existingTargetIds, onSelect }: AiModelTargetPickerDialogProps) {
    const [query, setQuery] = useState('');
    const [selection, setSelection] = useState<Selection | null>(null);
    const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});

    useEffect(() => {
        // Reset on close. The first setState in this effect trips
        // react-hooks/set-state-in-effect; lifting `query`/`selection`/
        // `collapsed` to the parent or using useReducer would fix it
        // properly — tracked as tech debt.
        if (!open) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setQuery('');
            setSelection(null);
            setCollapsed({});
        }
    }, [open]);

    const existingSet = useMemo(() => new Set(existingTargetIds), [existingTargetIds]);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        return providers
            .map(p => {
                const providerMatches = !q || p.name.toLowerCase().includes(q) || (p.description ?? '').toLowerCase().includes(q);
                const models = p.models.filter(
                    m => !q || providerMatches || m.name.toLowerCase().includes(q) || (m.description ?? '').toLowerCase().includes(q),
                );
                return { provider: p, models, providerMatches };
            })
            .filter(entry => entry.providerMatches || entry.models.length > 0);
    }, [providers, query]);

    const selectedLabel = useMemo(() => {
        if (!selection) return null;
        const p = providers.find(x => x.id === selection.providerId);
        if (!p) return null;
        if (selection.kind === 'provider') {
            return {
                title: p.name,
                kind: 'Provider' as const,
                subtitle: `Policy will apply to every model offered by ${p.name}.`,
            };
        }
        const m = p.models.find(x => x.id === selection.modelId);
        if (!m) return null;
        return {
            title: `${p.name} · ${m.name}`,
            kind: 'Model' as const,
            subtitle: `Policy will apply to ${m.name} only (child of ${p.name}).`,
        };
    }, [selection, providers]);

    const isExistingProvider = (providerId: string) => existingSet.has(providerId);
    const isExistingModel = (modelId: string) => existingSet.has(modelId);

    const handleContinue = () => {
        if (!selection) return;
        const p = providers.find(x => x.id === selection.providerId);
        if (!p) return;
        if (selection.kind === 'provider') {
            onSelect({
                id: p.id,
                name: p.name,
                description: p.description ?? '',
                type: 'LLM',
                subResources: p.models.map(m => ({ id: m.id, name: m.name, description: m.description, kind: 'LLMModel' })),
                badges: ['AI Provider'],
            });
            return;
        }
        const m = p.models.find(x => x.id === selection.modelId);
        if (!m) return;
        onSelect({
            id: m.id,
            name: `${p.name} · ${m.name}`,
            description: m.description ?? `Model ${m.name} exposed by ${p.name}.`,
            type: 'LLM',
            subResources: [],
            badges: ['AI Model', p.name],
        });
    };

    const toggleCollapsed = (providerId: string) => {
        setCollapsed(prev => ({ ...prev, [providerId]: !prev[providerId] }));
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Create AI Model policy</DialogTitle>
                    <DialogDescription>
                        Pick what this policy applies to. Choose an <span className="font-medium text-foreground">AI Provider</span> to
                        cover every model it exposes, or drill down to a specific <span className="font-medium text-foreground">Model</span>{' '}
                        to scope the policy tighter.
                    </DialogDescription>
                </DialogHeader>

                <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                        placeholder="Search providers or models…"
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        className="pl-8"
                        aria-label="Search providers or models"
                    />
                </div>

                <div className="max-h-[440px] overflow-y-auto rounded-md border">
                    {filtered.length === 0 ? (
                        <div className="p-6 text-center text-sm text-muted-foreground">No providers or models match your search.</div>
                    ) : (
                        <ul className="divide-y">
                            {filtered.map(({ provider, models }) => {
                                const isCollapsed = collapsed[provider.id] ?? false;
                                const providerSelected = selection?.kind === 'provider' && selection.providerId === provider.id;
                                const providerTaken = isExistingProvider(provider.id);

                                return (
                                    <li key={provider.id}>
                                        <div
                                            className={cn(
                                                'flex items-start gap-3 px-3 py-3 transition',
                                                providerSelected && 'bg-primary/5',
                                            )}
                                        >
                                            <button
                                                type="button"
                                                onClick={() => toggleCollapsed(provider.id)}
                                                className="mt-1 flex size-5 items-center justify-center rounded text-muted-foreground hover:bg-muted hover:text-foreground"
                                                aria-label={isCollapsed ? 'Expand provider' : 'Collapse provider'}
                                            >
                                                {isCollapsed ? <ChevronRight className="size-3.5" /> : <ChevronDown className="size-3.5" />}
                                            </button>

                                            <button
                                                type="button"
                                                disabled={providerTaken}
                                                onClick={() => setSelection({ kind: 'provider', providerId: provider.id })}
                                                className={cn(
                                                    'flex flex-1 items-start gap-3 rounded-md px-2 py-1 text-left transition',
                                                    !providerTaken && 'hover:bg-muted/40',
                                                    providerTaken && 'cursor-not-allowed opacity-60',
                                                )}
                                            >
                                                <div className="mt-0.5 flex size-9 items-center justify-center rounded-lg bg-fuchsia-500/10">
                                                    <Building2 className="size-4 text-fuchsia-500" />
                                                </div>
                                                <div className="min-w-0 flex-1">
                                                    <div className="flex items-center gap-2">
                                                        <p className="truncate text-sm font-medium">{provider.name}</p>
                                                        <Badge
                                                            variant="outline"
                                                            className="h-4 border-fuchsia-500/40 bg-fuchsia-500/10 px-1.5 uppercase tracking-wide text-fuchsia-600"
                                                            style={{ fontSize: '10px' }}
                                                        >
                                                            Provider
                                                        </Badge>
                                                        {providerTaken ? (
                                                            <Badge variant="secondary" className="h-4 px-1.5" style={{ fontSize: '10px' }}>
                                                                Policy exists
                                                            </Badge>
                                                        ) : null}
                                                    </div>
                                                    {provider.description ? (
                                                        <p className="truncate text-xs text-muted-foreground">{provider.description}</p>
                                                    ) : null}
                                                    <p className="mt-1 text-muted-foreground" style={{ fontSize: '11px' }}>
                                                        {provider.models.length} {provider.models.length === 1 ? 'model' : 'models'} ·
                                                        policy covers every child model
                                                    </p>
                                                </div>
                                                {providerSelected ? <ShieldCheck className="mt-1 size-4 text-primary" /> : null}
                                            </button>
                                        </div>

                                        {!isCollapsed && models.length > 0 ? (
                                            <ul className="divide-y border-t bg-muted/20">
                                                {models.map(m => {
                                                    const modelSelected =
                                                        selection?.kind === 'model' &&
                                                        selection.providerId === provider.id &&
                                                        selection.modelId === m.id;
                                                    const modelTaken = isExistingModel(m.id);
                                                    return (
                                                        <li key={m.id}>
                                                            <button
                                                                type="button"
                                                                disabled={modelTaken}
                                                                onClick={() =>
                                                                    setSelection({
                                                                        kind: 'model',
                                                                        providerId: provider.id,
                                                                        modelId: m.id,
                                                                    })
                                                                }
                                                                className={cn(
                                                                    'flex w-full items-start gap-3 py-2.5 pl-12 pr-4 text-left transition',
                                                                    !modelTaken && 'hover:bg-muted/40',
                                                                    modelSelected && 'bg-primary/5',
                                                                    modelTaken && 'cursor-not-allowed opacity-60',
                                                                )}
                                                            >
                                                                <div className="mt-0.5 flex size-7 items-center justify-center rounded-md bg-fuchsia-500/5">
                                                                    <Brain className="size-3.5 text-fuchsia-500" />
                                                                </div>
                                                                <div className="min-w-0 flex-1">
                                                                    <div className="flex items-center gap-2">
                                                                        <p className="truncate text-sm">{m.name}</p>
                                                                        <Badge
                                                                            variant="outline"
                                                                            className="h-4 px-1.5 uppercase tracking-wide"
                                                                            style={{ fontSize: '10px' }}
                                                                        >
                                                                            Model
                                                                        </Badge>
                                                                        {m.badges?.map(b => (
                                                                            <Badge
                                                                                key={b}
                                                                                variant="outline"
                                                                                className="h-4 px-1.5 capitalize"
                                                                                style={{ fontSize: '10px' }}
                                                                            >
                                                                                {b}
                                                                            </Badge>
                                                                        ))}
                                                                        {modelTaken ? (
                                                                            <Badge
                                                                                variant="secondary"
                                                                                className="h-4 px-1.5"
                                                                                style={{ fontSize: '10px' }}
                                                                            >
                                                                                Policy exists
                                                                            </Badge>
                                                                        ) : null}
                                                                    </div>
                                                                    {m.description ? (
                                                                        <p className="truncate text-xs text-muted-foreground">
                                                                            {m.description}
                                                                        </p>
                                                                    ) : null}
                                                                </div>
                                                                {modelSelected ? (
                                                                    <ShieldCheck className="mt-1 size-4 text-primary" />
                                                                ) : null}
                                                            </button>
                                                        </li>
                                                    );
                                                })}
                                            </ul>
                                        ) : null}
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                <DialogFooter className="items-center gap-3 sm:justify-between">
                    <div className="min-w-0 text-left text-xs text-muted-foreground">
                        {selectedLabel ? (
                            <div className="flex items-center gap-2">
                                <Badge
                                    variant="outline"
                                    className={cn(
                                        'h-5 px-1.5 uppercase tracking-wide',
                                        selectedLabel.kind === 'Provider' ? 'border-fuchsia-500/40 bg-fuchsia-500/10 text-fuchsia-600' : '',
                                    )}
                                    style={{ fontSize: '10px' }}
                                >
                                    {selectedLabel.kind}
                                </Badge>
                                <span className="truncate text-foreground">{selectedLabel.title}</span>
                                <span className="hidden truncate sm:inline">— {selectedLabel.subtitle}</span>
                            </div>
                        ) : (
                            <span>Select a provider or a specific model to continue.</span>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        <Button variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button onClick={handleContinue} disabled={!selection}>
                            Continue
                        </Button>
                    </div>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
