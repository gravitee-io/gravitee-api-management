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
    Checkbox,
    Field,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Textarea,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { getHttpUrlError, isHttpUrlValid } from '../utils/dictionaryFormValidation';

export const HTTP_METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'] as const;

export type DictionaryHttpMethod = (typeof HTTP_METHOD_OPTIONS)[number];

export interface DictionaryHttpHeaderFormValue {
    id: string;
    name: string;
    value: string;
}

export interface DictionaryHttpProviderFormValue {
    url: string;
    method: DictionaryHttpMethod;
    body: string;
    headers: DictionaryHttpHeaderFormValue[];
    specification: string;
    useSystemProxy: boolean;
}

function newHeaderRow(): DictionaryHttpHeaderFormValue {
    return { id: crypto.randomUUID(), name: '', value: '' };
}

export function DictionaryHttpProviderFields({
    value,
    onChange,
    disabled,
}: Readonly<{
    value: DictionaryHttpProviderFormValue;
    onChange: (next: DictionaryHttpProviderFormValue) => void;
    disabled?: boolean;
}>) {
    const urlError = getHttpUrlError(value.url);
    const specificationMissing = value.specification.trim().length === 0;

    function updateHeader(id: string, patch: Partial<Pick<DictionaryHttpHeaderFormValue, 'name' | 'value'>>) {
        onChange({
            ...value,
            headers: value.headers.map(header => (header.id === id ? { ...header, ...patch } : header)),
        });
    }

    function removeHeader(id: string) {
        onChange({
            ...value,
            headers: value.headers.filter(header => header.id !== id),
        });
    }

    return (
        <section className="space-y-4 rounded-lg border p-4">
            <div className="space-y-1">
                <h3 className="text-sm font-semibold">HTTP Provider</h3>
            </div>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="dictionary-provider-url">
                    HTTP Service URL{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <Input
                    id="dictionary-provider-url"
                    value={value.url}
                    onChange={e => onChange({ ...value, url: e.target.value })}
                    placeholder="https://service.internal/dictionary"
                    disabled={disabled}
                    required
                    aria-invalid={urlError !== null}
                    aria-describedby={urlError !== null ? 'dictionary-provider-url-error' : undefined}
                />
                {urlError ? (
                    <p id="dictionary-provider-url-error" className="text-sm text-destructive" role="alert">
                        {urlError}
                    </p>
                ) : null}
            </Field>

            <div className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
                <Field orientation="vertical" className="gap-1.5">
                    <FieldLabel htmlFor="dictionary-provider-method">HTTP Method</FieldLabel>
                    <Select
                        value={value.method}
                        onValueChange={method => onChange({ ...value, method: method as DictionaryHttpMethod })}
                        disabled={disabled}
                    >
                        <SelectTrigger id="dictionary-provider-method">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {HTTP_METHOD_OPTIONS.map(method => (
                                <SelectItem key={method} value={method}>
                                    {method}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </Field>

                <label
                    htmlFor="dictionary-provider-system-proxy"
                    className="mb-0.5 flex cursor-pointer items-center gap-2 rounded-md px-1 py-2 text-sm"
                >
                    <Checkbox
                        id="dictionary-provider-system-proxy"
                        checked={value.useSystemProxy}
                        onCheckedChange={checked => onChange({ ...value, useSystemProxy: checked === true })}
                        disabled={disabled}
                    />
                    Use system proxy
                </label>
            </div>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="dictionary-provider-body">Request body</FieldLabel>
                <Textarea
                    id="dictionary-provider-body"
                    value={value.body}
                    onChange={e => onChange({ ...value, body: e.target.value })}
                    placeholder="Optional request body"
                    disabled={disabled}
                    rows={3}
                />
            </Field>

            <div className="space-y-3">
                <div className="flex items-center justify-between gap-2">
                    <FieldLabel className="mb-0">Headers</FieldLabel>
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="gap-1"
                        disabled={disabled}
                        onClick={() => onChange({ ...value, headers: [...value.headers, newHeaderRow()] })}
                    >
                        <PlusIcon className="size-4" aria-hidden />
                        Add
                    </Button>
                </div>

                {value.headers.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No headers. Click Add to include request headers.</p>
                ) : (
                    <div className="space-y-2">
                        {value.headers.map(header => (
                            <div key={header.id} className="grid grid-cols-[1fr_1fr_auto] gap-2">
                                <Input
                                    value={header.name}
                                    onChange={e => updateHeader(header.id, { name: e.target.value })}
                                    placeholder="Header name"
                                    disabled={disabled}
                                    aria-label="Header name"
                                />
                                <Input
                                    value={header.value}
                                    onChange={e => updateHeader(header.id, { value: e.target.value })}
                                    placeholder="Header value"
                                    disabled={disabled}
                                    aria-label="Header value"
                                />
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    disabled={disabled}
                                    onClick={() => removeHeader(header.id)}
                                    aria-label="Remove header"
                                >
                                    <Trash2Icon className="size-4" aria-hidden />
                                </Button>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="dictionary-provider-specification">
                    JOLT Specification{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <Textarea
                    id="dictionary-provider-specification"
                    value={value.specification}
                    onChange={e => onChange({ ...value, specification: e.target.value })}
                    disabled={disabled}
                    required
                    rows={8}
                    className="font-mono text-xs"
                    aria-invalid={specificationMissing}
                    aria-describedby={specificationMissing ? 'dictionary-provider-specification-error' : undefined}
                />
                {specificationMissing ? (
                    <p id="dictionary-provider-specification-error" className="text-sm text-destructive" role="alert">
                        JOLT specification is required
                    </p>
                ) : (
                    <p className="text-xs text-muted-foreground">Transforms the HTTP response into dictionary key/value properties.</p>
                )}
            </Field>
        </section>
    );
}

export function isHttpProviderFormValid(value: DictionaryHttpProviderFormValue): boolean {
    return isHttpUrlValid(value.url) && value.specification.trim().length > 0;
}

export function toProviderHeaders(headers: DictionaryHttpHeaderFormValue[]): Array<{ name: string; value: string }> {
    return headers.map(header => ({ name: header.name.trim(), value: header.value })).filter(header => header.name.length > 0);
}
