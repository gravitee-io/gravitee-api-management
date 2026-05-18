/**
 * Add/Edit dialog for an authorization Action. Actions live as entities with
 * {@code _kind: action} and uid {@code action.<slug>}. Slug is derived from the
 * name (lowercase a-z 0-9, dashes for everything else) and shown live in the
 * dialog so the user knows exactly which entity uid will be created.
 *
 * Portal-rendered modal so the backdrop covers the whole viewport
 * (consistent with ScimConnectorsDialog).
 */
import { Alert, AlertDescription, AlertTitle, Button, Label } from '@gravitee/graphene-core';
import { Zap, X } from 'lucide-react';
import { useEffect, useState, type CSSProperties } from 'react';
import { createPortal } from 'react-dom';
import { authzApiService } from '../../../lib/api/authz-api.service';
import type { EntityResponse } from '../../../lib/api/authz-api.types';

interface Props {
    open: boolean;
    onOpenChange: (next: boolean) => void;
    /** Pass an entity to edit (must be _kind=action), null/undefined to create. */
    editing: EntityResponse | null;
    envId: string;
    onSaved: () => void;
}

export function slugifyActionName(name: string): string {
    if (!name) return '';
    return name
        .toLowerCase()
        .replaceAll('_', '-')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

export function actionUid(name: string): string {
    const slug = slugifyActionName(name);
    return slug ? `action.${slug}` : 'action.unnamed';
}

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
    width: 'min(560px, 100%)',
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
    backgroundColor: '#fef3c7',
    color: '#b45309',
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
const body: CSSProperties = { padding: '1.25rem 1.5rem', overflowY: 'auto', flex: 1 };
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
const fieldLabel: CSSProperties = {
    display: 'block',
    fontSize: 12,
    fontWeight: 600,
    color: '#374151',
    marginBottom: 4,
    letterSpacing: '0.01em',
};
const fieldHelp: CSSProperties = { fontSize: 11, color: '#6b7280', marginTop: 4 };

export function ActionFormDialog({ open, onOpenChange, editing, envId, onSaved }: Props) {
    const isEdit = editing !== null;
    const [name, setName] = useState((editing?.attributes?.name as string) ?? editing?.uid?.replace(/^action\./, '') ?? '');
    const [description, setDescription] = useState((editing?.attributes?.description as string) ?? '');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (open) {
            setName((editing?.attributes?.name as string) ?? editing?.uid?.replace(/^action\./, '') ?? '');
            setDescription((editing?.attributes?.description as string) ?? '');
            setError(null);
        }
    }, [open, editing]);

    useEffect(() => {
        if (!open) return;
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

    const slug = slugifyActionName(name);
    const previewUid = actionUid(name);
    const valid = slug.length > 0;

    const submit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        if (!valid) {
            setError('Name must contain at least one letter or digit.');
            return;
        }
        setSubmitting(true);
        try {
            const payload = {
                uid: previewUid,
                attributes: {
                    _kind: 'action',
                    name: name.trim(),
                    description: description.trim() || undefined,
                },
                parents: [],
            };
            if (isEdit && editing) {
                await authzApiService.updateEntity(envId, editing.id, payload);
            } else {
                await authzApiService.createEntity(envId, payload);
            }
            onSaved();
            onOpenChange(false);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Save failed');
        } finally {
            setSubmitting(false);
        }
    };

    return createPortal(
        <div role="presentation" onClick={() => onOpenChange(false)} style={overlay}>
            <div role="dialog" aria-modal="true" aria-label={isEdit ? 'Edit action' : 'New action'} onClick={e => e.stopPropagation()} style={panel}>
                <div style={headerBar}>
                    <div style={headerIcon}>
                        <Zap size={18} />
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                        <h2 style={{ margin: 0, fontSize: 16, fontWeight: 600, color: '#0f172a', lineHeight: '1.4' }}>
                            {isEdit ? `Edit action — ${editing?.attributes?.name ?? ''}` : 'New action'}
                        </h2>
                        <p style={{ margin: '4px 0 0', fontSize: 12, color: '#6b7280', lineHeight: '1.5' }}>
                            Authorization verbs you reference from policies. The entity uid is derived from the name and is shown live below.
                        </p>
                    </div>
                    <button type="button" onClick={() => onOpenChange(false)} style={closeBtn} aria-label="Close" title="Close">
                        <X size={18} />
                    </button>
                </div>

                <form onSubmit={submit} style={{ display: 'contents' }}>
                    <div style={body}>
                        {error ? (
                            <div style={{ marginBottom: 12 }}>
                                <Alert variant="destructive">
                                    <AlertTitle>Error</AlertTitle>
                                    <AlertDescription style={{ fontFamily: 'monospace', fontSize: 12 }}>{error}</AlertDescription>
                                </Alert>
                            </div>
                        ) : null}

                        <div style={{ marginBottom: 14 }}>
                            <Label htmlFor="act-name" style={fieldLabel}>
                                Action name <span style={{ color: '#ef4444' }}>*</span>
                            </Label>
                            <input
                                id="act-name"
                                className="sc-input"
                                style={inputStyle}
                                value={name}
                                onChange={e => setName(e.target.value)}
                                placeholder="e.g. read, can_invoke, approve_payment"
                                autoComplete="off"
                                required
                                disabled={isEdit}
                            />
                            <p style={fieldHelp}>
                                The verb you reference from policy statements. {isEdit ? 'Immutable once created.' : 'Use snake_case or kebab-case — special characters become dashes.'}
                            </p>
                        </div>

                        <div style={{ marginBottom: 14 }}>
                            <Label htmlFor="act-desc" style={fieldLabel}>
                                Description
                            </Label>
                            <textarea
                                id="act-desc"
                                className="sc-input"
                                style={{ ...inputStyle, fontFamily: 'inherit', minHeight: 80, resize: 'vertical' }}
                                value={description}
                                onChange={e => setDescription(e.target.value)}
                                placeholder="What this action means in plain language. Optional but helpful when authors browse the catalog."
                            />
                        </div>

                        <div
                            style={{
                                border: '1px solid #e5e7eb',
                                borderRadius: 8,
                                backgroundColor: '#f9fafb',
                                padding: '10px 12px',
                                fontSize: 12,
                                color: '#374151',
                            }}
                        >
                            <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 4, fontWeight: 600, letterSpacing: '0.04em', textTransform: 'uppercase' }}>
                                Entity ID
                            </div>
                            <code
                                style={{
                                    fontFamily: 'ui-monospace, SFMono-Regular, monospace',
                                    color: valid ? '#0f172a' : '#9ca3af',
                                    fontSize: 13,
                                }}
                            >
                                {previewUid}
                            </code>
                        </div>
                    </div>

                    <div style={footerBar}>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={submitting || !valid}>
                            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create action'}
                        </Button>
                    </div>
                </form>
            </div>
        </div>,
        document.body,
    );
}
