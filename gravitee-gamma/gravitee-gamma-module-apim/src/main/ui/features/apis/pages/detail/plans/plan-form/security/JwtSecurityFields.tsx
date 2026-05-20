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

export type JwtSignature =
    | 'RSA_RS256'
    | 'RSA_RS384'
    | 'RSA_RS512'
    | 'HMAC_HS256'
    | 'HMAC_HS384'
    | 'HMAC_HS512'
    | 'ECDSA_ES256'
    | 'ECDSA_ES384'
    | 'ECDSA_ES512';

export type JwtPublicKeyResolver = 'GIVEN_KEY' | 'JWKS_URL' | 'RETRIEVING_KID';

export interface JwtConfig {
    signature: JwtSignature;
    publicKeyResolver: JwtPublicKeyResolver;
    resolverParameter: string;
    issuer: string;
    audiences: string;
    stripToken: boolean;
    userClaim: string;
    clientIdClaim: string;
}

export const DEFAULT_JWT_CONFIG: JwtConfig = {
    signature: 'RSA_RS256',
    publicKeyResolver: 'GIVEN_KEY',
    resolverParameter: '',
    issuer: '',
    audiences: '',
    stripToken: false,
    userClaim: 'sub',
    clientIdClaim: '',
};

const SIGNATURE_OPTIONS: { value: JwtSignature; label: string }[] = [
    { value: 'RSA_RS256', label: 'RSA — RS256' },
    { value: 'RSA_RS384', label: 'RSA — RS384' },
    { value: 'RSA_RS512', label: 'RSA — RS512' },
    { value: 'HMAC_HS256', label: 'HMAC — HS256' },
    { value: 'HMAC_HS384', label: 'HMAC — HS384' },
    { value: 'HMAC_HS512', label: 'HMAC — HS512' },
    { value: 'ECDSA_ES256', label: 'ECDSA — ES256' },
    { value: 'ECDSA_ES384', label: 'ECDSA — ES384' },
    { value: 'ECDSA_ES512', label: 'ECDSA — ES512' },
];

const RESOLVER_LABELS: Record<JwtPublicKeyResolver, string> = {
    GIVEN_KEY: 'Public key',
    JWKS_URL: 'JWKS URL',
    RETRIEVING_KID: 'KID claim name',
};

interface JwtSecurityFieldsProps {
    value: JwtConfig;
    onChange: (v: JwtConfig) => void;
    readOnly?: boolean;
}

export function JwtSecurityFields({ value, onChange, readOnly = false }: Readonly<JwtSecurityFieldsProps>) {
    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="jwt-signature">Signature type</Label>
                <Select
                    value={value.signature}
                    onValueChange={v => onChange({ ...value, signature: v as JwtSignature })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="jwt-signature">
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
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-resolver">Public key resolver</Label>
                <Select
                    value={value.publicKeyResolver}
                    onValueChange={v => onChange({ ...value, publicKeyResolver: v as JwtPublicKeyResolver, resolverParameter: '' })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="jwt-resolver">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="GIVEN_KEY">Given key</SelectItem>
                        <SelectItem value="JWKS_URL">JWKS URL</SelectItem>
                        <SelectItem value="RETRIEVING_KID">Retrieving KID</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-resolver-param">{RESOLVER_LABELS[value.publicKeyResolver]}</Label>
                <Input
                    id="jwt-resolver-param"
                    value={value.resolverParameter}
                    onChange={e => onChange({ ...value, resolverParameter: e.target.value })}
                    placeholder={value.publicKeyResolver === 'JWKS_URL' ? 'https://...' : ''}
                    disabled={readOnly}
                />
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-issuer">Issuer</Label>
                <Input
                    id="jwt-issuer"
                    value={value.issuer}
                    onChange={e => onChange({ ...value, issuer: e.target.value })}
                    placeholder="https://accounts.example.com"
                    disabled={readOnly}
                />
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-audiences">Audiences</Label>
                <Input
                    id="jwt-audiences"
                    value={value.audiences}
                    onChange={e => onChange({ ...value, audiences: e.target.value })}
                    placeholder="Comma-separated audience values"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">Comma-separated list of expected audience values.</p>
            </div>

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <Label htmlFor="jwt-strip" className="text-sm font-medium">
                    Strip JWT token
                </Label>
                <Switch
                    id="jwt-strip"
                    checked={value.stripToken}
                    onCheckedChange={checked => onChange({ ...value, stripToken: checked })}
                    disabled={readOnly}
                />
            </div>

            <div className="space-y-2">
                <Label htmlFor="jwt-user-claim">User claim</Label>
                <Input
                    id="jwt-user-claim"
                    value={value.userClaim}
                    onChange={e => onChange({ ...value, userClaim: e.target.value })}
                    placeholder="sub"
                    disabled={readOnly}
                />
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
            </div>
        </div>
    );
}
