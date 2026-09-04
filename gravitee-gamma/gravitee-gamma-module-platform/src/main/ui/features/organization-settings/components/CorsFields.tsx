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

import { Checkbox, Field, FieldDescription, FieldError, FieldLabel, Input } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import type { CorsFieldReadonly, CorsFormState } from './CorsSection';
import { SystemReadonlyHint } from './SystemReadonlyHint';
import { ChipInput } from '../../shared/components/ChipInput';
import { CORS_DEFAULT_HTTP_HEADERS, CORS_HTTP_METHODS, getInvalidAllowOrigins, type CorsHttpMethod } from '../utils/corsValidators';

export const CORS_HEADER_FIELD_DESCRIPTION =
    'Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.';

export const CORS_METHODS_FIELD_DESCRIPTION =
    'Specifies the method or methods allowed when accessing the resource. This is used in response to a preflight request.';

export const CORS_MAX_AGE_DESCRIPTION = 'How long the response from a pre-flight request can be cached by clients (seconds).';

export type CorsFieldsProps = Readonly<{
    value: CorsFormState;
    disabled: boolean;
    readonly?: CorsFieldReadonly;
    onChange: (next: CorsFormState) => void;
    allowOriginId: string;
    allowHeadersId?: string;
    exposedHeadersId?: string;
    maxAgeId?: string;
    allowOriginPlaceholder?: string;
    allowOriginAddOnBlur?: boolean;
    allowOriginLead?: ReactNode;
    allowOriginTrail?: ReactNode;
    onAllowOriginChange?: (next: string[]) => void;
    methodsDescription?: string | null;
    allowHeadersDescription?: string;
    exposedHeadersDescription?: string;
    /** @deprecated Use allowHeadersDescription / exposedHeadersDescription */
    headerDescription?: string;
    maxAgeLabel?: string;
    maxAgeDescription?: string;
}>;

export function CorsFields({
    value,
    disabled,
    readonly = {},
    onChange,
    allowOriginId,
    allowHeadersId = 'cors-allow-headers',
    exposedHeadersId = 'cors-exposed-headers',
    maxAgeId = 'cors-max-age',
    allowOriginPlaceholder = '',
    allowOriginAddOnBlur = false,
    allowOriginLead,
    allowOriginTrail,
    onAllowOriginChange,
    methodsDescription = CORS_METHODS_FIELD_DESCRIPTION,
    allowHeadersDescription,
    exposedHeadersDescription,
    headerDescription = CORS_HEADER_FIELD_DESCRIPTION,
    maxAgeLabel = 'Max age',
    maxAgeDescription = CORS_MAX_AGE_DESCRIPTION,
}: CorsFieldsProps) {
    const resolvedAllowHeadersDescription = allowHeadersDescription ?? headerDescription;
    const resolvedExposedHeadersDescription = exposedHeadersDescription ?? headerDescription;
    function isFieldDisabled(key: keyof CorsFieldReadonly): boolean {
        return disabled || Boolean(readonly[key]);
    }

    function toggleMethod(method: CorsHttpMethod, checked: boolean) {
        const allowMethods = checked ? [...value.allowMethods, method] : value.allowMethods.filter(item => item !== method);
        onChange({ ...value, allowMethods });
    }

    function handleAllowOriginChange(allowOrigin: string[]) {
        if (onAllowOriginChange) {
            onAllowOriginChange(allowOrigin);
            return;
        }
        onChange({ ...value, allowOrigin });
    }

    const invalidOrigins = getInvalidAllowOrigins(value.allowOrigin);
    const originFieldError = invalidOrigins.length > 0 ? `"${invalidOrigins.join('", "')}" Regex is invalid` : null;

    return (
        <>
            <Field>
                <FieldLabel htmlFor={allowOriginId}>Allow-Origin</FieldLabel>
                {allowOriginLead}
                <SystemReadonlyHint locked={Boolean(readonly.allowOrigin)}>
                    <ChipInput
                        id={allowOriginId}
                        values={value.allowOrigin}
                        onChange={handleAllowOriginChange}
                        placeholder={allowOriginPlaceholder}
                        disabled={isFieldDisabled('allowOrigin')}
                        addOnBlur={allowOriginAddOnBlur}
                    />
                </SystemReadonlyHint>
                {allowOriginTrail}
                {originFieldError ? <FieldError>{originFieldError}</FieldError> : null}
            </Field>

            <Field>
                <FieldLabel>Access-Control-Allow-Methods</FieldLabel>
                <SystemReadonlyHint locked={Boolean(readonly.allowMethods)}>
                    <div className="flex flex-wrap gap-3">
                        {CORS_HTTP_METHODS.map(method => (
                            <label key={method} className="flex items-center gap-2 text-sm">
                                <Checkbox
                                    checked={value.allowMethods.includes(method)}
                                    onCheckedChange={checked => toggleMethod(method, checked === true)}
                                    disabled={isFieldDisabled('allowMethods')}
                                    aria-label={method}
                                />
                                {method}
                            </label>
                        ))}
                    </div>
                </SystemReadonlyHint>
                {methodsDescription ? <FieldDescription>{methodsDescription}</FieldDescription> : null}
            </Field>

            <Field>
                <FieldLabel htmlFor={allowHeadersId}>Allow-Headers</FieldLabel>
                {resolvedAllowHeadersDescription ? <FieldDescription>{resolvedAllowHeadersDescription}</FieldDescription> : null}
                <SystemReadonlyHint locked={Boolean(readonly.allowHeaders)}>
                    <ChipInput
                        id={allowHeadersId}
                        values={value.allowHeaders}
                        onChange={allowHeaders => onChange({ ...value, allowHeaders })}
                        placeholder="Content-Type, ..."
                        disabled={isFieldDisabled('allowHeaders')}
                        addOnComma
                        addOnBlur={false}
                        suggestions={CORS_DEFAULT_HTTP_HEADERS}
                    />
                </SystemReadonlyHint>
            </Field>

            <Field>
                <FieldLabel htmlFor={exposedHeadersId}>Exposed-Headers</FieldLabel>
                {resolvedExposedHeadersDescription ? <FieldDescription>{resolvedExposedHeadersDescription}</FieldDescription> : null}
                <SystemReadonlyHint locked={Boolean(readonly.exposedHeaders)}>
                    <ChipInput
                        id={exposedHeadersId}
                        values={value.exposedHeaders}
                        onChange={exposedHeaders => onChange({ ...value, exposedHeaders })}
                        placeholder="Content-Type, ..."
                        disabled={isFieldDisabled('exposedHeaders')}
                        addOnComma
                        addOnBlur={false}
                        suggestions={CORS_DEFAULT_HTTP_HEADERS}
                    />
                </SystemReadonlyHint>
            </Field>

            <Field>
                <FieldLabel htmlFor={maxAgeId}>{maxAgeLabel}</FieldLabel>
                {maxAgeDescription ? <FieldDescription>{maxAgeDescription}</FieldDescription> : null}
                <SystemReadonlyHint locked={Boolean(readonly.maxAge)}>
                    <Input
                        id={maxAgeId}
                        type="number"
                        min={0}
                        value={value.maxAge}
                        onChange={event => onChange({ ...value, maxAge: event.target.value })}
                        disabled={isFieldDisabled('maxAge')}
                    />
                </SystemReadonlyHint>
            </Field>
        </>
    );
}
