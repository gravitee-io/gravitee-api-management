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

import { Alert, AlertDescription, Checkbox, Input } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { ChipInput } from '../../shared/components/ChipInput';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { CORS_HTTP_METHODS, getInvalidAllowOrigins, type CorsHttpMethod } from '../utils/corsValidators';

export interface CorsFormState {
    allowOrigin: string[];
    allowMethods: string[];
    allowHeaders: string[];
    exposedHeaders: string[];
    maxAge: string;
}

export interface CorsFieldReadonly {
    allowOrigin?: boolean;
    allowMethods?: boolean;
    allowHeaders?: boolean;
    exposedHeaders?: boolean;
    maxAge?: boolean;
}

export function CorsSection({
    value,
    disabled,
    readonly = {},
    onChange,
}: Readonly<{
    value: CorsFormState;
    disabled: boolean;
    readonly?: CorsFieldReadonly;
    onChange: (next: CorsFormState) => void;
}>) {
    const [pendingWildcard, setPendingWildcard] = useState(false);
    const invalidOrigins = getInvalidAllowOrigins(value.allowOrigin);

    function isFieldDisabled(key: keyof CorsFieldReadonly): boolean {
        return disabled || Boolean(readonly[key]);
    }

    function handleOriginsChange(next: string[]) {
        if (next.includes('*') && !value.allowOrigin.includes('*')) {
            setPendingWildcard(true);
            return;
        }
        onChange({ ...value, allowOrigin: next });
    }

    function toggleMethod(method: CorsHttpMethod, checked: boolean) {
        const allowMethods = checked ? [...value.allowMethods, method] : value.allowMethods.filter(item => item !== method);
        onChange({ ...value, allowMethods });
    }

    return (
        <section className="rounded-lg border p-4 space-y-6">
            <div className="space-y-1.5">
                <label htmlFor="cors-allow-origin" className="text-sm font-medium">
                    Allow-Origin
                </label>
                <ChipInput
                    id="cors-allow-origin"
                    values={value.allowOrigin}
                    onChange={handleOriginsChange}
                    placeholder="*, https://mydomain.com, (http|https).*.mydomain.com, ..."
                    disabled={isFieldDisabled('allowOrigin')}
                />
                <p className="text-xs text-muted-foreground">
                    The origin parameter specifies a URI that may access the resource. Scheme, domain and port are part of the same-origin
                    definition. If you choose to enable * it means that it allows all requests, regardless of origin. Regular expressions
                    are also supported.
                </p>
                {invalidOrigins.length > 0 ? (
                    <p className="text-sm text-destructive" role="alert">
                        "{invalidOrigins.join('", "')}" Regex is invalid
                    </p>
                ) : null}
                {value.allowOrigin.includes('*') ? (
                    <Alert>
                        <InfoIcon className="size-4" />
                        <AlertDescription>
                            Setting <span className="font-mono">*</span> exposes this management API to any website. Make sure that is
                            intended.
                        </AlertDescription>
                    </Alert>
                ) : null}
            </div>

            <div className="space-y-1.5">
                <p className="text-sm font-medium">Access-Control-Allow-Methods</p>
                <div className="flex flex-wrap gap-3">
                    {CORS_HTTP_METHODS.map(method => (
                        <label key={method} className="flex items-center gap-2 text-sm">
                            <Checkbox
                                checked={value.allowMethods.includes(method)}
                                onCheckedChange={checked => toggleMethod(method, checked === true)}
                                disabled={isFieldDisabled('allowMethods')}
                                aria-label={method}
                            />
                            <span className="font-mono text-xs">{method}</span>
                        </label>
                    ))}
                </div>
                <p className="text-xs text-muted-foreground">
                    Specifies the method or methods allowed when accessing the resource. This is used in response to a preflight request.
                </p>
            </div>

            <div className="space-y-1.5">
                <label htmlFor="cors-allow-headers" className="text-sm font-medium">
                    Allow-Headers
                </label>
                <ChipInput
                    id="cors-allow-headers"
                    values={value.allowHeaders}
                    onChange={allowHeaders => onChange({ ...value, allowHeaders })}
                    placeholder="Content-Type, ..."
                    disabled={isFieldDisabled('allowHeaders')}
                    addOnComma
                />
                <p className="text-xs text-muted-foreground">
                    Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.
                </p>
            </div>

            <div className="space-y-1.5">
                <label htmlFor="cors-exposed-headers" className="text-sm font-medium">
                    Exposed-Headers
                </label>
                <ChipInput
                    id="cors-exposed-headers"
                    values={value.exposedHeaders}
                    onChange={exposedHeaders => onChange({ ...value, exposedHeaders })}
                    placeholder="Content-Type, ..."
                    disabled={isFieldDisabled('exposedHeaders')}
                    addOnComma
                />
                <p className="text-xs text-muted-foreground">
                    Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.
                </p>
            </div>

            <div className="space-y-1.5">
                <label htmlFor="cors-max-age" className="text-sm font-medium">
                    Max age
                </label>
                <Input
                    id="cors-max-age"
                    type="number"
                    min={0}
                    value={value.maxAge}
                    onChange={e => onChange({ ...value, maxAge: e.target.value })}
                    disabled={isFieldDisabled('maxAge')}
                />
                <p className="text-xs text-muted-foreground">
                    How long the response from a pre-flight request can be cached by clients (seconds).
                </p>
            </div>

            <ConfirmDialog
                open={pendingWildcard}
                onOpenChange={open => !open && setPendingWildcard(false)}
                title="Are you sure?"
                description="Do you want to remove all cross-origin restrictions?"
                confirmLabel="Yes, I want to allow all origins."
                onConfirm={() => {
                    onChange({ ...value, allowOrigin: [...value.allowOrigin, '*'] });
                    setPendingWildcard(false);
                }}
            />
        </section>
    );
}
