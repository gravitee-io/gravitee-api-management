/**
 * Manages SCIM connectors for the current environment. Each connector is a stored
 * pull configuration (URL + token + name) that the backend scheduler polls every
 * ~10s and reconciles into the entity graph. The connector's {@code name} becomes
 * part of the entity uid: {@code user.<name>.<userName>} / {@code group.<name>.<displayName>}.
 *
 * Custom portal-rendered modal so the backdrop always covers the entire viewport
 * (the host shell's sidebar sits in a separate stacking context that the
 * graphene Dialog's backdrop fails to cover).
 *
 * Two views inside the modal:
 *   - "list"   — table of registered connectors with status dots and per-row actions
 *   - "editor" — form to create a new connector or edit an existing one
 */
import { Alert, AlertDescription, AlertTitle, Button, Label } from '@gravitee/graphene-core';
import { ChevronLeft, Plus, RefreshCw, Trash2, UploadCloud, X } from 'lucide-react';
import { useCallback, useEffect, useState, type CSSProperties, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { authzApiService } from '../../../lib/api/authz-api.service';
import type { ScimConnectorResponse } from '../../../lib/api/authz-api.types';
import { useEnvironment } from '../../lib/env/EnvironmentContext';

interface Props {
    open: boolean;
    onOpenChange: (next: boolean) => void;
    onChanged?: () => void;
}

type Mode = { kind: 'list' } | { kind: 'editor'; editing: ScimConnectorResponse | null };

const NAME_PATTERN = /^[a-z0-9][a-z0-9-]*$/;
const NAME_HELP = 'lowercase alphanumerics with optional dashes, no spaces';

// ---------- styles (inline for stacking-context safety) ----------------------

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
    backgroundColor: '#eef2ff',
    color: '#4338ca',
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
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
};
const body: CSSProperties = {
    padding: '1.25rem 1.5rem',
    overflowY: 'auto',
    flex: 1,
};
const footerBar: CSSProperties = {
    padding: '0.875rem 1.5rem',
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '0.5rem',
    backgroundColor: '#fafafa',
};
const inputStyle: CSSProperties = {
    width: '100%',
    padding: '0.55rem 0.75rem',
    fontSize: 13,
    borderRadius: 8,
    border: '1px solid #d1d5db',
    backgroundColor: '#fff',
    outline: 'none',
    transition: 'border-color 120ms, box-shadow 120ms',
};
const inputFocusable = (extra: CSSProperties = {}): CSSProperties => ({
    ...inputStyle,
    ...extra,
});
const fieldLabel: CSSProperties = {
    display: 'block',
    fontSize: 12,
    fontWeight: 600,
    color: '#374151',
    marginBottom: 4,
    letterSpacing: '0.01em',
};
const fieldHelp: CSSProperties = { fontSize: 11, color: '#6b7280', marginTop: 4 };
const required: CSSProperties = { color: '#ef4444' };

function formatTimestamp(value: string | number | null | undefined): string {
    if (value === null || value === undefined) return 'never';
    const d = typeof value === 'number' ? new Date(value * 1000) : new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString();
}

interface StatusDotProps {
    status: string | null | undefined;
}

function StatusDot({ status }: StatusDotProps) {
    let bg = '#9ca3af';
    let title = 'Not yet synced';
    if (status === 'OK') {
        bg = '#10b981';
        title = 'Last sync OK';
    } else if (status === 'PARTIAL') {
        bg = '#f59e0b';
        title = 'Last sync had warnings';
    } else if (status === 'ERROR') {
        bg = '#ef4444';
        title = 'Last sync failed';
    }
    return (
        <span
            title={title}
            aria-label={title}
            style={{
                display: 'inline-block',
                width: 10,
                height: 10,
                borderRadius: '50%',
                backgroundColor: bg,
                boxShadow: `0 0 0 3px ${bg}22, 0 0 0 1px ${bg}55`,
                verticalAlign: 'middle',
            }}
        />
    );
}

// ---------- main component ---------------------------------------------------

