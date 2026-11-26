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
import { Label, Switch } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

import { DEFAULT_OAUTH2_CONFIG, type OAuth2Config } from './oauth2Config';
import { type ApiResourceOption, ResourceSelectInput } from './ResourceSelectInput';

export { DEFAULT_OAUTH2_CONFIG };
export type { OAuth2Config };

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
    /** Resources configured on the API or API Product (with plugin icons), used to power the resource autocompletes. */
    resourceOptions?: readonly ApiResourceOption[];
    /** API plans keep the API hint. Product plans point at the product's Resources page. */
    resourceOwner?: 'API' | 'API_PRODUCT';
    /** False while an API Product's resources have not loaded yet, so an empty list is not treated as "none saved". */
    resourcesKnown?: boolean;
    error?: string;
}

const EXPRESSION_HINT = "Expressions aren't supported for API Product plans. Choose a resource from the product's Resources page.";
const DISABLED_HINT =
    "This resource is disabled. Enable it on the product's Resources page. A plan that uses a disabled resource can't be published or saved once published.";
const EMPTY_PRODUCT_HINT = "No OAuth2 resource on this API Product. Add one on the product's Resources page.";

type ResourceHint = 'error' | 'empty' | 'expression' | 'missing' | 'disabled';

/**
 * A value containing `{` is an expression, not a resource name, so the missing-resource hint must not fire for it.
 * Product plans compare the saved name as-is: publish does the same, so a trailing space is an unknown resource.
 * API plans keep the trimmed comparison.
 */
function isMissingResource(value: string, options: readonly ApiResourceOption[], matchSavedName: boolean): boolean {
    const compared = matchSavedName ? value : value.trim();
    if (!compared.trim() || compared.includes('{')) return false;
    return !options.some(option => option.name === compared);
}

function isDisabledResource(value: string, options: readonly ApiResourceOption[]): boolean {
    if (!value.trim()) return false;
    return options.some(option => option.name === value && option.enabled === false);
}

function resourceHint({
    error,
    empty,
    expression,
    missing,
    disabled,
}: {
    error?: string;
    empty: boolean;
    expression: boolean;
    missing: boolean;
    disabled: boolean;
}): ResourceHint | undefined {
    if (error) return 'error';
    if (empty) return 'empty';
    if (expression) return 'expression';
    if (missing) return 'missing';
    if (disabled) return 'disabled';
    return undefined;
}

function hintDescriptionId(fieldId: string, hint: ResourceHint | undefined): string | undefined {
    return hint ? `${fieldId}-${hint}` : undefined;
}

function hintText(hint: ResourceHint, missingHint: string, error?: string): string | undefined {
    if (hint === 'error') return error;
    if (hint === 'empty') return EMPTY_PRODUCT_HINT;
    if (hint === 'expression') return EXPRESSION_HINT;
    if (hint === 'missing') return missingHint;
    return DISABLED_HINT;
}

export function OAuth2SecurityFields({
    value,
    onChange,
    readOnly = false,
    resourceOptions = [],
    resourceOwner = 'API',
    resourcesKnown = true,
    error,
}: Readonly<OAuth2SecurityFieldsProps>) {
    const oauthOptions = resourceOptions.filter(o => o.type.startsWith('oauth2'));
    const cacheOptions = resourceOptions.filter(o => o.type.startsWith('cache'));
    const isProduct = resourceOwner === 'API_PRODUCT';

    const oauthMissing = isMissingResource(value.oauthResource, oauthOptions, isProduct);
    const cacheMissing = isMissingResource(value.oauthCacheResource, cacheOptions, isProduct);
    const noProductOAuth = isProduct && resourcesKnown && oauthOptions.length === 0;
    const missingHint = isProduct
        ? "Resource does not exist. Add it on the product's Resources page for it to be taken into account."
        : "Resource does not exist. Add it to the API's Resources section for it to be taken into account.";
    const oauthHint = resourceHint({
        error,
        empty: noProductOAuth,
        expression: isProduct && value.oauthResource.includes('{'),
        missing: oauthMissing,
        disabled: isProduct && isDisabledResource(value.oauthResource, oauthOptions),
    });
    const cacheHint = resourceHint({
        empty: false,
        expression: isProduct && value.oauthCacheResource.includes('{'),
        missing: cacheMissing,
        disabled: isProduct && isDisabledResource(value.oauthCacheResource, cacheOptions),
    });
    const oauthHintId = hintDescriptionId('oauth2-resource', oauthHint);
    const cacheHintId = hintDescriptionId('oauth2-cache-resource', cacheHint);

    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="oauth2-resource">
                    OAuth2 resource <span className="text-destructive">*</span>
                </Label>
                <ResourceSelectInput
                    id="oauth2-resource"
                    value={value.oauthResource}
                    onChange={oauthResource => onChange({ ...value, oauthResource })}
                    options={oauthOptions}
                    placeholder={
                        isProduct
                            ? 'Name of the OAuth2 resource configured on this API Product'
                            : 'Name of the OAuth2 resource configured on this API'
                    }
                    disabled={readOnly}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={oauthHintId}
                />
                {oauthHint && oauthHintId ? (
                    <p id={oauthHintId} className="text-xs text-destructive">
                        {hintText(oauthHint, missingHint, error)}
                    </p>
                ) : null}
                <p className="text-xs text-muted-foreground">
                    {isProduct
                        ? "The name of the OAuth2 authorization server resource configured on this API Product's Resources page."
                        : "The name of the OAuth2 authorization server resource configured in your API's Resources section. Supports EL."}
                </p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="oauth2-cache-resource">Cache resource</Label>
                <ResourceSelectInput
                    id="oauth2-cache-resource"
                    value={value.oauthCacheResource}
                    onChange={oauthCacheResource => onChange({ ...value, oauthCacheResource })}
                    options={cacheOptions}
                    placeholder="Name of the cache resource to store tokens"
                    disabled={readOnly}
                    aria-describedby={cacheHintId}
                />
                {cacheHint && cacheHintId ? (
                    <p id={cacheHintId} className="text-xs text-destructive">
                        {hintText(cacheHint, missingHint)}
                    </p>
                ) : null}
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
