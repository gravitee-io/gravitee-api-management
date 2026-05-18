/**
 * Actions catalog — list of authorization verbs referenced by policy statements.
 *
 * Actions are stored as entities with {@code _kind: action} and uid
 * {@code action.<slug-of-name>}. The dedicated UI here lifts them out of the
 * generic Entities Principals/Resources tabs so authors get a focused catalog
 * (name + entity id + description + actions menu), matching the
 * gamma-ui-prototype Actions page.
 */
import { Alert, AlertDescription, AlertTitle, Button } from '@gravitee/graphene-core';
import { Pencil, Plus, RefreshCcw, Search, Trash2, Zap } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { authzApiService } from '../../../lib/api/authz-api.service';
import type { EntityResponse } from '../../../lib/api/authz-api.types';
import { useEnvironment } from '../../lib/env/EnvironmentContext';
import { ActionFormDialog } from './ActionFormDialog';

function isAction(e: EntityResponse): boolean {
    const kind = (e.attributes?._kind ?? '') as string;
    if (kind.toLowerCase() === 'action') return true;
    return typeof e.uid === 'string' && e.uid.startsWith('action.');
}

function nameOf(e: EntityResponse): string {
    return (e.attributes?.name as string) ?? e.uid.replace(/^action\./, '');
}

function descriptionOf(e: EntityResponse): string | null {
    const d = e.attributes?.description;
    return typeof d === 'string' && d.length > 0 ? d : null;
}

