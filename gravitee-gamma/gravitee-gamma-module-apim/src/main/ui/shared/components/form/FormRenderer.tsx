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
    Field,
    FieldContent,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { useMemo } from 'react';

import type { ApiCreationState } from '../../../features/apis/types/models';
import type { FieldConfig } from '../../../utils/fieldRegistry';

export type FormRendererProps = Readonly<{
    fields: readonly FieldConfig[];
    state: ApiCreationState;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
    errors: Record<string, string>;
}>;

export function FormRenderer({ fields, state, getValue, updateField, errors }: FormRendererProps) {
    const visibleFields = useMemo(() => fields.filter(f => (f.visible ? f.visible(state) : true)), [fields, state]);

    return (
        <div className="space-y-5">
            {visibleFields.map(field => {
                const value = getValue(field.bind);
                const error = errors[field.bind];
                const fieldErrors = error ? [{ message: error }] : undefined;

                return (
                    <Field key={field.id} orientation="vertical">
                        <FieldLabel htmlFor={field.id}>
                            {field.label}
                            {field.required ? <span className="ml-1 text-destructive">*</span> : null}
                        </FieldLabel>
                        {field.description ? <FieldDescription>{field.description}</FieldDescription> : null}
                        <FieldContent>
                            {field.type === 'input' ? (
                                field.inputKind === 'textarea' ? (
                                    <Textarea
                                        id={field.id}
                                        value={typeof value === 'string' ? value : ''}
                                        aria-invalid={Boolean(error)}
                                        onChange={event => updateField(field.bind, event.target.value)}
                                    />
                                ) : (
                                    <Input
                                        id={field.id}
                                        type={field.inputType ?? 'text'}
                                        pattern={field.pattern}
                                        title={field.validationTitle}
                                        value={typeof value === 'string' ? value : ''}
                                        aria-invalid={Boolean(error)}
                                        onChange={event => updateField(field.bind, event.target.value)}
                                    />
                                )
                            ) : null}

                            {field.type === 'select' ? (
                                <Select value={typeof value === 'string' ? value : ''} onValueChange={v => updateField(field.bind, v)}>
                                    <SelectTrigger id={field.id} aria-invalid={Boolean(error)}>
                                        <SelectValue placeholder="Select…" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {(field.options ?? []).map(opt => (
                                            <SelectItem key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            ) : null}

                            {field.type === 'switch' ? (
                                <div className="flex items-center justify-between gap-3 rounded-md border bg-card px-3 py-2">
                                    <div className="min-w-0">
                                        <div className="text-sm font-medium text-foreground">{field.label}</div>
                                        {field.description ? (
                                            <div className="text-xs text-muted-foreground">{field.description}</div>
                                        ) : null}
                                    </div>
                                    <Switch
                                        id={field.id}
                                        checked={Boolean(value)}
                                        onCheckedChange={checked => updateField(field.bind, checked)}
                                    />
                                </div>
                            ) : null}
                        </FieldContent>
                        <FieldError errors={fieldErrors} />
                    </Field>
                );
            })}
        </div>
    );
}
