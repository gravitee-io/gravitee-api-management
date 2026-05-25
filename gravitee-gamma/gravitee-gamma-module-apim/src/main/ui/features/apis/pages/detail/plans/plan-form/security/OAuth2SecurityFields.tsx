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
import { Input, Label, Switch } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

export interface OAuth2Config {
    oauthResource: string;
    oauthCacheResource: string;
    extractPayload: boolean;
    checkRequiredScopes: boolean;
    requiredScopes: string[];
    modeStrict: boolean;
    propagateAuthHeader: boolean;
}

export const DEFAULT_OAUTH2_CONFIG: OAuth2Config = {
    oauthResource: '',
    oauthCacheResource: '',
    extractPayload: false,
    checkRequiredScopes: false,
    requiredScopes: [],
    modeStrict: true,
    propagateAuthHeader: true,
};

function ScopeTagInput({ value, onChange, disabled }: { value: string[]; onChange: (v: string[]) => void; disabled?: boolean }) {
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
            {value.map(scope => (
                <span
                    key={scope}
                    className="inline-flex items-center gap-1 rounded-md bg-secondary text-secondary-foreground text-xs font-medium px-2 py-0.5"
                >
                    {scope}
                    {!disabled && (
                        <button
                            type="button"
                            onClick={() => onChange(value.filter(s => s !== scope))}
                            className="opacity-60 hover:opacity-100 hover:text-destructive"
                            aria-label={`Remove ${scope}`}
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
                    placeholder={value.length === 0 ? 'e.g. read:api' : ''}
                />
            )}
        </div>
    );
}

interface OAuth2SecurityFieldsProps {
    value: OAuth2Config;
    onChange: (v: OAuth2Config) => void;
    readOnly?: boolean;
}

export function OAuth2SecurityFields({ value, onChange, readOnly = false }: Readonly<OAuth2SecurityFieldsProps>) {
    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="oauth2-resource">
                    OAuth2 resource <span className="text-destructive">*</span>
                </Label>
                <Input
                    id="oauth2-resource"
                    value={value.oauthResource}
                    onChange={e => onChange({ ...value, oauthResource: e.target.value })}
                    placeholder="Name of the OAuth2 resource configured on this API"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    The name of the OAuth2 authorization server resource configured in your API&apos;s Resources section. Supports EL.
                </p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="oauth2-cache-resource">Cache resource</Label>
                <Input
                    id="oauth2-cache-resource"
                    value={value.oauthCacheResource}
                    onChange={e => onChange({ ...value, oauthCacheResource: e.target.value })}
                    placeholder="Name of the cache resource to store tokens"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">Cache resource used to store the validated tokens.</p>
            </div>

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="oauth2-extract-payload" className="text-sm font-medium">
                        Extract OAuth2 payload
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        Push the token endpoint payload into the &apos;oauth.payload&apos; context attribute.
                    </p>
                </div>
                <Switch
                    id="oauth2-extract-payload"
                    checked={value.extractPayload}
                    onCheckedChange={checked => onChange({ ...value, extractPayload: checked })}
                    disabled={readOnly}
                />
            </div>

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="oauth2-check-scopes" className="text-sm font-medium">
                        Check scopes
                    </Label>
                    <p className="text-xs text-muted-foreground">Check required scopes to access the resource.</p>
                </div>
                <Switch
                    id="oauth2-check-scopes"
                    checked={value.checkRequiredScopes}
                    onCheckedChange={checked =>
                        onChange({ ...value, checkRequiredScopes: checked, requiredScopes: checked ? value.requiredScopes : [] })
                    }
                    disabled={readOnly}
                />
            </div>

            {value.checkRequiredScopes && (
                <>
                    <div className="space-y-2">
                        <Label>Required scopes</Label>
                        <ScopeTagInput
                            value={value.requiredScopes}
                            onChange={scopes => onChange({ ...value, requiredScopes: scopes })}
                            disabled={readOnly}
                        />
                        <p className="text-xs text-muted-foreground">
                            List of OAuth2 scopes that must be present in the token. Press Enter or comma to add.
                        </p>
                    </div>

                    <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                        <div className="space-y-0.5">
                            <Label htmlFor="oauth2-mode-strict" className="text-sm font-medium">
                                Mode strict
                            </Label>
                            <p className="text-xs text-muted-foreground">
                                All listed scopes must be present (AND). When off, any scope is sufficient (OR).
                            </p>
                        </div>
                        <Switch
                            id="oauth2-mode-strict"
                            checked={value.modeStrict}
                            onCheckedChange={checked => onChange({ ...value, modeStrict: checked })}
                            disabled={readOnly}
                        />
                    </div>
                </>
            )}

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="oauth2-propagate" className="text-sm font-medium">
                        Permit authorization header to the target endpoints
                    </Label>
                    <p className="text-xs text-muted-foreground">Forward the Authorization header to the backend after validation.</p>
                </div>
                <Switch
                    id="oauth2-propagate"
                    checked={value.propagateAuthHeader}
                    onCheckedChange={checked => onChange({ ...value, propagateAuthHeader: checked })}
                    disabled={readOnly}
                />
            </div>
        </div>
    );
}
