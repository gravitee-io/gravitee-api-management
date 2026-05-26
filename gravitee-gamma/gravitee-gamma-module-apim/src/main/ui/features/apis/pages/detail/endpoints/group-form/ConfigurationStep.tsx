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
import { Button, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Switch } from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronUpIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import type { HeaderEntry, HttpFormState, ProxyFormState, SharedConfigFormState, SslFormState } from '../types';
import { newHeaderRow } from '../types';

interface ConfigurationStepProps {
    config: SharedConfigFormState;
    onChange: (patch: Partial<SharedConfigFormState>) => void;
}

// ─── Collapsible section wrapper ──────────────────────────────────────────────

function CollapsibleSection({ title, defaultOpen = false, children }: { title: string; defaultOpen?: boolean; children: React.ReactNode }) {
    const [open, setOpen] = useState(defaultOpen);
    return (
        <div className="rounded-lg border">
            <button
                type="button"
                className="flex w-full items-center justify-between px-4 py-3 text-sm font-medium hover:bg-accent/50 transition-colors rounded-lg"
                onClick={() => setOpen(o => !o)}
                aria-expanded={open}
            >
                {title}
                {open ? (
                    <ChevronUpIcon className="size-4 shrink-0" aria-hidden />
                ) : (
                    <ChevronDownIcon className="size-4 shrink-0" aria-hidden />
                )}
            </button>
            {open && <div className="border-t px-4 pb-4 pt-4 space-y-4">{children}</div>}
        </div>
    );
}

// ─── Number input helper ───────────────────────────────────────────────────────