export function ScimConnectorsDialog({ open, onOpenChange, onChanged }: Props) {
    const envId = useEnvironment();
    const [connectors, setConnectors] = useState<ScimConnectorResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [mode, setMode] = useState<Mode>({ kind: 'list' });
    const [error, setError] = useState<string | null>(null);

    const reload = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await authzApiService.listScimConnectors(envId);
            setConnectors([...data]);
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to load connectors');
        } finally {
            setLoading(false);
        }
    }, [envId]);

    useEffect(() => {
        if (!open) return;
        void reload();
        const t = setInterval(reload, 5000);
        return () => clearInterval(t);
    }, [open, reload]);

    useEffect(() => {
        if (!open) {
            setMode({ kind: 'list' });
            setError(null);
            return;
        }
        const prev = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onOpenChange(false);
        };
        document.addEventListener('keydown', onKey);
        return () => {
            document.body.style.overflow = prev;
            document.removeEventListener('keydown', onKey);
        };
    }, [open, onOpenChange]);

    if (!open) return null;

    const goList = () => {
        setMode({ kind: 'list' });
        setError(null);
    };

    const removeConnector = async (c: ScimConnectorResponse) => {
        if (!window.confirm(`Delete connector "${c.name}"? All entities owned by it will be removed too.`)) return;
        try {
            await authzApiService.deleteScimConnector(envId, c.id);
            await reload();
            onChanged?.();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Delete failed');
        }
    };

    const syncNow = async (c: ScimConnectorResponse) => {
        try {
            await authzApiService.syncScimConnectorNow(envId, c.id);
            await reload();
            onChanged?.();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Sync failed');
        }
    };

    const isEditor = mode.kind === 'editor';
    const title = isEditor
        ? mode.editing
            ? `Edit connector — ${mode.editing.name}`
            : 'New connector'
        : 'SCIM Connectors';
    const subtitle = isEditor
        ? 'A connector pulls SCIM users and groups on a fixed cadence. The name is unique within the environment and becomes the connector segment in every imported entity uid.'
        : 'Registered SCIM 2.0 endpoints. The scheduler polls each every ~10s and reconciles users/groups into the entity graph. Click a connector to edit its setup.';

    return createPortal(
        <>
            <style>{`
                @keyframes sc-fadein { from { opacity: 0; } to { opacity: 1; } }
                @keyframes sc-pop {
                    from { opacity: 0; transform: translateY(8px) scale(0.98); }
                    to   { opacity: 1; transform: translateY(0) scale(1); }
                }
                .sc-input:focus { border-color: #6366f1 !important; box-shadow: 0 0 0 3px rgba(99,102,241,0.18) !important; }
                .sc-row:hover { background: #f9fafb; }
                .sc-row-link { color: #4338ca; font-weight: 600; cursor: pointer; }
                .sc-row-link:hover { text-decoration: underline; }
                .sc-iconbtn { width: 30px; height: 30px; border-radius: 6px; border: 1px solid #e5e7eb; background: #fff; color: #4b5563; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
                .sc-iconbtn:hover { background: #f3f4f6; color: #111827; }
                .sc-iconbtn.danger:hover { background: #fee2e2; color: #b91c1c; border-color: #fecaca; }
                .sc-card:hover { border-color: #6366f1; box-shadow: 0 4px 12px -2px rgba(99,102,241,0.18); }
                .sc-card:focus-visible { outline: 2px solid #6366f1; outline-offset: 2px; }
            `}</style>
            <div role="presentation" onClick={() => onOpenChange(false)} style={overlay}>
                <div role="dialog" aria-modal="true" aria-label={title} onClick={e => e.stopPropagation()} style={panel}>
                    <div style={headerBar}>
                        {isEditor ? (
                            <button
                                type="button"
                                onClick={goList}
                                style={{ ...headerIcon, cursor: 'pointer', border: 'none' }}
                                aria-label="Back to connector list"
                                title="Back"
                            >
                                <ChevronLeft size={18} />
                            </button>
                        ) : (
                            <div style={headerIcon}>
                                <UploadCloud size={18} />
                            </div>
                        )}
                        <div style={{ flex: 1, minWidth: 0 }}>
                            <h2 style={{ margin: 0, fontSize: 16, fontWeight: 600, color: '#0f172a', lineHeight: '1.4' }}>{title}</h2>
                            <p style={{ margin: '4px 0 0', fontSize: 12, color: '#6b7280', lineHeight: '1.5' }}>{subtitle}</p>
                        </div>
                        <button type="button" onClick={() => onOpenChange(false)} style={closeBtn} aria-label="Close" title="Close">
                            <X size={18} />
                        </button>
                    </div>

                    <div style={body}>
                        {error ? (
                            <div style={{ marginBottom: 12 }}>
                                <Alert variant="destructive">
                                    <AlertTitle>Error</AlertTitle>
                                    <AlertDescription style={{ fontFamily: 'monospace', fontSize: 12 }}>{error}</AlertDescription>
                                </Alert>
                            </div>
                        ) : null}

                        {mode.kind === 'list' ? (
                            <ConnectorsListView
                                connectors={connectors}
                                loading={loading}
                                onAdd={() => setMode({ kind: 'editor', editing: null })}
                                onEdit={c => setMode({ kind: 'editor', editing: c })}
                                onSync={syncNow}
                                onDelete={removeConnector}
                            />
                        ) : (
                            <ConnectorEditor
                                envId={envId}
                                initial={mode.editing}
                                onSaved={async () => {
                                    await reload();
                                    onChanged?.();
                                    goList();
                                }}
                                onCancel={goList}
                                setError={setError}
                            />
                        )}
                    </div>

                    {mode.kind === 'list' ? (
                        <div style={footerBar}>
                            <Button variant="outline" onClick={() => onOpenChange(false)}>
                                Close
                            </Button>
                        </div>
                    ) : null}
                </div>
            </div>
        </>,
        document.body,
    );
}

