import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

interface DeviceState {
    hostname: string;
    last_seen: string;
    proxy_port: number;
    version: string;
}

function LaptopIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="3" width="20" height="13" rx="2" />
            <path d="M0 20h24" />
            <path d="M8 20l1.5-4h5L16 20" />
        </svg>
    );
}

function HeartbeatIcon({ color }: { readonly color: string }) {
    return (
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="22,12 18,12 15,21 9,3 6,12 2,12" />
        </svg>
    );
}

export function FleetPage() {
    const [devices, setDevices] = useState<DeviceState[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchDevices = () => {
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
        };

        fetchDevices();
        const interval = setInterval(fetchDevices, 10000);
        return () => clearInterval(interval);
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
                        <Link
                            key={device.hostname}
                            to="../events"
                            state={{ host: device.hostname }}
                            relative="route"
                            style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}
                        >
                            <DeviceCard device={device} />
                        </Link>
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

    const statusColor = isOnline ? '#22c55e' : '#f97316';
    const statusLabel = isOnline ? 'Active' : 'Inactive for more than 2 min';

    return (
        <div
            style={{
                border: '1px solid var(--color-border)',
                borderRadius: '0.5rem',
                padding: '1rem',
                background: 'var(--color-card)',
                cursor: 'pointer',
            }}
        >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <span style={{ color: 'var(--color-muted-foreground)', display: 'flex' }}>
                    <LaptopIcon />
                </span>
                <span style={{ fontWeight: 600 }}>{device.hostname}</span>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--color-muted-foreground)', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: statusColor }}>
                    <HeartbeatIcon color={statusColor} />
                    <span style={{ fontWeight: 500 }}>{statusLabel}</span>
                </div>
                <div>Version: {device.version}</div>
                {device.proxy_port ? <div>Proxy port: {device.proxy_port}</div> : null}
                <div>Last seen: {lastSeen.toLocaleString()}</div>
            </div>
        </div>
    );
}
