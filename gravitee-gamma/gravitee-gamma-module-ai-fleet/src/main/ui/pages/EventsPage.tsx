import { useEffect, useRef, useState } from 'react';

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

const EVENT_COLORS: Record<string, string> = {
    request: '#22c55e',
    policy_block: '#ef4444',
    policy_warn: '#f59e0b',
    direct_connection: '#f97316',
};

export function EventsPage() {
    const [hostname, setHostname] = useState('');
    const [devices, setDevices] = useState<string[]>([]);
    const [events, setEvents] = useState<FleetEvent[]>([]);
    const bottomRef = useRef<HTMLDivElement>(null);

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
        setEvents([]);

        const source = new EventSource(`/gamma/organizations/DEFAULT/modules/ai-fleet/events/${hostname}`);
        source.onmessage = e => {
            try {
                const event: FleetEvent = JSON.parse(e.data);
                setEvents(prev => [...prev.slice(-199), event]);
            } catch {
                // ignore malformed lines
            }
        };
        return () => source.close();
    }, [hostname]);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [events]);

    return (
        <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', height: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
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
                {events.length === 0 ? (
                    <span style={{ color: 'var(--color-muted-foreground)' }}>Waiting for events from {hostname || '…'}…</span>
                ) : (
                    events.map((event, i) => <EventRow key={i} event={event} />)
                )}
                <div ref={bottomRef} />
            </div>
        </div>
    );
}

function EventRow({ event }: { readonly event: FleetEvent }) {
    const color = EVENT_COLORS[event.type] ?? '#94a3b8';
    const ts = new Date(event.timestamp).toLocaleTimeString();

    const detail = (() => {
        switch (event.type) {
            case 'request':
                return `${event.model} ${event.tokens_in ?? 0}→${event.tokens_out ?? 0} tok`;
            case 'policy_block':
                return `BLOCKED by ${event.policy_applied}: ${event.reason}`;
            case 'policy_warn':
                return `WARN by ${event.policy_applied}: ${event.reason}`;
            case 'direct_connection':
                return `${event.process_name} → ${event.provider}`;
            default:
                return event.action ?? '';
        }
    })();

    return (
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.25rem' }}>
            <span style={{ color: 'var(--color-muted-foreground)', flexShrink: 0 }}>{ts}</span>
            <span style={{ color, flexShrink: 0, minWidth: 120 }}>{event.type}</span>
            <span>{detail}</span>
        </div>
    );
}
