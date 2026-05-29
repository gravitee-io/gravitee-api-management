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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Switch } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

import { CollapsibleSection } from '../../../../../components/CollapsibleSection';

// ─── Types ────────────────────────────────────────────────────────────────────

export type JwtSignature = 'RSA_RS256' | 'RSA_RS384' | 'RSA_RS512' | 'HMAC_HS256' | 'HMAC_HS384' | 'HMAC_HS512';
export type JwtPublicKeyResolver = 'GIVEN_KEY' | 'GATEWAY_KEYS' | 'JWKS_URL';
type RevocationAuthType = 'none' | 'basic' | 'token';

export interface JwtConfig {
    signature: JwtSignature;
    publicKeyResolver: JwtPublicKeyResolver;
    resolverParameter: string;
    connectTimeout: number;
    requestTimeout: number;
    followRedirects: boolean;
    useSystemProxy: boolean;
    extractClaims: boolean;
    propagateAuthHeader: boolean;
    userClaim: string;
    clientIdClaim: string;
    confirmationMethodValidation: {
        ignoreMissing: boolean;
        certificateBoundThumbprint: {
            enabled: boolean;
            extractCertificateFromHeader: boolean;
            headerName: string;
        };
    };
    tokenTypValidation: {
        enabled: boolean;
        ignoreMissing: boolean;
        expectedValues: string[];
        ignoreCase: boolean;
    };
    revocationCheck: {
        enabled: boolean;
        revocationClaim: string;
        revocationListUrl: string;
        refreshInterval: number;
        connectTimeout: number;
        requestTimeout: number;
        followRedirects: boolean;
        useSystemProxy: boolean;
        auth: {
            type: RevocationAuthType;
            basic?: { username: string; password: string };
            token?: { value: string };
        };
    };
}

export const DEFAULT_JWT_CONFIG: JwtConfig = {
    signature: 'RSA_RS256',
    publicKeyResolver: 'GIVEN_KEY',
    resolverParameter: '',
    connectTimeout: 2000,
    requestTimeout: 2000,
    followRedirects: false,
    useSystemProxy: false,
    extractClaims: false,
    propagateAuthHeader: true,
    userClaim: 'sub',
    clientIdClaim: '',
    confirmationMethodValidation: {
        ignoreMissing: false,
        certificateBoundThumbprint: {
            enabled: false,
            extractCertificateFromHeader: false,
            headerName: 'ssl-client-cert',
        },
    },
    tokenTypValidation: {
        enabled: false,
        ignoreMissing: false,
        expectedValues: ['JWT'],
        ignoreCase: false,
    },
    revocationCheck: {
        enabled: false,
        revocationClaim: 'jti',
        revocationListUrl: '',
        refreshInterval: 300,
        connectTimeout: 2000,
        requestTimeout: 2000,
        followRedirects: false,
        useSystemProxy: false,
        auth: { type: 'none' },
    },
};

// ─── Constants ────────────────────────────────────────────────────────────────

const SIGNATURE_OPTIONS: { value: JwtSignature; label: string }[] = [
    { value: 'RSA_RS256', label: 'RS256 — RSA signature with SHA-256' },
    { value: 'RSA_RS384', label: 'RS384 — RSA signature with SHA-384' },
    { value: 'RSA_RS512', label: 'RS512 — RSA signature with SHA-512' },
    { value: 'HMAC_HS256', label: 'HS256 — HMAC with SHA-256' },
    { value: 'HMAC_HS384', label: 'HS384 — HMAC with SHA-384' },
    { value: 'HMAC_HS512', label: 'HS512 — HMAC with SHA-512' },
];

const RESOLVER_LABELS: Record<JwtPublicKeyResolver, string> = {
    GIVEN_KEY: 'Public key',
    GATEWAY_KEYS: 'Resolver parameter',
    JWKS_URL: 'JWKS URL',
};

const RESOLVER_PLACEHOLDERS: Partial<Record<JwtPublicKeyResolver, string>> = {
    GIVEN_KEY: '-----BEGIN PUBLIC KEY-----...',
    JWKS_URL: 'https://idp.example.com/.well-known/jwks.json',
};

// ─── Sub-components ──────────────────────────────────────────────────────────

