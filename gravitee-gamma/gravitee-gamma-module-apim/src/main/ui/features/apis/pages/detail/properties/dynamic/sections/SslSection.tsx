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
    Alert,
    AlertDescription,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';

import { CollapsibleSection, SwitchRow } from '../../../../../components/CollapsibleSection';
import type { KeyStoreFormState, KeyStoreType, SslFormState, TrustStoreFormState, TrustStoreType } from '../types';

const EL_HINT = "Supports EL expressions, e.g. /path/to/file or {#api.properties['key']}";

function ELInput({
    id,
    label,
    value,
    placeholder,
    onChange,
}: {
    id: string;
    label: string;
    value: string;
    placeholder?: string;
    onChange: (v: string) => void;
}) {
    return (
        <div className="space-y-1.5">
            <Label htmlFor={id} className="text-sm">
                {label}
            </Label>
            <Input
                id={id}
                value={value}
                placeholder={placeholder ?? 'Path or EL expression'}
                onChange={e => onChange(e.target.value)}
                className="font-mono text-sm"
            />
            <p className="text-xs text-muted-foreground py-2">{EL_HINT}</p>
        </div>
    );
}

function PathPasswordAlias({
    idPrefix,
    path,
    password,
    alias,
    onChange,
}: {
    idPrefix: string;
    path: string;
    password: string;
    alias: string;
    onChange: (patch: { path?: string; password?: string; alias?: string }) => void;
}) {
    return (
        <>
            <ELInput id={`${idPrefix}-path`} label="Path" value={path} onChange={v => onChange({ path: v })} />
            <div className="space-y-1.5">
                <Label htmlFor={`${idPrefix}-password`} className="text-sm">
                    Password
                </Label>
                <Input
                    id={`${idPrefix}-password`}
                    type="password"
                    value={password}
                    onChange={e => onChange({ password: e.target.value })}
                />
            </div>
            <div className="space-y-1.5">
                <Label htmlFor={`${idPrefix}-alias`} className="text-sm">
                    Alias
                </Label>
                <Input id={`${idPrefix}-alias`} value={alias} placeholder="Optional" onChange={e => onChange({ alias: e.target.value })} />
            </div>
        </>
    );
}

// ─── TrustStore fields ────────────────────────────────────────────────────────

function TrustStoreFields({ ts, onChange }: { ts: TrustStoreFormState; onChange: (next: TrustStoreFormState) => void }) {
    function handleTypeChange(newType: TrustStoreType) {
        switch (newType) {
            case 'PEM':
                onChange({ type: 'PEM', path: '' });
                break;
            case 'JKS':
                onChange({ type: 'JKS', path: '', password: '', alias: '' });
                break;
            case 'PKCS12':
                onChange({ type: 'PKCS12', path: '', password: '', alias: '' });
                break;
            default:
                onChange({ type: 'NONE' });
        }
    }

    return (
        <div className="space-y-4">
            <div className="space-y-1.5">
                <Label htmlFor="ssl-trust-store-type" className="text-sm">
                    Trust store type
                </Label>
                <Select value={ts.type} onValueChange={v => handleTypeChange(v as TrustStoreType)}>
                    <SelectTrigger id="ssl-trust-store-type" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="NONE">None</SelectItem>
                        <SelectItem value="PEM">PEM</SelectItem>
                        <SelectItem value="JKS">JKS</SelectItem>
                        <SelectItem value="PKCS12">PKCS12</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {ts.type === 'PEM' && (
                <ELInput
                    id="ssl-ts-pem-path"
                    label="Certificate path"
                    value={ts.path}
                    placeholder="/path/to/cert.pem"
                    onChange={v => onChange({ ...ts, path: v })}
                />
            )}

            {(ts.type === 'JKS' || ts.type === 'PKCS12') && (
                <PathPasswordAlias
                    idPrefix="ssl-ts"
                    path={ts.path}
                    password={ts.password}
                    alias={ts.alias}
                    onChange={patch => onChange({ ...ts, ...patch } as TrustStoreFormState)}
                />
            )}
        </div>
    );
}

// ─── KeyStore fields ──────────────────────────────────────────────────────────

