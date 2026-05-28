/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Input, Label } from '@gravitee/graphene-core';

import { CollapsibleSection, SwitchRow } from '../../../../../components/CollapsibleSection';
import type { HttpClientFormState } from '../types';

// ─── Local helper ─────────────────────────────────────────────────────────────

function NumInput({
    id,
    label,
    value,
    hint,
    min,
    onChange,
    disabled,
}: {
    id: string;
    label: string;
    value: number;
    hint?: string;
    min?: number;
    onChange: (n: number) => void;
    disabled?: boolean;
}) {
    return (
        <div className="space-y-1.5">
            <Label htmlFor={id} className="text-sm">
                {label}
            </Label>
            <Input
                id={id}
                type="number"
                min={min}
                value={value}
                disabled={disabled}
                onChange={e => {
                    const n = parseInt(e.target.value, 10);
                    if (!isNaN(n)) onChange(n);
                }}
            />
            {hint && <p className="text-xs text-muted-foreground py-2">{hint}</p>}
        </div>
    );
}

// ─── Export ───────────────────────────────────────────────────────────────────

interface HttpClientSectionProps {
    httpClient: HttpClientFormState;
    onChange: (patch: Partial<HttpClientFormState>) => void;
    disabled?: boolean;
}

export function HttpClientSection({ httpClient, onChange, disabled }: Readonly<HttpClientSectionProps>) {
    return (
        <CollapsibleSection title="HTTP client options">
            <div className="grid grid-cols-2 gap-4">
                <NumInput
                    id="dp-connect-timeout"
                    label="Connect timeout (ms)"
                    value={httpClient.connectTimeout}
                    min={1}
                    hint="Maximum time to establish the connection."
                    onChange={v => onChange({ connectTimeout: v })}
                    disabled={disabled}
                />
                <NumInput
                    id="dp-read-timeout"
                    label="Read timeout (ms)"
                    value={httpClient.readTimeout}
                    min={1}
                    hint="Maximum time to receive the full response."
                    onChange={v => onChange({ readTimeout: v })}
                    disabled={disabled}
                />
                <NumInput
                    id="dp-keepalive-timeout"
                    label="Keep-alive timeout (ms)"
                    value={httpClient.keepAliveTimeout}
                    min={1}
                    hint="Maximum idle time before an unused connection is evicted from the pool."
                    onChange={v => onChange({ keepAliveTimeout: v })}
                    disabled={disabled}
                />
                <NumInput
                    id="dp-idle-timeout"
                    label="Idle timeout (ms)"
                    value={httpClient.idleTimeout}
                    min={0}
                    hint="Maximum time a TCP connection stays active with no data. Zero means no timeout."
                    onChange={v => onChange({ idleTimeout: v })}
                    disabled={disabled}
                />
                <NumInput
                    id="dp-max-connections"
                    label="Max concurrent connections"
                    value={httpClient.maxConcurrentConnections}
                    min={1}
                    onChange={v => onChange({ maxConcurrentConnections: v })}
                    disabled={disabled}
                />
            </div>
            <div className="space-y-4">
                <SwitchRow
                    id="dp-keepalive"
                    label="Keep-alive"
                    desc="Reuse TCP connections across multiple HTTP requests to reduce connection overhead."
                    checked={httpClient.keepAlive}
                    onChange={v => onChange({ keepAlive: v })}
                    disabled={disabled}
                />
                <SwitchRow
                    id="dp-pipelining"
                    label="Enable HTTP pipelining"
                    desc="Send multiple requests without waiting for each response. Ignored for HTTP/2."
                    checked={httpClient.pipelining}
                    onChange={v => onChange({ pipelining: v })}
                    disabled={disabled}
                />
                <SwitchRow
                    id="dp-compression"
                    label="Enable compression (gzip, deflate)"
                    desc="Advertise compression support to the upstream server and decompress the response automatically."
                    checked={httpClient.useCompression}
                    onChange={v => onChange({ useCompression: v })}
                    disabled={disabled}
                />
                <SwitchRow
                    id="dp-propagate-encoding"
                    label="Propagate client Accept-Encoding header"
                    desc="Forward the client's Accept-Encoding header to the upstream. Do NOT enable if you interact with the response body."
                    checked={httpClient.propagateClientAcceptEncoding}
                    onChange={v => onChange({ propagateClientAcceptEncoding: v })}
                    disabled={disabled || httpClient.useCompression}
                    note={httpClient.useCompression ? 'Accept-Encoding can only be propagated when compression is disabled.' : undefined}
                />
                <SwitchRow
                    id="dp-follow-redirects"
                    label="Follow HTTP redirects"
                    desc="Automatically follow 3xx redirects returned by the upstream server."
                    checked={httpClient.followRedirects}
                    onChange={v => onChange({ followRedirects: v })}
                    disabled={disabled}
                />
            </div>
        </CollapsibleSection>
    );
}
