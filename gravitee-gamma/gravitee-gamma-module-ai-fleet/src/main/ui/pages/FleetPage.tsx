import { useEffect, useState } from 'react';

interface DeviceState {
    hostname: string;
    last_seen: string;
    proxy_port: number;
    version: string;
}

export function FleetPage() {
    const [devices, setDevices] = useState<DeviceState[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetch('/gamma/organizations/DEFAULT/modules/ai-fleet/devices')
            .then(res => res.json())
            .then(data => {
                setDevices(data);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    }, []);

    if (loading) return <p>Loading fleet...</p>;
    if (error) return <p>Error: {error}</p>;

    return (
        <div style={{ padding: '1.5rem' }}>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
                AI Fleet ({devices.length} device{devices.length !== 1 ? 's' : ''})
            </h1>
            {devices.length === 0 ? (
                <p style={{ color: 'var(--color-muted-foreground)' }}>No DAImon devices detected. Start a DAImon agent to see it here.</p>
            ) : (
                <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))' }}>
                    {devices.map(device => (
                        <DeviceCard key={device.hostname} device={device} />
                    ))}
                </div>
            )}
        </div>
    );
}

function DeviceCard({ device }: { readonly device: DeviceState }) {
    const lastSeen = new Date(device.last_seen);
    const ageMs = Date.now() - lastSeen.getTime();
    const isOnline = ageMs < 2 * 60 * 1000;

    return (
        <div
            style={{
                border: '1px solid var(--color-border)',
                borderRadius: '0.5rem',
                padding: '1rem',
                background: 'var(--color-card)',
            }}
        >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <span
                    style={{
                        width: 8,
                        height: 8,
                        borderRadius: '50%',
                        background: isOnline ? '#22c55e' : '#ef4444',
                        flexShrink: 0,
                    }}
                />
                <span style={{ fontWeight: 600 }}>{device.hostname}</span>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--color-muted-foreground)' }}>
                <div>Version: {device.version}</div>
                {device.proxy_port ? <div>Proxy port: {device.proxy_port}</div> : null}
                <div>Last seen: {lastSeen.toLocaleString()}</div>
            </div>
        </div>
    );
}