function SwitchRow({
    id,
    label,
    description,
    checked,
    onChange,
    disabled,
}: {
    id: string;
    label: string;
    description?: string;
    checked: boolean;
    onChange: (v: boolean) => void;
    disabled?: boolean;
}) {
    return (
        <div className="flex items-center justify-between rounded-lg border px-4 py-3">
            <div className="space-y-0.5">
                <Label htmlFor={id} className="text-sm font-medium">
                    {label}
                </Label>
                {description && <p className="text-xs text-muted-foreground">{description}</p>}
            </div>
            <Switch id={id} checked={checked} onCheckedChange={onChange} disabled={disabled} />
        </div>
    );
}

function TagInput({
    value,
    onChange,
    disabled,
    placeholder,
}: {
    value: string[];
    onChange: (v: string[]) => void;
    disabled?: boolean;
    placeholder?: string;
}) {
    const [input, setInput] = useState('');
    const inputRef = useRef<HTMLInputElement>(null);

    function addTag() {
        const trimmed = input.trim();
        if (trimmed && !value.includes(trimmed)) onChange([...value, trimmed]);
        setInput('');
    }

    function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            addTag();
        } else if (e.key === 'Backspace' && input === '' && value.length > 0) onChange(value.slice(0, -1));
    }

    return (
        <div className="flex flex-wrap gap-1.5 min-h-10 rounded-md border border-input bg-background px-3 py-2">
            {value.map(tag => (
                <span
                    key={tag}
                    className="inline-flex items-center gap-1 rounded-md bg-secondary text-secondary-foreground text-xs font-medium px-2 py-0.5"
                >
                    {tag}
                    {!disabled && (
                        <button
                            type="button"
                            onClick={() => onChange(value.filter(t => t !== tag))}
                            className="opacity-60 hover:opacity-100 hover:text-destructive"
                            aria-label={`Remove ${tag}`}
                        >
                            <XIcon className="size-3" aria-hidden />
                        </button>
                    )}
                </span>
            ))}
            {!disabled && (
                <input
                    ref={inputRef}
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onBlur={addTag}
                    className="flex-1 min-w-24 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
                    placeholder={value.length === 0 ? (placeholder ?? 'Type and press Enter…') : ''}
                />
            )}
        </div>
    );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface JwtSecurityFieldsProps {
    value: JwtConfig;
    onChange: (v: JwtConfig) => void;
    readOnly?: boolean;
}

