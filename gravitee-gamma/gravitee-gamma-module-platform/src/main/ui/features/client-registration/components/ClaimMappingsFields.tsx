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
import { Button, Field, FieldDescription, FieldError, FieldLabel, Input } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import type { ClaimMappingRow } from '../utils/validateProviderForm';

export function ClaimMappingsFields({
    rows,
    onChange,
    disabled,
    error,
    showErrors,
}: Readonly<{
    rows: ClaimMappingRow[];
    onChange: (next: ClaimMappingRow[]) => void;
    disabled: boolean;
    error?: string;
    showErrors: boolean;
}>) {
    return (
        <div className="space-y-3">
            <h2 className="text-base font-semibold">Claim Mappings</h2>
            <p className="text-sm text-muted-foreground">
                Inject claims persisted from the user&apos;s identity provider into the dynamic client registration request, so the provider
                receives tenant or user context. Each mapping takes a claim name and the field of the registration request to write it to. A
                claim the user does not have is skipped.
            </p>
            <Field>
                <FieldLabel>Claim name (key) and registration request field (value)</FieldLabel>
                <div className="space-y-2">
                    {rows.map((row, index) => (
                        <div key={index} className="flex items-start gap-2">
                            <Input
                                aria-label={`Claim name ${index + 1}`}
                                value={row.key}
                                disabled={disabled}
                                placeholder="Claim name"
                                onChange={event => {
                                    const next = [...rows];
                                    next[index] = { ...row, key: event.target.value };
                                    onChange(next);
                                }}
                            />
                            <Input
                                aria-label={`Registration field ${index + 1}`}
                                value={row.value}
                                disabled={disabled}
                                placeholder="metadata.organization"
                                onChange={event => {
                                    const next = [...rows];
                                    next[index] = { ...row, value: event.target.value };
                                    onChange(next);
                                }}
                            />
                            {disabled ? null : (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    aria-label={`Remove mapping ${index + 1}`}
                                    onClick={() => onChange(rows.filter((_, rowIndex) => rowIndex !== index))}
                                >
                                    <Trash2Icon className="size-4" aria-hidden />
                                </Button>
                            )}
                        </div>
                    ))}
                </div>
                {disabled ? null : (
                    <Button type="button" variant="outline" size="sm" onClick={() => onChange([...rows, { key: '', value: '' }])}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add mapping
                    </Button>
                )}
                {showErrors && error ? <FieldError>{error}</FieldError> : null}
                <FieldDescription>
                    Only extension fields can be written, using dot notation for nested ones such as <code>metadata.organization</code>.
                    Standard registration fields defined by the specification, <code>client_name</code> among them, are rejected.
                </FieldDescription>
            </Field>
        </div>
    );
}
