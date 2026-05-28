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
import {
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import type { HeaderEntry, HttpMethod } from '../types';
import { newHeaderRow } from '../types';

// ─── HTTP methods ─────────────────────────────────────────────────────────────

const HTTP_METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];

// ─── Export ───────────────────────────────────────────────────────────────────

interface HttpRequestSectionProps {
    method: HttpMethod;
    url: string;
    urlError?: string;
    headers: HeaderEntry[];
    body: string;
    specification: string;
    useSystemProxy: boolean;
    onChange: (patch: {
        method?: HttpMethod;
        url?: string;
        headers?: HeaderEntry[];
        body?: string;
        specification?: string;
        useSystemProxy?: boolean;
    }) => void;
    disabled?: boolean;
}

export function HttpRequestSection({
    method,
    url,
    urlError,
    headers,
    body,
    specification,
    useSystemProxy,
    onChange,
    disabled,
}: Readonly<HttpRequestSectionProps>) {
    function addHeader() {
        onChange({ headers: [...headers, newHeaderRow()] });
    }

    function updateHeader(id: string, field: 'name' | 'value', val: string) {
        onChange({ headers: headers.map(h => (h._id === id ? { ...h, [field]: val } : h)) });
    }

    function removeHeader(id: string) {
        onChange({ headers: headers.filter(h => h._id !== id) });
    }

    return (
        <div className="space-y-4">
            {/* ─── Method + URL ─────────────────────────────────────────────── */}
            <div className="space-y-1.5">
                <Label className="text-sm">Request</Label>
                <div className="flex gap-2">
                    <Select value={method} onValueChange={v => onChange({ method: v as HttpMethod })} disabled={disabled}>
                        <SelectTrigger className="w-28 shrink-0">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {HTTP_METHODS.map(m => (
                                <SelectItem key={m} value={m}>
                                    {m}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    <div className="flex-1 space-y-1">
                        <Input
                            id="dp-url"
                            value={url}
                            placeholder="https://api.example.com/properties"
                            onChange={e => onChange({ url: e.target.value })}
                            disabled={disabled}
                            aria-invalid={Boolean(urlError)}
                        />
                        {urlError && <p className="text-xs text-destructive">{urlError}</p>}
                    </div>
                </div>
            </div>

            {/* ─── System proxy shortcut ────────────────────────────────────── */}
            <div className="flex items-center justify-between gap-4">
                <div className="space-y-0.5">
                    <Label htmlFor="dp-use-system-proxy" className="text-sm">
                        Use system proxy
                    </Label>
                    <p className="text-xs text-muted-foreground">Route this request through the gateway&apos;s JVM system proxy.</p>
                </div>
                <Switch
                    id="dp-use-system-proxy"
                    checked={useSystemProxy}
                    onCheckedChange={v => onChange({ useSystemProxy: v })}
                    disabled={disabled}
                />
            </div>

            {/* ─── Headers ──────────────────────────────────────────────────── */}
            <div className="space-y-2">
                <Label className="text-sm">Request headers</Label>
                <p className="text-xs text-muted-foreground py-2">Headers sent with every poll request.</p>
                <div className="space-y-2">
                    {headers.map(h => (
                        <div key={h._id} className="flex items-center gap-2">
                            <Input
                                value={h.name}
                                placeholder="Header name"
                                onChange={e => updateHeader(h._id, 'name', e.target.value)}
                                className="flex-1"
                                disabled={disabled}
                            />
                            <Input
                                value={h.value}
                                placeholder="Value"
                                onChange={e => updateHeader(h._id, 'value', e.target.value)}
                                className="flex-1"
                                disabled={disabled}
                            />
                            <Button
                                type="button"
                                size="sm"
                                variant="ghost"
                                className="size-8 p-0 text-destructive hover:text-destructive shrink-0"
                                aria-label="Remove header"
                                onClick={() => removeHeader(h._id)}
                                disabled={disabled}
                            >
                                <Trash2Icon className="size-3.5" aria-hidden />
                            </Button>
                        </div>
                    ))}
                </div>
                <Button type="button" size="sm" variant="outline" className="gap-1.5" onClick={addHeader} disabled={disabled}>
                    <PlusIcon className="size-3.5" aria-hidden />
                    Add header
                </Button>
            </div>

            {/* ─── Request body ─────────────────────────────────────────────── */}
            <div className="space-y-1.5">
                <Label htmlFor="dp-body" className="text-sm">
                    Request body
                </Label>
                <p className="text-xs text-muted-foreground py-2">
                    Body sent with the poll request. Only applicable for POST, PUT, PATCH methods.
                </p>
                <Textarea
                    id="dp-body"
                    value={body}
                    placeholder='{"query": "properties"}'
                    onChange={e => onChange({ body: e.target.value })}
                    disabled={disabled}
                    rows={4}
                    className="font-mono text-sm"
                />
            </div>

            {/* ─── JOLT specification ───────────────────────────────────────── */}
            <div className="space-y-1.5">
                <Label htmlFor="dp-specification" className="text-sm">
                    JOLT transformation specification
                </Label>
                <p className="text-xs text-muted-foreground py-2">
                    JOLT spec to transform the upstream JSON response into{' '}
                    <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">{'[{"key":"k","value":"v"}]'}</code> format. Each
                    object must have a <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">key</code> and a{' '}
                    <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">value</code> field.
                </p>
                <Textarea
                    id="dp-specification"
                    value={specification}
                    onChange={e => onChange({ specification: e.target.value })}
                    disabled={disabled}
                    rows={8}
                    className="font-mono text-sm"
                    spellCheck={false}
                />
                <p className="text-xs text-muted-foreground py-2">
                    Leave as the default <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">default</code> operation if the
                    upstream already returns the expected format.
                </p>
            </div>
        </div>
    );
}
