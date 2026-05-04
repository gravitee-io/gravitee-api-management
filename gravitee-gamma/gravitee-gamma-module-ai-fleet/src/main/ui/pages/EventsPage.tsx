import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

interface FleetEvent {
    timestamp: string;
    type: string;
    device_id: string;
    model?: string;
    tokens_in?: number;
    tokens_out?: number;
    action?: string;
    reason?: string;
    process_name?: string;
    provider?: string;
    policy_applied?: string;
}

const TRAFFIC_COLORS: Record<string, string> = {
    request: '#22c55e',
    policy_block: '#ef4444',
    policy_warn: '#f59e0b',
};

export function EventsPage() {
    const [searchParams] = useSearchParams();
    const [hostname, setHostname] = useState(searchParams.get('host') ?? '');
    const [devices, setDevices] = useState<string[]>([]);
    const [trafficEvents, setTrafficEvents] = useState<FleetEvent[]>([]);
    const [directEvents, setDirectEvents] = useState<FleetEvent[]>([]);
    const lastTimestampRef = useRef<string | null>(null);
    const trafficBottomRef = useRef<HTMLDivElement>(null);

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
        lastTimestampRef.current = null;

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
                        if (traffic.length > 0) setTrafficEvents(prev => [...prev, ...traffic].slice(-200));
                        if (direct.length > 0) setDirectEvents(prev => [...prev, ...direct].slice(-50));
                    }
                })
                .catch(() => {});
        };

        poll();
        const interval = setInterval(poll, 5000);
        return () => clearInterval(interval);
    }, [hostname]);

    useEffect(() => {
        trafficBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [trafficEvents]);

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

            {/* Intercepted traffic */}
            <div style={{ flex: 2, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
                <h2 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--color-foreground)' }}>
                    Intercepted traffic
                </h2>
                <div
                    style={{
                        flex: 1,
                        overflowY: 'auto',
                        fontFamily: 'monospace',
                        fontSize: '0.8rem',
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
                        trafficEvents.map((event, i) => <TrafficRow key={i} event={event} />)
                    )}
                    <div ref={trafficBottomRef} />
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
                        fontSize: '0.75rem',
                        color: 'var(--color-muted-foreground)',
                        lineHeight: 1.5,
                    }}
                >
                    <strong style={{ color: '#f97316' }}>POC note</strong> — In this demo, Claude Code is redirected via{' '}
                    <code>ANTHROPIC_BASE_URL</code>. However, Claude Code makes additional direct calls that bypass this setting:{' '}
                    <strong>authentication</strong> (login, token refresh), <strong>telemetry</strong> (usage analytics sent by the CLI),
                    and <strong>internal infrastructure</strong> calls (MCP servers, auto-updates). These cannot be intercepted via the proxy
                    and are captured here by passive network detection.
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
                        fontSize: '0.75rem',
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

function TrafficRow({ event }: { readonly event: FleetEvent }) {
    const color = TRAFFIC_COLORS[event.type] ?? '#94a3b8';
    const ts = new Date(event.timestamp).toLocaleTimeString();

    const detail = (() => {
        switch (event.type) {
            case 'request':
                return `${event.model}  ${event.tokens_in ?? 0} → ${event.tokens_out ?? 0} tok`;
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

function DirectRow({ event }: { readonly event: FleetEvent }) {
    const ts = new Date(event.timestamp).toLocaleTimeString();
    return (
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.2rem', color: '#f97316' }}>
            <span style={{ color: 'var(--color-muted-foreground)', flexShrink: 0 }}>{ts}</span>
            <span>
                {event.process_name} (PID {event.device_id}) → {event.provider}
            </span>
        </div>
    );
}
