import { Button, Input, Label } from '@gravitee/graphene-core';
import { useState } from 'react';
import type { EntityRequest, EntityResponse } from '../../../lib/api/authz-api.types';

export interface EntityEditorFormProps {
    readonly initial?: EntityResponse | null;
    readonly onCancel: () => void;
    readonly onSubmit: (request: EntityRequest) => Promise<void>;
}

export function EntityEditorForm({ initial, onCancel, onSubmit }: EntityEditorFormProps) {
    const [uid, setUid] = useState(initial?.uid ?? '');
    const [attributesJson, setAttributesJson] = useState(() => JSON.stringify(initial?.attributes ?? {}, null, 2));
    const [parentsText, setParentsText] = useState(() => (initial?.parents ?? []).join('\n'));
    const [submitting, setSubmitting] = useState(false);
    const [parseError, setParseError] = useState<string | null>(null);

    const submit = async () => {
        setParseError(null);
        let parsed: unknown;
        try {
            parsed = JSON.parse(attributesJson);
        } catch {
            setParseError('Attributes must be valid JSON.');
            return;
        }
        // `JSON.parse` happily accepts primitives (`"hello"`, `42`, `null`,
        // `true`) and arrays (`[1,2,3]`) — none of which are valid attribute
        // bags. Reject anything that isn't a plain non-null object so we
        // never send a malformed payload to the backend.
        if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
            setParseError('Attributes must be a JSON object.');
            return;
        }
        const attributes = parsed as Record<string, unknown>;
        const parents = parentsText
            .split('\n')
            .map(s => s.trim())
            .filter(Boolean);
        setSubmitting(true);
        try {
            await onSubmit({ uid, attributes, parents });
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
                <Label htmlFor="entity-uid">Entity UID</Label>
                <Input id="entity-uid" value={uid} onChange={e => setUid(e.target.value)} placeholder='User::"alice"' />
            </div>
            <div>
                <Label htmlFor="entity-attrs">Attributes (JSON)</Label>
                <textarea
                    id="entity-attrs"
                    value={attributesJson}
                    onChange={e => setAttributesJson(e.target.value)}
                    rows={8}
                    style={{ width: '100%', fontFamily: 'monospace', fontSize: 13 }}
                />
                {parseError !== null && <div style={{ color: 'crimson', fontSize: '0.85rem' }}>{parseError}</div>}
            </div>
            <div>
                <Label htmlFor="entity-parents">Parents (one per line)</Label>
                <textarea
                    id="entity-parents"
                    value={parentsText}
                    onChange={e => setParentsText(e.target.value)}
                    rows={3}
                    style={{ width: '100%', fontFamily: 'monospace', fontSize: 13 }}
                />
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                <Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
                    Cancel
                </Button>
                <Button type="button" onClick={submit} disabled={submitting || uid.trim() === ''}>
                    {submitting ? 'Saving…' : initial ? 'Update entity' : 'Create entity'}
                </Button>
            </div>
        </div>
    );
}