export function ActionsPage() {
    const envId = useEnvironment();
    const [actions, setActions] = useState<EntityResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [query, setQuery] = useState('');

    const [dialogOpen, setDialogOpen] = useState(false);
    const [editing, setEditing] = useState<EntityResponse | null>(null);
    const [deleting, setDeleting] = useState<EntityResponse | null>(null);

    const reload = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await authzApiService.listEntities(envId, { page: 1, perPage: 500 });
            const items = (res.data ?? []).filter(isAction);
            items.sort((a, b) => nameOf(a).localeCompare(nameOf(b)));
            setActions(items);
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to load actions');
        } finally {
            setLoading(false);
        }
    }, [envId]);

    useEffect(() => {
        void reload();
    }, [reload]);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return actions;
        return actions.filter(a => {
            const n = nameOf(a).toLowerCase();
            const d = (descriptionOf(a) ?? '').toLowerCase();
            return n.includes(q) || a.uid.toLowerCase().includes(q) || d.includes(q);
        });
    }, [query, actions]);

    const openCreate = () => {
        setEditing(null);
        setDialogOpen(true);
    };

    const openEdit = (a: EntityResponse) => {
        setEditing(a);
        setDialogOpen(true);
    };

    const removeAction = async (a: EntityResponse) => {
        try {
            await authzApiService.deleteEntity(envId, a.id);
            setDeleting(null);
            await reload();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Delete failed');
        }
    };

    return (
        <div className="space-y-6" data-testid="page-actions">
            <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-start gap-3">
                    <div className="size-10 rounded-md bg-amber-500/10 flex items-center justify-center">
                        <Zap className="size-5 text-amber-500" aria-hidden />
                    </div>
                    <div>
                        <h1 className="text-2xl mb-1">Actions</h1>
                        <p className="text-sm text-muted-foreground max-w-2xl">
                            Catalog of action verbs you can reference from policy statements. Each row has a name, optional description,
                            and an auto-generated <code className="font-mono">action.&lt;slug&gt;</code> entity id used as the policy
                            target.
                        </p>
                    </div>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    <Button variant="outline" onClick={reload} disabled={loading} title="Refresh">
                        <RefreshCcw className="size-3.5 mr-2" />
                        Refresh
                    </Button>
                    <Button onClick={openCreate}>
                        <Plus className="size-3.5 mr-2" />
                        Add action
                    </Button>
                </div>
            </header>

            {error ? (
                <Alert variant="destructive">
                    <AlertTitle>Error</AlertTitle>
                    <AlertDescription className="font-mono text-xs">{error}</AlertDescription>
                </Alert>
            ) : null}

            {actions.length === 0 && !loading ? (
                <ActionsEmptyState onCreate={openCreate} />
            ) : (
                <>
                    <div className="relative max-w-md">
                        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                        <input
                            type="text"
                            placeholder="Search by name, entity id, or description…"
                            value={query}
                            onChange={e => setQuery(e.target.value)}
                            className="w-full pl-9 pr-3 py-2 text-sm rounded-md border border-input bg-background focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                        />
                    </div>

                    <div className="rounded-xl border bg-card overflow-hidden">
                        <div
                            style={{
                                display: 'grid',
                                gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.2fr) minmax(0,1.6fr) 80px',
                                gap: 12,
                                alignItems: 'center',
                                padding: '10px 16px',
                                borderBottom: '1px solid #e5e7eb',
                                backgroundColor: '#f9fafb',
                                fontSize: 11,
                                fontWeight: 600,
                                color: '#6b7280',
                                textTransform: 'uppercase',
                                letterSpacing: '0.04em',
                            }}
                        >
                            <div>Action name</div>
                            <div>Entity ID</div>
                            <div>Description</div>
                            <div style={{ textAlign: 'right' }}>&nbsp;</div>
                        </div>
                        {filtered.length === 0 ? (
                            <div className="px-4 py-12 text-center text-sm text-muted-foreground">
                                No actions match your search.
                            </div>
                        ) : (
                            <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
                                {filtered.map((row, i) => (
                                    <li
                                        key={row.id}
                                        style={{
                                            display: 'grid',
                                            gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.2fr) minmax(0,1.6fr) 80px',
                                            gap: 12,
                                            alignItems: 'center',
                                            padding: '12px 16px',
                                            borderTop: i === 0 ? 'none' : '1px solid #f1f5f9',
                                        }}
                                    >
                                        <div className="min-w-0 flex items-center gap-2">
                                            <Zap className="size-3.5 shrink-0 text-amber-500" aria-hidden />
                                            <span className="font-mono text-sm font-medium truncate">{nameOf(row)}</span>
                                        </div>
                                        <code className="text-xs text-muted-foreground truncate block" title={row.uid}>
                                            {row.uid}
                                        </code>
                                        <p className="text-sm text-muted-foreground line-clamp-2 min-w-0">
                                            {descriptionOf(row) ?? '—'}
                                        </p>
                                        <div className="flex justify-end gap-1">
                                            <button
                                                type="button"
                                                onClick={() => openEdit(row)}
                                                title="Edit"
                                                aria-label={`Edit ${nameOf(row)}`}
                                                className="inline-flex items-center justify-center size-8 rounded-md border border-border bg-background text-foreground/70 hover:bg-muted hover:text-foreground"
                                            >
                                                <Pencil className="size-3.5" />
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => setDeleting(row)}
                                                title="Delete"
                                                aria-label={`Delete ${nameOf(row)}`}
                                                className="inline-flex items-center justify-center size-8 rounded-md border border-border bg-background text-foreground/70 hover:bg-red-50 hover:text-red-600 hover:border-red-200"
                                            >
                                                <Trash2 className="size-3.5" />
                                            </button>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </>
            )}

            <ActionFormDialog
                open={dialogOpen}
                onOpenChange={setDialogOpen}
                editing={editing}
                envId={envId}
                onSaved={() => void reload()}
            />

            {deleting ? (
                <DeleteConfirm action={deleting} onCancel={() => setDeleting(null)} onConfirm={() => removeAction(deleting)} />
            ) : null}
        </div>
    );
}

function ActionsEmptyState({ onCreate }: { onCreate: () => void }) {
    return (
        <div className="rounded-xl border border-dashed bg-muted/20 px-6 py-12 text-center">
            <div className="mx-auto mb-3 size-12 rounded-full bg-amber-500/10 flex items-center justify-center">
                <Zap className="size-6 text-amber-500" aria-hidden />
            </div>
            <h2 className="text-base font-semibold mb-1">No actions defined yet</h2>
            <p className="mx-auto max-w-md text-sm text-muted-foreground mb-4">
                Add action verbs your policies will reference — for example <code className="font-mono">read</code>,{' '}
                <code className="font-mono">can_invoke</code>, or <code className="font-mono">approve_payment</code>.
            </p>
            <Button onClick={onCreate}>
                <Plus className="size-3.5 mr-2" /> Add your first action
            </Button>
        </div>
    );
}

function DeleteConfirm({
    action,
    onCancel,
    onConfirm,
}: {
    action: EntityResponse;
    onCancel: () => void;
    onConfirm: () => void;
}) {
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onCancel();
        };
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [onCancel]);
    return (
        <div
            role="presentation"
            onClick={onCancel}
            style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: 'rgba(15, 23, 42, 0.55)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '2rem',
                zIndex: 10000,
            }}
        >
            <div
                role="dialog"
                aria-modal="true"
                aria-label="Confirm delete"
                onClick={e => e.stopPropagation()}
                style={{
                    backgroundColor: '#fff',
                    borderRadius: 12,
                    maxWidth: 460,
                    width: '100%',
                    padding: '1.25rem 1.5rem',
                    boxShadow: '0 25px 70px -10px rgba(15,23,42,0.35)',
                }}
            >
                <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600, color: '#0f172a' }}>Remove action?</h3>
                <p style={{ marginTop: 8, fontSize: 13, color: '#4b5563' }}>
                    Policies that reference{' '}
                    <code style={{ fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontWeight: 600 }}>{nameOf(action)}</code> may
                    need to be updated.
                </p>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
                    <Button type="button" variant="outline" onClick={onCancel}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        onClick={onConfirm}
                        style={{ backgroundColor: '#ef4444', borderColor: '#ef4444', color: '#fff' }}
                    >
                        Delete
                    </Button>
                </div>
            </div>
        </div>
    );
}
