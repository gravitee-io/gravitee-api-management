import { useEffect, useRef, useState } from 'react';
import Prism from 'prismjs';
import 'prismjs/components/prism-yaml';

type SaveState = 'idle' | 'saving' | 'saved' | 'error';

// Injected once — Prism dark theme scoped to .yaml-editor
const PRISM_STYLE = `
.yaml-editor { background: var(--color-card); color: #abb2bf; }
.yaml-editor .token.comment    { color: #5c6370; font-style: italic; }
.yaml-editor .token.key        { color: #e06c75; }
.yaml-editor .token.string     { color: #98c379; }
.yaml-editor .token.boolean    { color: #56b6c2; }
.yaml-editor .token.number     { color: #d19a66; }
.yaml-editor .token.null       { color: #56b6c2; }
.yaml-editor .token.punctuation { color: #abb2bf; }
.yaml-editor .token.tag        { color: #e5c07b; }
.yaml-editor .token.important  { color: #e5c07b; font-weight: bold; }
`;

function injectStyle() {
    if (document.getElementById('prism-yaml-style')) return;
    const el = document.createElement('style');
    el.id = 'prism-yaml-style';
    el.textContent = PRISM_STYLE;
    document.head.appendChild(el);
}

const SHARED_STYLE: React.CSSProperties = {
    fontFamily: "'Fira Code', 'Cascadia Code', 'Menlo', monospace",
    fontSize: '0.85rem',
    lineHeight: '1.6',
    padding: '0.75rem',
    margin: 0,
    tabSize: 2,
    whiteSpace: 'pre',
    overflowWrap: 'normal',
    wordBreak: 'normal',
    borderRadius: '0.5rem',
    border: '1px solid var(--color-border)',
};

function YamlEditor({ value, onChange }: { readonly value: string; readonly onChange: (v: string) => void }) {
    const preRef = useRef<HTMLPreElement>(null);
    const highlighted = Prism.highlight(value || ' ', Prism.languages['yaml'], 'yaml');

    const syncScroll = (e: React.UIEvent<HTMLTextAreaElement>) => {
        if (preRef.current) {
            preRef.current.scrollTop = e.currentTarget.scrollTop;
            preRef.current.scrollLeft = e.currentTarget.scrollLeft;
        }
    };

    return (
        <div style={{ position: 'relative', flex: 1, minHeight: 1200 }}>
            <pre
                ref={preRef}
                aria-hidden
                className="yaml-editor"
                style={{
                    ...SHARED_STYLE,
                    position: 'absolute',
                    inset: 0,
                    overflow: 'hidden',
                    pointerEvents: 'none',
                }}
                dangerouslySetInnerHTML={{ __html: highlighted + '\n' }}
            />
            <textarea
                value={value}
                onChange={e => onChange(e.target.value)}
                onScroll={syncScroll}
                spellCheck={false}
                autoCapitalize="off"
                autoCorrect="off"
                style={{
                    ...SHARED_STYLE,
                    position: 'absolute',
                    inset: 0,
                    background: 'transparent',
                    color: 'transparent',
                    caretColor: '#abb2bf',
                    outline: 'none',
                    resize: 'none',
                    overflow: 'auto',
                    zIndex: 1,
                }}
            />
        </div>
    );
}

export function PoliciesPage() {
    const [content, setContent] = useState('');
    const [saveState, setSaveState] = useState<SaveState>('idle');
    const [loadError, setLoadError] = useState<string | null>(null);

    useEffect(() => { injectStyle(); }, []);

    useEffect(() => {
        fetch('/gamma/organizations/DEFAULT/modules/ai-fleet/policies')
            .then(res => {
                if (!res.ok) return res.text().then(t => { throw new Error(t); });
                return res.text();
            })
            .then(setContent)
            .catch(err => setLoadError(err.message));
    }, []);

    const save = () => {
        setSaveState('saving');
        fetch('/gamma/organizations/DEFAULT/modules/ai-fleet/policies', {
            method: 'POST',
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
                    flexShrink: 0,
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
                <YamlEditor value={content} onChange={v => { setContent(v); setSaveState('idle'); }} />
            )}
        </div>
    );
}