// ---------- List view --------------------------------------------------------

interface ListProps {
    connectors: ScimConnectorResponse[];
    loading: boolean;
    onAdd: () => void;
    onEdit: (c: ScimConnectorResponse) => void;
    onSync: (c: ScimConnectorResponse) => void;
    onDelete: (c: ScimConnectorResponse) => void;
}

function ConnectorsListView({ connectors, loading, onAdd, onEdit, onSync, onDelete }: ListProps) {
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 12, color: '#6b7280' }}>
                    {loading ? 'Loading…' : `${connectors.length} connector${connectors.length === 1 ? '' : 's'}`}
                </span>
                <Button size="sm" onClick={onAdd}>
                    <Plus className="mr-2 size-3.5" /> Add Connector
                </Button>
            </div>

            {connectors.length === 0 && !loading ? (
                <EmptyState onAdd={onAdd} />
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    {connectors.map(c => (
                        <ConnectorCard
                            key={c.id}
                            connector={c}
                            onEdit={() => onEdit(c)}
                            onSync={() => onSync(c)}
                            onDelete={() => onDelete(c)}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

function ConnectorCard({
    connector: c,
    onEdit,
    onSync,
    onDelete,
}: {
    connector: ScimConnectorResponse;
    onEdit: () => void;
    onSync: () => void;
    onDelete: () => void;
}) {
    const subText: string[] = [];
    if (c.lastSyncAt) {
        const u = c.lastUsersSynced;
        const g = c.lastGroupsSynced;
        subText.push(`${u} ${u === 1 ? 'user' : 'users'}`);
        subText.push(`${g} ${g === 1 ? 'group' : 'groups'}`);
        if (c.lastDeleted > 0) subText.push(`${c.lastDeleted} removed`);
    }
    return (
        <div className="sc-card" style={cardStyle} onClick={onEdit} role="button" tabIndex={0}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                <div style={{ paddingTop: 4 }}>
                    <StatusDot status={c.lastSyncStatus} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
                        <span style={{ fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 15, fontWeight: 600, color: '#0f172a' }}>
                            {c.name}
                        </span>
                        <ScopeBadges users={c.importUsers} groups={c.importGroups} />
                    </div>
                    <div
                        style={{
                            fontFamily: 'ui-monospace, SFMono-Regular, monospace',
                            fontSize: 12,
                            color: '#6b7280',
                            marginTop: 4,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                        title={c.url}
                    >
                        {c.url}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8, fontSize: 12, color: '#6b7280', flexWrap: 'wrap' }}>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                            <span style={{ color: '#374151', fontWeight: 500 }}>Last sync</span>
                            <span>·</span>
                            <span>{formatTimestamp(c.lastSyncAt)}</span>
                        </span>
                        {subText.length > 0 ? (
                            <>
                                <span style={{ color: '#d1d5db' }}>·</span>
                                <span style={{ color: '#4b5563' }}>{subText.join(', ')}</span>
                            </>
                        ) : null}
                        {c.lastError ? (
                            <>
                                <span style={{ color: '#d1d5db' }}>·</span>
                                <span
                                    title={c.lastError}
                                    style={{
                                        color: '#b91c1c',
                                        maxWidth: 260,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                        display: 'inline-block',
                                        verticalAlign: 'middle',
                                    }}
                                >
                                    {c.lastError}
                                </span>
                            </>
                        ) : null}
                    </div>
                </div>
                <div
                    style={{ display: 'flex', gap: 6, flex: 'none' }}
                    onClick={e => e.stopPropagation()}
                >
                    <button type="button" className="sc-iconbtn" onClick={onSync} title="Sync now" aria-label="Sync now">
                        <RefreshCw size={14} />
                    </button>
                    <button type="button" className="sc-iconbtn danger" onClick={onDelete} title="Delete" aria-label="Delete">
                        <Trash2 size={14} />
                    </button>
                </div>
            </div>
        </div>
    );
}

const cardStyle: CSSProperties = {
    border: '1px solid #e5e7eb',
    borderRadius: 12,
    padding: '14px 16px',
    backgroundColor: '#fff',
    cursor: 'pointer',
    transition: 'border-color 120ms, box-shadow 120ms, transform 120ms',
};

function EmptyState({ onAdd }: { onAdd: () => void }) {
    return (
        <div
            style={{
                border: '1px dashed #d1d5db',
                borderRadius: 12,
                padding: '2rem 1.5rem',
                textAlign: 'center',
                color: '#6b7280',
                backgroundColor: '#fafafa',
            }}
        >
            <UploadCloud size={28} style={{ margin: '0 auto 0.5rem', color: '#9ca3af' }} />
            <p style={{ margin: '0 0 4px', fontSize: 14, fontWeight: 600, color: '#374151' }}>No SCIM connectors yet</p>
            <p style={{ margin: '0 0 12px', fontSize: 12 }}>
                Add a connector pointing at a SCIM 2.0 endpoint and the scheduler will keep users and groups in sync automatically.
            </p>
            <Button size="sm" onClick={onAdd}>
                <Plus className="mr-2 size-3.5" /> Add Connector
            </Button>
        </div>
    );
}

function ScopeBadges({ users, groups }: { users: boolean; groups: boolean }) {
    const base: CSSProperties = {
        display: 'inline-block',
        fontSize: 10,
        fontWeight: 600,
        padding: '2px 8px',
        borderRadius: 999,
        marginRight: 4,
        letterSpacing: '0.04em',
        textTransform: 'uppercase',
    };
    return (
        <>
            {users ? <span style={{ ...base, backgroundColor: '#ecfdf5', color: '#047857' }}>Users</span> : null}
            {groups ? <span style={{ ...base, backgroundColor: '#eff6ff', color: '#1d4ed8' }}>Groups</span> : null}
            {!users && !groups ? <span style={{ ...base, backgroundColor: '#fef2f2', color: '#b91c1c' }}>Disabled</span> : null}
        </>
    );
}

function CountChip({ label, tone, title }: { label: string; tone: 'ok' | 'warn' | 'mute'; title?: string }) {
    const palette: Record<typeof tone, CSSProperties> = {
        ok: { backgroundColor: '#ecfdf5', color: '#047857', borderColor: '#a7f3d0' },
        warn: { backgroundColor: '#fef3c7', color: '#92400e', borderColor: '#fcd34d' },
        mute: { backgroundColor: '#f3f4f6', color: '#6b7280', borderColor: '#e5e7eb' },
    };
    return (
        <span
            title={title}
            style={{
                ...palette[tone],
                display: 'inline-block',
                fontSize: 11,
                fontWeight: 500,
                padding: '2px 8px',
                borderRadius: 999,
                border: '1px solid',
            }}
        >
            {label}
        </span>
    );
}

function Th({ children, width, align }: { children?: ReactNode; width?: string; align?: 'left' | 'right' }) {
    return (
        <th
            style={{
                textAlign: align ?? 'left',
                padding: '10px 14px',
                fontWeight: 600,
                width,
            }}
        >
            {children}
        </th>
    );
}

function Td({ children, align }: { children?: ReactNode; align?: 'left' | 'right' }) {
    return <td style={{ padding: '12px 14px', verticalAlign: 'top', textAlign: align ?? 'left' }}>{children}</td>;
}

// ---------- Editor view ------------------------------------------------------

interface EditorProps {
    envId: string;
    initial: ScimConnectorResponse | null;
    onSaved: () => Promise<void> | void;
    onCancel: () => void;
    setError: (msg: string | null) => void;
}

function ConnectorEditor({ envId, initial, onSaved, onCancel, setError }: EditorProps) {
    const isEdit = initial !== null;
    const [name, setName] = useState(initial?.name ?? '');
    const [url, setUrl] = useState(initial?.url ?? 'http://localhost:8092/gamma-authorization/scim');
    const [token, setToken] = useState('');
    const [importUsers, setImportUsers] = useState(initial?.importUsers ?? true);
    const [importGroups, setImportGroups] = useState(initial?.importGroups ?? true);
    const [submitting, setSubmitting] = useState(false);

    const importEverything = importUsers && importGroups;
    const toggleAll = (v: boolean) => {
        setImportUsers(v);
        setImportGroups(v);
    };

    const submit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        if (!NAME_PATTERN.test(name)) {
            setError(`Name must be ${NAME_HELP}.`);
            return;
        }
        if (!importUsers && !importGroups) {
            setError('Select at least one of Users / Groups to sync.');
            return;
        }
        setSubmitting(true);
        try {
            const payload = {
                name,
                url: url.trim(),
                token: token.trim(),
                importUsers,
                importGroups,
            };
            if (isEdit && initial) {
                await authzApiService.updateScimConnector(envId, initial.id, payload);
            } else {
                await authzApiService.createScimConnector(envId, payload);
            }
            await onSaved();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Save failed');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <div>
                    <Label htmlFor="sc-name" style={fieldLabel}>
                        Name <span style={required}>*</span>
                    </Label>
                    <input
                        id="sc-name"
                        className="sc-input"
                        style={inputFocusable(isEdit ? { backgroundColor: '#f3f4f6', color: '#6b7280' } : {})}
                        value={name}
                        onChange={e => setName(e.target.value)}
                        required
                        placeholder="e.g. am-local"
                        pattern="^[a-z0-9][a-z0-9-]*$"
                        title={NAME_HELP}
                        autoComplete="off"
                        disabled={isEdit}
                    />
                    <p style={fieldHelp}>
                        Used in entity uid as{' '}
                        <code style={{ fontFamily: 'ui-monospace, SFMono-Regular, monospace', backgroundColor: '#f3f4f6', padding: '1px 4px', borderRadius: 3 }}>
                            user.&lt;name&gt;.&lt;userName&gt;
                        </code>
                        . {NAME_HELP}
                        {isEdit ? ' — immutable once created.' : null}
                    </p>
                </div>
                <div>
                    <Label htmlFor="sc-url" style={fieldLabel}>
                        SCIM Base URL <span style={required}>*</span>
                    </Label>
                    <input
                        id="sc-url"
                        className="sc-input"
                        style={inputStyle}
                        value={url}
                        onChange={e => setUrl(e.target.value)}
                        required
                        autoComplete="off"
                    />
                    <p style={fieldHelp}>Root of the SCIM 2.0 service. The /Users and /Groups paths are appended automatically.</p>
                </div>
            </div>

            <div>
                <Label htmlFor="sc-token" style={fieldLabel}>
                    Bearer Token {!isEdit ? <span style={required}>*</span> : null}
                </Label>
                <input
                    id="sc-token"
                    className="sc-input"
                    style={{ ...inputStyle, fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 12 }}
                    type="text"
                    value={token}
                    onChange={e => setToken(e.target.value)}
                    placeholder={isEdit ? '••• leave blank to keep current token •••' : 'eyJhbGciOi…'}
                    autoComplete="off"
                    spellCheck={false}
                    data-1p-ignore
                    data-lpignore="true"
                    required={!isEdit}
                />
            </div>

            <div
                style={{
                    border: '1px solid #e5e7eb',
                    borderRadius: 10,
                    padding: '0.875rem 1rem',
                    backgroundColor: '#fafafa',
                }}
            >
                <div style={{ fontSize: 12, fontWeight: 600, color: '#374151', marginBottom: 8 }}>Scope</div>
                <CheckboxRow
                    label="Sync everything"
                    checked={importEverything}
                    onChange={toggleAll}
                    indent={0}
                    strong
                />
                <CheckboxRow label="Sync Users" checked={importUsers} onChange={setImportUsers} indent={1} />
                <CheckboxRow label="Sync Groups" checked={importGroups} onChange={setImportGroups} indent={1} />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, paddingTop: 4, borderTop: '1px solid #f1f5f9' }}>
                <Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
                    Cancel
                </Button>
                <Button type="submit" disabled={submitting}>
                    {submitting ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Connector'}
                </Button>
            </div>
        </form>
    );
}

function CheckboxRow({
    label,
    checked,
    onChange,
    indent,
    strong,
}: {
    label: string;
    checked: boolean;
    onChange: (v: boolean) => void;
    indent: number;
    strong?: boolean;
}) {
    return (
        <label
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                paddingLeft: indent * 22,
                marginBottom: 4,
                cursor: 'pointer',
                fontSize: 13,
                fontWeight: strong ? 600 : 400,
                color: strong ? '#111827' : '#374151',
            }}
        >
            <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)} />
            {label}
        </label>
    );
}
