import { useEffect, useState } from 'react';

type SaveState = 'idle' | 'saving' | 'saved' | 'error';

export function PoliciesPage() {
    const [devices, setDevices] = useState<string[]>([]);
    const [hostname, setHostname] = useState('');
    const [content, setContent] = useState('');
    const [saveState, setSaveState] = useState<SaveState>('idle');
    const [loadError, setLoadError] = useState<string | null>(null);

    useEffect(() => {
        fetch('/gamma/organizations/DEFAULT/modules/ai-fleet/devices')
            .then(res => res.json())
            .then((data: { hostname: string }[]) => {
                const hostnames = data.map(d => d.hostname);
                setDevices(hostnames);
                if (hostnames.length > 0) setHostname(hostnames[0]);
            })
            .catch(() => {});
    }, []);

    useEffect(() => {
        if (!hostname) return;
        setContent('');
        setLoadError(null);
        setSaveState('idle');
        fetch(`/gamma/organizations/DEFAULT/modules/ai-fleet/policies/${hostname}`)
            .then(res => {
                if (!res.ok) return res.text().then(t => { throw new Error(t); });
                return res.text();
            })
            .then(setContent)
            .catch(err => setLoadError(err.message));
    }, [hostname]);

    const save = () => {
        setSaveState('saving');
        fetch(`/gamma/organizations/DEFAULT/modules/ai-fleet/policies/${hostname}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'text/plain' },
            body: content,
        })
            .then(res => {
                if (!res.ok) return res.text().then(t => { throw new Error(t); });
                setSaveState('saved');
                setTimeout(() => setSaveState('idle'), 2500);
            })
            .catch(() => {
                setSaveState('error');
                setTimeout(() => setSaveState('idle'), 3000);
            });
    };

    const saveLabel: Record<SaveState, string> = {
        idle: 'Save',
        saving: 'Saving…',
        saved: '✓ Saved — DAImon reloading',
        error: '✗ Save failed',
    };

    const saveColor: Record<SaveState, string> = {
        idle: '#60a5fa',
        saving: '#94a3b8',
        saved: '#22c55e',
        error: '#ef4444',
    };

    return (
        <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem', height: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <h1 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Policies</h1>
                {devices.length > 0 && (
                    <select
                        value={hostname}
                        onChange={e => setHostname(e.target.value)}
                        style={{ padding: '0.25rem 0.5rem', borderRadius: '0.375rem', border: '1px solid var(--color-border)' }}
                    >
                        {devices.map(d => <option key={d} value={d}>{d}</option>)}
                    </select>
                )}
                <button
                    onClick={save}
                    disabled={saveState === 'saving' || !content}
                    style={{
                        padding: '0.35rem 1rem',
                        borderRadius: '0.375rem',
                        border: 'none',
                        background: saveColor[saveState],
                        color: '#fff',
                        fontWeight: 600,
                        fontSize: '0.9rem',
                        cursor: saveState === 'saving' ? 'default' : 'pointer',
                        transition: 'background 0.2s',
                    }}
                >
                    {saveLabel[saveState]}
                </button>
            </div>

            <div
                style={{
                    padding: '0.5rem 0.75rem',
                    borderRadius: '0.375rem',
                    background: 'rgba(96, 165, 250, 0.08)',
                    border: '1px solid rgba(96, 165, 250, 0.25)',
                    fontSize: '0.85rem',
                    color: 'var(--color-muted-foreground)',
                    lineHeight: 1.5,
                }}
            >
                <strong style={{ color: '#60a5fa' }}>ℹ︎ Live reload</strong> — Changes are written directly to the DAImon's{' '}
                <code>policies.yaml</code> on disk. The DAImon picks them up instantly via file watcher — no restart needed.
            </div>

            {loadError ? (
                <div style={{ color: '#ef4444', fontSize: '0.85rem' }}>
                    Could not load policies: {loadError}
                    {loadError.includes('not registered') && (
                        <span> — make sure the DAImon is running and has registered its policies path.</span>
                    )}
                </div>
            ) : (
                <textarea
                    value={content}
                    onChange={e => { setContent(e.target.value); setSaveState('idle'); }}
                    spellCheck={false}
                    style={{
                        flex: 1,
                        fontFamily: 'monospace',
                        fontSize: '0.85rem',
                        padding: '0.75rem',
                        background: 'var(--color-card)',
                        border: '1px solid var(--color-border)',
                        borderRadius: '0.5rem',
                        color: 'inherit',
                        resize: 'none',
                        lineHeight: 1.6,
                        outline: 'none',
                    }}
                />
            )}
        </div>
    );
}
