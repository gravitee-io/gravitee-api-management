import { useEffect, useRef, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';

interface FleetEvent {
    timestamp: string;
    type: string;
    device_id: string;
    model?: string;
    tokens_in?: number;
    tokens_in_system?: number;
    tokens_in_history?: number;
    tokens_in_user?: number;
    tokens_out?: number;
    action?: string;
    reason?: string;
    process_name?: string;
    provider?: string;
    policy_applied?: string;
}

interface Stats {
    requests: number;
    blocked: number;
    tokens_in: number;
    tokens_in_system: number;
    tokens_in_history: number;
    tokens_in_user: number;
    tokens_out: number;
}

const TRAFFIC_COLORS: Record<string, string> = {
    request: '#22c55e',
    policy_block: '#ef4444',
    policy_warn: '#f59e0b',
};

export function EventsPage() {
    const location = useLocation();
    const [searchParams] = useSearchParams();
    const initialHost = (location.state as { host?: string } | null)?.host ?? searchParams.get('host') ?? '';
    const [hostname, setHostname] = useState(initialHost);
    const [devices, setDevices] = useState<string[]>([]);
    const [stats, setStats] = useState<Stats>({ requests: 0, blocked: 0, tokens_in: 0, tokens_in_system: 0, tokens_in_history: 0, tokens_in_user: 0, tokens_out: 0 });
    const [trafficEvents, setTrafficEvents] = useState<FleetEvent[]>([]);
    const [directEvents, setDirectEvents] = useState<FleetEvent[]>([]);
    const lastTimestampRef = useRef<string | null>(null);

    useEffect(() => {
        fetch('/gamma/organizations/DEFAULT/modules/ai-fleet/devices')
            .then(res => res.json())
            .then((data: { hostname: string }[]) => {
                const hostnames = data.map(d => d.hostname);
                setDevices(hostnames);
                if (!hostname && hostnames.length > 0) setHostname(hostnames[0]);
            })
            .catch(() => {});
    }, []);

    useEffect(() => {
        if (!hostname) return;
        setTrafficEvents([]);
        setDirectEvents([]);
        setStats({ requests: 0, blocked: 0, tokens_in: 0, tokens_in_system: 0, tokens_in_history: 0, tokens_in_user: 0, tokens_out: 0 });
        lastTimestampRef.current = null;

        const fetchStats = () => {
            fetch(`/gamma/organizations/DEFAULT/modules/ai-fleet/stats/${hostname}`)
                .then(res => res.json())
                .then(setStats)
                .catch(() => {});
        };

        const poll = () => {
            const url = lastTimestampRef.current
                ? `/gamma/organizations/DEFAULT/modules/ai-fleet/events/${hostname}?since=${encodeURIComponent(lastTimestampRef.current)}`
                : `/gamma/organizations/DEFAULT/modules/ai-fleet/events/${hostname}`;

            fetch(url)
                .then(res => res.json())
                .then((data: FleetEvent[]) => {
                    if (data.length > 0) {
                        lastTimestampRef.current = data[data.length - 1].timestamp;
                        const traffic = data.filter(e => e.type !== 'direct_connection');
                        const direct = data.filter(e => e.type === 'direct_connection');
                        if (traffic.length > 0) {
                            setTrafficEvents(prev => [...prev, ...traffic].slice(-200));
                            fetchStats();
                        }
                        if (direct.length > 0) setDirectEvents(prev => [...prev, ...direct].slice(-50));
                    }
                })
                .catch(() => {});
        };

        fetchStats();
        poll();
        const interval = setInterval(poll, 5000);
        return () => clearInterval(interval);
    }, [hostname]);

    return (
        <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem', height: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <h1 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Live Events</h1>
                {devices.length > 0 && (
                    <select
                        value={hostname}
                        onChange={e => setHostname(e.target.value)}
                        style={{ padding: '0.25rem 0.5rem', borderRadius: '0.375rem', border: '1px solid var(--color-border)' }}
                    >
                        {devices.map(d => (
                            <option key={d} value={d}>
                                {d}
                            </option>
                        ))}
                    </select>
                )}
            </div>

            {/* Metrics */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem' }}>
                <MetricCard label="Requests" value={stats.requests} color="#22c55e" />
                <MetricCard label="Blocked" value={stats.blocked} color="#ef4444" />
                <MetricCard label="Tokens out" value={stats.tokens_out} color="#a78bfa" />
            </div>
            <TokensInCard stats={stats} />

            {/* Intercepted traffic */}
            <div style={{ flex: 2, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <h2 style={{ fontSize: '0.875rem', fontWeight: 600 }}>Intercepted traffic</h2>
                    <span style={{ fontSize: '0.85rem', color: 'var(--color-muted-foreground)', letterSpacing: '0.02em' }}>↓ recent first</span>
                </div>
                <div
                    style={{
                        flex: 1,
                        overflowY: 'auto',
                        fontFamily: 'monospace',
                        fontSize: '0.85rem',
                        background: 'var(--color-card)',
                        border: '1px solid var(--color-border)',
                        borderRadius: '0.5rem',
                        padding: '0.75rem',
                    }}
                >
                    {trafficEvents.length === 0 ? (
                        <span style={{ color: 'var(--color-muted-foreground)' }}>
                            No intercepted traffic yet. Make sure Claude uses{' '}
                            <code>ANTHROPIC_BASE_URL=http://localhost:8990</code>.
                        </span>
                    ) : (
                        [...trafficEvents].reverse().map((event, i) => <TrafficRow key={i} event={event} />)
                    )}
                </div>
            </div>

            {/* Direct connections */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
                <div
                    style={{
                        marginBottom: '0.5rem',
                        padding: '0.5rem 0.75rem',
                        borderRadius: '0.375rem',
                        background: 'rgba(249, 115, 22, 0.08)',
                        border: '1px solid rgba(249, 115, 22, 0.3)',
                        fontSize: '0.85rem',
                        color: 'var(--color-muted-foreground)',
                        lineHeight: 1.5,
                    }}
                >
                    <strong style={{ color: '#f97316' }}>POC note</strong> — In this demo, Claude Code is redirected via{' '}
                    <code>ANTHROPIC_BASE_URL</code>. However, Claude Code makes additional direct calls that bypass this setting:
                    <ul style={{ margin: '0.4rem 0 0 1.2rem', padding: 0, lineHeight: 1.6 }}>
                        <li><strong>Authentication</strong> — login, token refresh</li>
                        <li><strong>Telemetry</strong> — usage analytics sent by the CLI</li>
                        <li><strong>Internal infrastructure</strong> — MCP servers, auto-updates</li>
                    </ul>
                    These cannot be intercepted via the proxy and are captured here by passive network detection.
                </div>
                <h2
                    style={{
                        fontSize: '0.875rem',
                        fontWeight: 600,
                        marginBottom: '0.5rem',
                        color: 'var(--color-muted-foreground)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                    }}
                >
                    <span
                        style={{
                            width: 8,
                            height: 8,
                            borderRadius: '50%',
                            background: directEvents.length > 0 ? '#f97316' : '#94a3b8',
                            display: 'inline-block',
                        }}
                    />
                    Direct connections detected ({directEvents.length})
                </h2>
                <div
                    style={{
                        flex: 1,
                        overflowY: 'auto',
                        fontFamily: 'monospace',
                        fontSize: '0.85rem',
                        background: 'var(--color-card)',
                        border: '1px solid var(--color-border)',
                        borderRadius: '0.5rem',
                        padding: '0.75rem',
                        opacity: 0.85,
                    }}
                >
                    {directEvents.length === 0 ? (
                        <span style={{ color: 'var(--color-muted-foreground)' }}>No direct connections detected.</span>
                    ) : (
                        directEvents.map((event, i) => <DirectRow key={i} event={event} />)
                    )}
                </div>
            </div>
        </div>
    );
}

function MetricCard({ label, value, color }: { readonly label: string; readonly value: number; readonly color: string }) {
    return (
        <div
            style={{
                padding: '0.75rem 1rem',
                background: 'var(--color-card)',
                border: '1px solid var(--color-border)',
                borderRadius: '0.5rem',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.25rem',
            }}
        >
            <span style={{ fontSize: '0.85rem', color: 'var(--color-muted-foreground)' }}>{label}</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color }}>{value.toLocaleString()}</span>
        </div>
    );
}

const TOKEN_BREAKDOWN = [
    { key: 'tokens_in_system' as const, label: 'System', color: '#f97316', desc: 'System prompt injected by Claude Code — tool definitions, CLAUDE.md rules, context compaction' },
    { key: 'tokens_in_history' as const, label: 'History', color: '#94a3b8', desc: 'Conversation history sent on each request (stateless API — full context every time)' },
    { key: 'tokens_in_user' as const, label: 'User', color: '#60a5fa', desc: 'Your current message' },
] as const;

function TokensInCard({ stats }: { readonly stats: Stats }) {
    return (
        <div
            style={{
                padding: '0.75rem 1rem',
                background: 'var(--color-card)',
                border: '1px solid var(--color-border)',
                borderRadius: '0.5rem',
            }}
        >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.75rem' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--color-muted-foreground)' }}>Tokens in</span>
                <span style={{ fontSize: '1.5rem', fontWeight: 700, color: '#60a5fa' }}>{stats.tokens_in.toLocaleString()}</span>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem' }}>
                {TOKEN_BREAKDOWN.map(({ key, label, color, desc }) => (
                    <div key={key} style={{ borderLeft: `3px solid ${color}`, paddingLeft: '0.6rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.2rem' }}>
                            <span style={{ fontSize: '1rem', fontWeight: 600, color }}>{label}</span>
                            <span style={{ fontSize: '1rem', fontWeight: 700, color }}>{stats[key].toLocaleString()}</span>
                        </div>
                        <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--color-muted-foreground)', lineHeight: 1.4 }}>{desc}</p>
                    </div>
                ))}
            </div>
        </div>
    );
}

function TrafficRow({ event }: { readonly event: FleetEvent }) {
    const color = TRAFFIC_COLORS[event.type] ?? '#94a3b8';
    const ts = new Date(event.timestamp).toLocaleTimeString();

    const detail = (() => {
        switch (event.type) {
            case 'request': {
                const hasSplit = (event.tokens_in_system ?? 0) + (event.tokens_in_history ?? 0) + (event.tokens_in_user ?? 0) > 0;
                const inDetail = hasSplit
                    ? `sys ${event.tokens_in_system ?? 0} · hist ${event.tokens_in_history ?? 0} · user ${event.tokens_in_user ?? 0}`
                    : `${event.tokens_in ?? 0}`;
                return `${event.model ?? '(unknown)'}  ${inDetail} → ${event.tokens_out ?? 0} tok`;
            }
            case 'policy_block':
                return `BLOCKED by ${event.policy_applied}: ${event.reason}`;
            case 'policy_warn':
                return `WARN by ${event.policy_applied}: ${event.reason}`;
            default:
                return event.action ?? '';
        }
    })();

    return (
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.3rem' }}>
            <span style={{ color: 'var(--color-muted-foreground)', flexShrink: 0 }}>{ts}</span>
            <span style={{ color, flexShrink: 0, minWidth: 100 }}>{event.type}</span>
            <span>{detail}</span>
        </div>
    );
}

function formatProvider(provider: string): string {
    const parts = provider.split(' → ');
    return parts.length === 2 ? `${parts[0]}(${parts[1]})` : provider;
}

function processLabel(processName: string): string {
    if (processName === 'java') return '(GATEWAY)';
    if (/^\d+\.\d+\.\d+/.test(processName)) return '(Claude Code CLI)';
    return '';
}

function DirectRow({ event }: { readonly event: FleetEvent }) {
    const ts = new Date(event.timestamp).toLocaleTimeString();
    const label = processLabel(event.process_name ?? '');
    return (
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.2rem', color: '#f97316' }}>
            <span style={{ color: 'var(--color-muted-foreground)', flexShrink: 0 }}>{ts}</span>
            <span>
                {event.process_name} → {formatProvider(event.provider ?? '')}
                {label && <span style={{ color: '#94a3b8' }}> {label}</span>}
            </span>
        </div>
    );
}