function KeyStoreFields({ ks, onChange }: { ks: KeyStoreFormState; onChange: (next: KeyStoreFormState) => void }) {
    function handleTypeChange(newType: KeyStoreType) {
        switch (newType) {
            case 'PEM':
                onChange({ type: 'PEM', certPath: '', keyPath: '', keyPassword: '' });
                break;
            case 'JKS':
                onChange({ type: 'JKS', path: '', password: '', alias: '' });
                break;
            case 'PKCS12':
                onChange({ type: 'PKCS12', path: '', password: '', alias: '' });
                break;
            default:
                onChange({ type: 'NONE' });
        }
    }

    return (
        <div className="space-y-4">
            <div className="space-y-1.5">
                <Label htmlFor="ssl-key-store-type" className="text-sm">
                    Key store type
                </Label>
                <Select value={ks.type} onValueChange={v => handleTypeChange(v as KeyStoreType)}>
                    <SelectTrigger id="ssl-key-store-type" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="NONE">None</SelectItem>
                        <SelectItem value="PEM">PEM</SelectItem>
                        <SelectItem value="JKS">JKS</SelectItem>
                        <SelectItem value="PKCS12">PKCS12</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {ks.type === 'PEM' && (
                <>
                    <ELInput
                        id="ssl-ks-pem-cert-path"
                        label="Certificate path"
                        value={ks.certPath}
                        placeholder="/path/to/cert.pem"
                        onChange={v => onChange({ ...ks, certPath: v })}
                    />
                    <ELInput
                        id="ssl-ks-pem-key-path"
                        label="Private key path"
                        value={ks.keyPath}
                        placeholder="/path/to/key.pem"
                        onChange={v => onChange({ ...ks, keyPath: v })}
                    />
                    <div className="space-y-1.5">
                        <Label htmlFor="ssl-ks-pem-key-password" className="text-sm">
                            Key password
                        </Label>
                        <Input
                            id="ssl-ks-pem-key-password"
                            type="password"
                            value={ks.keyPassword}
                            placeholder="Optional"
                            onChange={e => onChange({ ...ks, keyPassword: e.target.value })}
                        />
                    </div>
                </>
            )}

            {(ks.type === 'JKS' || ks.type === 'PKCS12') && (
                <PathPasswordAlias
                    idPrefix="ssl-ks"
                    path={ks.path}
                    password={ks.password}
                    alias={ks.alias}
                    onChange={patch => onChange({ ...ks, ...patch } as KeyStoreFormState)}
                />
            )}
        </div>
    );
}

// ─── Export ───────────────────────────────────────────────────────────────────

interface SslSectionProps {
    ssl: SslFormState;
    onChange: (patch: Partial<SslFormState>) => void;
    disabled?: boolean;
}

export function SslSection({ ssl, onChange, disabled }: Readonly<SslSectionProps>) {
    return (
        <CollapsibleSection title="SSL / TLS">
            <div className="space-y-4">
                {/* ─── Basic toggles ─────────────────────────────────────────── */}
                <div className="space-y-4">
                    <SwitchRow
                        id="ssl-hostname-verifier"
                        label="Verify hostname"
                        desc="Verify that the server certificate hostname matches the target."
                        checked={ssl.hostnameVerifier}
                        onChange={v => onChange({ hostnameVerifier: v })}
                        disabled={disabled}
                    />
                    <SwitchRow
                        id="ssl-trust-all"
                        label="Trust all certificates"
                        desc="Accept any upstream certificate without validation. Not recommended for production."
                        checked={ssl.trustAll}
                        onChange={v => onChange({ trustAll: v })}
                        disabled={disabled}
                    />
                    {ssl.trustAll && (
                        <Alert variant="destructive">
                            <AlertDescription className="text-xs">
                                Trusting all certificates disables certificate validation. This exposes the connection to man-in-the-middle
                                attacks and should not be used in production environments.
                            </AlertDescription>
                        </Alert>
                    )}
                </div>

                {/* ─── Trust store ───────────────────────────────────────────── */}
                <div className="rounded-lg border p-4 space-y-4">
                    <p className="text-sm font-medium">Trust store</p>
                    <p className="text-xs text-muted-foreground py-2">
                        Used to verify the server&apos;s certificate chain against trusted CAs.
                    </p>
                    <TrustStoreFields ts={ssl.trustStore} onChange={ts => onChange({ trustStore: ts })} />
                </div>

                {/* ─── Key store ─────────────────────────────────────────────── */}
                <div className="rounded-lg border p-4 space-y-4">
                    <p className="text-sm font-medium">Key store</p>
                    <p className="text-xs text-muted-foreground py-2">
                        Used for mutual TLS — presents the gateway&apos;s certificate to the server.
                    </p>
                    <KeyStoreFields ks={ssl.keyStore} onChange={ks => onChange({ keyStore: ks })} />
                </div>
            </div>
        </CollapsibleSection>
    );
}