export function JwtSecurityFields({ value, onChange, readOnly = false }: Readonly<JwtSecurityFieldsProps>) {
    const isJwksUrl = value.publicKeyResolver === 'JWKS_URL';

    function patchConfirmation(patch: Partial<JwtConfig['confirmationMethodValidation']>) {
        onChange({ ...value, confirmationMethodValidation: { ...value.confirmationMethodValidation, ...patch } });
    }

    function patchCertBound(patch: Partial<JwtConfig['confirmationMethodValidation']['certificateBoundThumbprint']>) {
        patchConfirmation({
            certificateBoundThumbprint: { ...value.confirmationMethodValidation.certificateBoundThumbprint, ...patch },
        });
    }

    function patchTokenTyp(patch: Partial<JwtConfig['tokenTypValidation']>) {
        onChange({ ...value, tokenTypValidation: { ...value.tokenTypValidation, ...patch } });
    }

    function patchRevocation(patch: Partial<JwtConfig['revocationCheck']>) {
        onChange({ ...value, revocationCheck: { ...value.revocationCheck, ...patch } });
    }

    function patchRevocationAuth(patch: Partial<JwtConfig['revocationCheck']['auth']>) {
        patchRevocation({ auth: { ...value.revocationCheck.auth, ...patch } });
    }

    return (
        <div className="space-y-4">
            {/* Signature & Resolver */}
            <div className="space-y-2">
                <Label htmlFor="jwt-signature">Signature</Label>
                <Select
                    value={value.signature}
                    onValueChange={v => onChange({ ...value, signature: v as JwtSignature })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="jwt-signature" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {SIGNATURE_OPTIONS.map(o => (
                            <SelectItem key={o.value} value={o.value}>
                                {o.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">How the JSON Web Token must be signed.</p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-resolver">JWKS resolver</Label>
                <Select
                    value={value.publicKeyResolver}
                    onValueChange={v => onChange({ ...value, publicKeyResolver: v as JwtPublicKeyResolver, resolverParameter: '' })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="jwt-resolver" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="GIVEN_KEY">GIVEN_KEY — Provide a signature key as resolver parameter</SelectItem>
                        <SelectItem value="GATEWAY_KEYS">GATEWAY_KEYS — Look for key from Gateway configuration (issuer + kid)</SelectItem>
                        <SelectItem value="JWKS_URL">JWKS_URL — Retrieve JWKS from URL</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {value.publicKeyResolver !== 'GATEWAY_KEYS' && (
                <div className="space-y-2">
                    <Label htmlFor="jwt-resolver-param">{RESOLVER_LABELS[value.publicKeyResolver]}</Label>
                    <Input
                        id="jwt-resolver-param"
                        value={value.resolverParameter}
                        onChange={e => onChange({ ...value, resolverParameter: e.target.value })}
                        placeholder={RESOLVER_PLACEHOLDERS[value.publicKeyResolver] ?? ''}
                        disabled={readOnly}
                    />
                    <p className="text-xs text-muted-foreground">Supports Expression Language.</p>
                </div>
            )}

            {/* JWKS URL specific options */}
            {isJwksUrl && (
                <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">JWKS URL options</p>
                    <div className="grid gap-4" style={{ gridTemplateColumns: '1fr 1fr' }}>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-connect-timeout">Connect timeout (ms)</Label>
                            <Input
                                id="jwt-connect-timeout"
                                type="number"
                                min={0}
                                value={value.connectTimeout}
                                onChange={e => onChange({ ...value, connectTimeout: Number(e.target.value) })}
                                disabled={readOnly}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-request-timeout">Request timeout (ms)</Label>
                            <Input
                                id="jwt-request-timeout"
                                type="number"
                                min={0}
                                value={value.requestTimeout}
                                onChange={e => onChange({ ...value, requestTimeout: Number(e.target.value) })}
                                disabled={readOnly}
                            />
                        </div>
                    </div>
                    <SwitchRow
                        id="jwt-follow-redirects"
                        label="Follow HTTP redirects"
                        checked={value.followRedirects}
                        onChange={v => onChange({ ...value, followRedirects: v })}
                        disabled={readOnly}
                    />
                    <SwitchRow
                        id="jwt-use-system-proxy"
                        label="Use system proxy"
                        checked={value.useSystemProxy}
                        onChange={v => onChange({ ...value, useSystemProxy: v })}
                        disabled={readOnly}
                    />
                </div>
            )}

            {/* Token options */}
            <SwitchRow
                id="jwt-extract-claims"
                label="Extract JWT Claims"
                description="Put claims into the 'jwt.claims' context attribute."
                checked={value.extractClaims}
                onChange={v => onChange({ ...value, extractClaims: v })}
                disabled={readOnly}
            />
            <SwitchRow
                id="jwt-propagate-auth"
                label="Propagate Authorization header"
                description="Forward the Authorization header to the target endpoints after validation."
                checked={value.propagateAuthHeader}
                onChange={v => onChange({ ...value, propagateAuthHeader: v })}
                disabled={readOnly}
            />

            {/* Claims */}
            <div className="space-y-2">
                <Label htmlFor="jwt-user-claim">User claim</Label>
                <Input
                    id="jwt-user-claim"
                    value={value.userClaim}
                    onChange={e => onChange({ ...value, userClaim: e.target.value })}
                    placeholder="sub"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Claim where the user can be extracted. Supports dot-notation for nested claims.
                </p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-client-claim">Client ID claim</Label>
                <Input
                    id="jwt-client-claim"
                    value={value.clientIdClaim}
                    onChange={e => onChange({ ...value, clientIdClaim: e.target.value })}
                    placeholder="azp"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Claim where the client ID can be extracted. Leave empty to use standard behavior.
                </p>
            </div>

            {/* Confirmation Method Validation */}
            <CollapsibleSection title="Confirmation Method Validation">
                <SwitchRow
                    id="jwt-cnf-ignore-missing"
                    label="Ignore missing CNF"
                    description="Ignore CNF validation if the token doesn't contain any CNF information."
                    checked={value.confirmationMethodValidation.ignoreMissing}
                    onChange={v => patchConfirmation({ ignoreMissing: v })}
                    disabled={readOnly}
                />
                <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                        Certificate Bound Thumbprint (x5t#S256)
                    </p>
                    <SwitchRow
                        id="jwt-cert-bound-enabled"
                        label="Enable certificate bound thumbprint validation"
                        description="Validate the certificate thumbprint extracted from the access_token with the one provided by the client."
                        checked={value.confirmationMethodValidation.certificateBoundThumbprint.enabled}
                        onChange={v => patchCertBound({ enabled: v })}
                        disabled={readOnly}
                    />
                    <SwitchRow
                        id="jwt-cert-extract-from-header"
                        label="Extract client certificate from headers"
                        description="Extract the client certificate from a request header. Necessary when the mTLS connection is handled by a proxy."
                        checked={value.confirmationMethodValidation.certificateBoundThumbprint.extractCertificateFromHeader}
                        onChange={v => patchCertBound({ extractCertificateFromHeader: v })}
                        disabled={readOnly || !value.confirmationMethodValidation.certificateBoundThumbprint.enabled}
                    />
                    {value.confirmationMethodValidation.certificateBoundThumbprint.extractCertificateFromHeader && (
                        <div className="space-y-2">
                            <Label htmlFor="jwt-cert-header-name">Header name</Label>
                            <Input
                                id="jwt-cert-header-name"
                                value={value.confirmationMethodValidation.certificateBoundThumbprint.headerName}
                                onChange={e => patchCertBound({ headerName: e.target.value })}
                                placeholder="ssl-client-cert"
                                disabled={readOnly}
                            />
                        </div>
                    )}
                </div>
            </CollapsibleSection>

            {/* Token Type Validation */}
            <CollapsibleSection title="Token Type Validation">
                <SwitchRow
                    id="jwt-typ-enabled"
                    label="Enable token type validation"
                    description="Validate the token type extracted from the access_token."
                    checked={value.tokenTypValidation.enabled}
                    onChange={v => patchTokenTyp({ enabled: v })}
                    disabled={readOnly}
                />
                {value.tokenTypValidation.enabled && (
                    <>
                        <SwitchRow
                            id="jwt-typ-ignore-missing"
                            label="Ignore missing token type"
                            description="Ignore validation if the token doesn't contain any token type information."
                            checked={value.tokenTypValidation.ignoreMissing}
                            onChange={v => patchTokenTyp({ ignoreMissing: v })}
                            disabled={readOnly}
                        />
                        <SwitchRow
                            id="jwt-typ-ignore-case"
                            label="Ignore case"
                            description="Ignore case when comparing the expected values."
                            checked={value.tokenTypValidation.ignoreCase}
                            onChange={v => patchTokenTyp({ ignoreCase: v })}
                            disabled={readOnly}
                        />
                        <div className="space-y-2">
                            <Label>Expected values</Label>
                            <TagInput
                                value={value.tokenTypValidation.expectedValues}
                                onChange={v => patchTokenTyp({ expectedValues: v })}
                                disabled={readOnly}
                                placeholder="JWT"
                            />
                            <p className="text-xs text-muted-foreground">List of accepted token types. Press Enter or comma to add.</p>
                        </div>
                    </>
                )}
            </CollapsibleSection>

            {/* Revocation Check */}
            <CollapsibleSection title="Revocation Check">
                <SwitchRow
                    id="jwt-rev-enabled"
                    label="Enable revocation check"
                    description="Check the configured claim against a cached revocation list and deny if a match is found."
                    checked={value.revocationCheck.enabled}
                    onChange={v => patchRevocation({ enabled: v })}
                    disabled={readOnly}
                />
                {value.revocationCheck.enabled && (
                    <>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-rev-claim">Revocation claim</Label>
                            <Input
                                id="jwt-rev-claim"
                                value={value.revocationCheck.revocationClaim}
                                onChange={e => patchRevocation({ revocationClaim: e.target.value })}
                                placeholder="jti"
                                disabled={readOnly}
                            />
                            <p className="text-xs text-muted-foreground">The claim checked against the revocation list.</p>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="jwt-rev-url">Revocation list URL</Label>
                            <Input
                                id="jwt-rev-url"
                                value={value.revocationCheck.revocationListUrl}
                                onChange={e => patchRevocation({ revocationListUrl: e.target.value })}
                                placeholder="https://..."
                                disabled={readOnly}
                            />
                            <p className="text-xs text-muted-foreground">
                                Must return a newline-separated list of strings (Content-Type: text/plain).
                            </p>
                        </div>
                        <div className="grid gap-4" style={{ gridTemplateColumns: '1fr 1fr 1fr' }}>
                            <div className="space-y-2">
                                <Label htmlFor="jwt-rev-refresh">Refresh interval (s)</Label>
                                <Input
                                    id="jwt-rev-refresh"
                                    type="number"
                                    min={1}
                                    value={value.revocationCheck.refreshInterval}
                                    onChange={e => patchRevocation({ refreshInterval: Number(e.target.value) })}
                                    disabled={readOnly}
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="jwt-rev-connect-timeout">Connect timeout (ms)</Label>
                                <Input
                                    id="jwt-rev-connect-timeout"
                                    type="number"
                                    min={0}
                                    value={value.revocationCheck.connectTimeout}
                                    onChange={e => patchRevocation({ connectTimeout: Number(e.target.value) })}
                                    disabled={readOnly}
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="jwt-rev-request-timeout">Request timeout (ms)</Label>
                                <Input
                                    id="jwt-rev-request-timeout"
                                    type="number"
                                    min={0}
                                    value={value.revocationCheck.requestTimeout}
                                    onChange={e => patchRevocation({ requestTimeout: Number(e.target.value) })}
                                    disabled={readOnly}
                                />
                            </div>
                        </div>
                        <SwitchRow
                            id="jwt-rev-follow-redirects"
                            label="Follow redirects"
                            checked={value.revocationCheck.followRedirects}
                            onChange={v => patchRevocation({ followRedirects: v })}
                            disabled={readOnly}
                        />
                        <SwitchRow
                            id="jwt-rev-system-proxy"
                            label="Use system proxy"
                            checked={value.revocationCheck.useSystemProxy}
                            onChange={v => patchRevocation({ useSystemProxy: v })}
                            disabled={readOnly}
                        />

                        {/* Revocation list auth */}
                        <div className="rounded-lg border bg-muted/30 p-4 space-y-4">
                            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                                Revocation list security configuration
                            </p>
                            <div className="space-y-2">
                                <Label htmlFor="jwt-rev-auth-type">Authentication type</Label>
                                <Select
                                    value={value.revocationCheck.auth.type}
                                    onValueChange={v =>
                                        patchRevocationAuth({ type: v as RevocationAuthType, basic: undefined, token: undefined })
                                    }
                                    disabled={readOnly}
                                >
                                    <SelectTrigger id="jwt-rev-auth-type">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="none">No security</SelectItem>
                                        <SelectItem value="basic">Basic security</SelectItem>
                                        <SelectItem value="token">Token security</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            {value.revocationCheck.auth.type === 'basic' && (
                                <div className="grid gap-4" style={{ gridTemplateColumns: '1fr 1fr' }}>
                                    <div className="space-y-2">
                                        <Label htmlFor="jwt-rev-auth-user">Username</Label>
                                        <Input
                                            id="jwt-rev-auth-user"
                                            value={value.revocationCheck.auth.basic?.username ?? ''}
                                            onChange={e =>
                                                patchRevocationAuth({
                                                    basic: {
                                                        ...value.revocationCheck.auth.basic,
                                                        username: e.target.value,
                                                        password: value.revocationCheck.auth.basic?.password ?? '',
                                                    },
                                                })
                                            }
                                            disabled={readOnly}
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label htmlFor="jwt-rev-auth-pass">Password</Label>
                                        <Input
                                            id="jwt-rev-auth-pass"
                                            type="password"
                                            value={value.revocationCheck.auth.basic?.password ?? ''}
                                            onChange={e =>
                                                patchRevocationAuth({
                                                    basic: {
                                                        ...value.revocationCheck.auth.basic,
                                                        username: value.revocationCheck.auth.basic?.username ?? '',
                                                        password: e.target.value,
                                                    },
                                                })
                                            }
                                            disabled={readOnly}
                                        />
                                    </div>
                                </div>
                            )}
                            {value.revocationCheck.auth.type === 'token' && (
                                <div className="space-y-2">
                                    <Label htmlFor="jwt-rev-auth-token">Token value</Label>
                                    <Input
                                        id="jwt-rev-auth-token"
                                        type="password"
                                        value={value.revocationCheck.auth.token?.value ?? ''}
                                        onChange={e => patchRevocationAuth({ token: { value: e.target.value } })}
                                        disabled={readOnly}
                                    />
                                    <p className="text-xs text-muted-foreground">Added as Bearer token in the Authorization header.</p>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </CollapsibleSection>
        </div>
    );
}