function NumInput({
    id,
    label,
    value,
    min,
    onChange,
    hint,
}: {
    id: string;
    label: string;
    value: number;
    min?: number;
    onChange: (n: number) => void;
    hint?: string;
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
                onChange={e => {
                    const n = parseInt(e.target.value, 10);
                    if (!isNaN(n)) onChange(n);
                }}
            />
            {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
        </div>
    );
}

// ─── Toggle row helper ────────────────────────────────────────────────────────

function SwitchRow({
    id,
    label,
    desc,
    checked,
    onChange,
    disabled,
    note,
}: {
    id: string;
    label: string;
    desc: string;
    checked: boolean;
    onChange: (v: boolean) => void;
    disabled?: boolean;
    note?: string;
}) {
    return (
        <div className="space-y-1">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-0.5">
                    <Label htmlFor={id} className={`text-sm${disabled ? ' text-muted-foreground' : ''}`}>
                        {label}
                    </Label>
                    <p className="text-xs text-muted-foreground">{desc}</p>
                </div>
                <Switch id={id} checked={checked} onCheckedChange={onChange} disabled={disabled} />
            </div>
            {note && <p className="text-xs text-warning">{note}</p>}
        </div>
    );
}

// ─── HTTP section ─────────────────────────────────────────────────────────────

function HttpSection({ http, onChange }: { http: HttpFormState; onChange: (p: Partial<HttpFormState>) => void }) {
    return (
        <CollapsibleSection title="HTTP configuration" defaultOpen={true}>
            {/* Connection & read timeouts */}
            <div className="grid grid-cols-2 gap-4">
                <NumInput
                    id="http-connect-timeout"
                    label="Connect timeout (ms)"
                    value={http.connectTimeout}
                    min={1}
                    onChange={v => onChange({ connectTimeout: v })}
                    hint="Timeout in ms to connect to the target."
                />
                <NumInput
                    id="http-read-timeout"
                    label="Read timeout (ms)"
                    value={http.readTimeout}
                    min={1}
                    onChange={v => onChange({ readTimeout: v })}
                    hint="Maximum time given to the backend to complete the request (including response) in milliseconds."
                />
                <NumInput
                    id="http-keepalive-timeout"
                    label="Keep-alive timeout (ms)"
                    value={http.keepAliveTimeout}
                    min={1}
                    onChange={v => onChange({ keepAliveTimeout: v })}
                    hint="Maximum time a connection will remain unused in the pool in milliseconds. Once the timeout has elapsed, the unused connection will be evicted."
                />
                <NumInput
                    id="http-idle-timeout"
                    label="Idle timeout (ms)"
                    value={http.idleTimeout}
                    min={0}
                    onChange={v => onChange({ idleTimeout: v })}
                    hint="Maximum time a TCP connection will stay active if no data is received or sent in milliseconds. Once elapsed, the connection will be closed to free resources. Zero means no timeout."
                />
                <NumInput
                    id="http-max-connections"
                    label="Max concurrent connections"
                    value={http.maxConcurrentConnections}
                    min={1}
                    onChange={v => onChange({ maxConcurrentConnections: v })}
                    hint="Maximum pool size for connections. For HTTP/2, this is the maximum number of multiplexed connections to the upstream server."
                />
                <NumInput
                    id="http-max-header-size"
                    label="Max header size (bytes)"
                    value={http.maxHeaderSize}
                    min={1}
                    onChange={v => onChange({ maxHeaderSize: v })}
                />
                <NumInput
                    id="http-max-chunk-size"
                    label="Max chunk size (bytes)"
                    value={http.maxChunkSize}
                    min={1}
                    onChange={v => onChange({ maxChunkSize: v })}
                />
                <div className="space-y-1.5">
                    <Label htmlFor="http-version" className="text-sm">
                        HTTP version
                    </Label>
                    <Select value={http.version} onValueChange={v => onChange({ version: v as HttpFormState['version'] })}>
                        <SelectTrigger id="http-version" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="HTTP_1_1">HTTP/1.1</SelectItem>
                            <SelectItem value="HTTP_2">HTTP/2</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
            </div>

            {/* Toggles */}
            <div className="space-y-4">
                <SwitchRow
                    id="http-keep-alive"
                    label="Keep-alive"
                    desc="Use an HTTP persistent connection to send and receive multiple HTTP requests / responses. This option is ignored for HTTP/2 as keep-alive is always enabled."
                    checked={http.keepAlive}
                    onChange={v => onChange({ keepAlive: v })}
                />
                <SwitchRow
                    id="http-pipelining"
                    label="Enable HTTP pipelining"
                    desc="When pipelining is enabled, requests will be written to connections without waiting for previous responses to return. This option is ignored for HTTP/2 as pipelining is not applicable."
                    checked={http.pipelining}
                    onChange={v => onChange({ pipelining: v })}
                />
                <SwitchRow
                    id="http-follow-redirects"
                    label="Follow HTTP redirects"
                    desc="When the connector receives a status code in the range 3xx from the backend, it follows the redirection provided by the Location response header."
                    checked={http.followRedirects}
                    onChange={v => onChange({ followRedirects: v })}
                />
                <SwitchRow
                    id="http-compression"
                    label="Enable compression (gzip, deflate)"
                    desc="The gateway can let the remote HTTP server know that it supports compression. If the remote server returns a compressed response, the gateway will decompress it. Disable this option if you don't want compression between the gateway and the remote server."
                    checked={http.useCompression}
                    onChange={v => onChange({ useCompression: v })}
                />
                <SwitchRow
                    id="http-propagate-encoding"
                    label="Propagate client Accept-Encoding header"
                    desc="If the client request header includes a value for Accept-Encoding, the gateway will propagate it to the backend. The gateway will NEVER attempt to decompress the body content if the backend response is compressed (gzip, deflate). DO NOT activate this option if you plan to interact with body responses."
                    checked={http.propagateClientAcceptEncoding}
                    onChange={v => onChange({ propagateClientAcceptEncoding: v })}
                    disabled={http.useCompression}
                    note={http.useCompression ? 'Accept-Encoding can only be propagated if "Enable compression" is disabled.' : undefined}
                />
                <SwitchRow
                    id="http-propagate-host"
                    label="Propagate client Host header"
                    desc="If activated, the host header propagated by the gateway to the backend is the value specified by the client request, possibly changed by policy execution. If not activated (default), the Gateway uses the endpoint target host unless the host header was changed by a policy."
                    checked={http.propagateClientHostHeader}
                    onChange={v => onChange({ propagateClientHostHeader: v })}
                />
                {http.version === 'HTTP_2' && (
                    <SwitchRow
                        id="http-h2c"
                        label="Allow h2c clear-text upgrade"
                        desc="Allow upgrading an HTTP/1.1 connection to HTTP/2 over clear text (h2c)."
                        checked={http.clearTextUpgrade}
                        onChange={v => onChange({ clearTextUpgrade: v })}
                    />
                )}
            </div>
        </CollapsibleSection>
    );
}

// ─── Proxy section ────────────────────────────────────────────────────────────

function ProxySection({ proxy, onChange }: { proxy: ProxyFormState; onChange: (p: Partial<ProxyFormState>) => void }) {
    return (
        <CollapsibleSection title="Proxy">
            <SwitchRow
                id="proxy-enabled"
                label="Enable proxy"
                desc="Route gateway-to-backend traffic through a proxy."
                checked={proxy.enabled}
                onChange={v => onChange({ enabled: v })}
            />

            {proxy.enabled && (
                <>
                    <SwitchRow
                        id="proxy-system"
                        label="Use system proxy"
                        desc="Inherit proxy settings from the gateway JVM."
                        checked={proxy.useSystemProxy}
                        onChange={v => onChange({ useSystemProxy: v })}
                    />

                    {!proxy.useSystemProxy && (
                        <div className="grid grid-cols-2 gap-4">
                            <div className="col-span-2 space-y-1.5">
                                <Label htmlFor="proxy-type" className="text-sm">
                                    Proxy type
                                </Label>
                                <Select value={proxy.type} onValueChange={v => onChange({ type: v })}>
                                    <SelectTrigger id="proxy-type" className="w-full">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="HTTP">HTTP</SelectItem>
                                        <SelectItem value="SOCKS4">SOCKS4</SelectItem>
                                        <SelectItem value="SOCKS5">SOCKS5</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="proxy-host" className="text-sm">
                                    Host
                                </Label>
                                <Input
                                    id="proxy-host"
                                    value={proxy.host}
                                    placeholder="proxy.example.com"
                                    onChange={e => onChange({ host: e.target.value })}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="proxy-port" className="text-sm">
                                    Port
                                </Label>
                                <Input
                                    id="proxy-port"
                                    type="number"
                                    value={proxy.port}
                                    placeholder="3128"
                                    onChange={e => onChange({ port: e.target.value })}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="proxy-username" className="text-sm">
                                    Username
                                </Label>
                                <Input
                                    id="proxy-username"
                                    value={proxy.username}
                                    placeholder="Optional"
                                    onChange={e => onChange({ username: e.target.value })}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="proxy-password" className="text-sm">
                                    Password
                                </Label>
                                <Input
                                    id="proxy-password"
                                    type="password"
                                    value={proxy.password}
                                    placeholder="Optional"
                                    onChange={e => onChange({ password: e.target.value })}
                                />
                            </div>
                        </div>
                    )}
                </>
            )}
        </CollapsibleSection>
    );
}

// ─── SSL section ──────────────────────────────────────────────────────────────

function SslSection({ ssl, onChange }: { ssl: SslFormState; onChange: (p: Partial<SslFormState>) => void }) {
    return (
        <CollapsibleSection title="SSL / TLS">
            <div className="space-y-4">
                <SwitchRow
                    id="ssl-hostname-verifier"
                    label="Hostname verifier"
                    desc="Verify that the upstream server's certificate hostname matches the request hostname."
                    checked={ssl.hostnameVerifier}
                    onChange={v => onChange({ hostnameVerifier: v })}
                />
                <SwitchRow
                    id="ssl-trust-all"
                    label="Trust all certificates"
                    desc="Accept any upstream certificate without validation. Not recommended for production environments."
                    checked={ssl.trustAll}
                    onChange={v => onChange({ trustAll: v })}
                />

                <div className="space-y-1.5">
                    <Label htmlFor="ssl-client-auth" className="text-sm">
                        Client authentication
                    </Label>
                    <Select
                        value={ssl.clientAuthentication}
                        onValueChange={v => onChange({ clientAuthentication: v as SslFormState['clientAuthentication'] })}
                    >
                        <SelectTrigger id="ssl-client-auth" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="NONE">None</SelectItem>
                            <SelectItem value="OPTIONAL">Optional</SelectItem>
                            <SelectItem value="REQUIRED">Required</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
            </div>
        </CollapsibleSection>
    );
}

// ─── Headers section ──────────────────────────────────────────────────────────

function HeadersSection({ headers, onChange }: { headers: HeaderEntry[]; onChange: (h: HeaderEntry[]) => void }) {
    function addHeader() {
        onChange([...headers, newHeaderRow()]);
    }

    function updateHeader(id: string, field: 'name' | 'value', val: string) {
        onChange(headers.map(h => (h._id === id ? { ...h, [field]: val } : h)));
    }

    function removeHeader(id: string) {
        onChange(headers.filter(h => h._id !== id));
    }

    return (
        <CollapsibleSection title="HTTP headers">
            <p className="text-xs text-muted-foreground">Add headers that the gateway will always send to the upstream endpoint.</p>
            <div className="space-y-2">
                {headers.map(h => (
                    <div key={h._id} className="flex items-center gap-2">
                        <Input
                            value={h.name}
                            placeholder="Header name"
                            onChange={e => updateHeader(h._id, 'name', e.target.value)}
                            className="flex-1"
                        />
                        <Input
                            value={h.value}
                            placeholder="Value"
                            onChange={e => updateHeader(h._id, 'value', e.target.value)}
                            className="flex-1"
                        />
                        <Button
                            type="button"
                            size="sm"
                            variant="ghost"
                            className="size-8 p-0 text-destructive hover:text-destructive shrink-0"
                            aria-label="Remove header"
                            onClick={() => removeHeader(h._id)}
                        >
                            <Trash2Icon className="size-3.5" aria-hidden />
                        </Button>
                    </div>
                ))}
            </div>
            <Button type="button" size="sm" variant="outline" className="gap-1.5" onClick={addHeader}>
                <PlusIcon className="size-3.5" aria-hidden />
                Add header
            </Button>
        </CollapsibleSection>
    );
}

// ─── Main export ──────────────────────────────────────────────────────────────

export function ConfigurationStep({ config, onChange }: Readonly<ConfigurationStepProps>) {
    return (
        <div className="space-y-3">
            <HttpSection http={config.http} onChange={p => onChange({ http: { ...config.http, ...p } })} />
            <ProxySection proxy={config.proxy} onChange={p => onChange({ proxy: { ...config.proxy, ...p } })} />
            <SslSection ssl={config.ssl} onChange={p => onChange({ ssl: { ...config.ssl, ...p } })} />
            <HeadersSection headers={config.headers} onChange={h => onChange({ headers: h })} />
        </div>
    );
}
