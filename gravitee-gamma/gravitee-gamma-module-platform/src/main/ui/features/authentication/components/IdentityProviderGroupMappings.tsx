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

import { Button, Card, CardContent, Field, FieldDescription, FieldError, FieldLabel, Input, Textarea } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { IdentityProviderMappingMultiSelect, type IdentityProviderMappingOption } from './IdentityProviderMappingMultiSelect';
import { RequiredMark } from './RequiredMark';
import { emptyGroupMapping, type IdentityProviderGroupMappingForm } from '../utils/identityProviderForm';

export function IdentityProviderGroupMappings({
    mappings,
    groups,
    showErrors,
    errors,
    disabled,
    onChange,
}: Readonly<{
    mappings: IdentityProviderGroupMappingForm[];
    groups: readonly IdentityProviderMappingOption[];
    showErrors: boolean;
    errors: Record<string, string>;
    disabled: boolean;
    onChange: (mappings: IdentityProviderGroupMappingForm[]) => void;
}>) {
    function patchMapping(index: number, patch: Partial<IdentityProviderGroupMappingForm>) {
        onChange(mappings.map((mapping, mappingIndex) => (mappingIndex === index ? { ...mapping, ...patch } : mapping)));
    }

    return (
        <Card>
            <CardContent className="space-y-4 pt-6">
                <h2 className="text-base font-semibold">Groups Mapping</h2>
                {mappings.map((mapping, index) => {
                    const conditionError = errors[`groupMappings.${index}.condition`];
                    const groupsError = errors[`groupMappings.${index}.groups`];
                    const groupsInvalid = showErrors && !!groupsError;
                    const expressionsError = errors[`groupMappings.${index}.groupExpressions`];
                    const expressionsInvalid = showErrors && !!expressionsError;
                    const expressionsId = `idp-group-expressions-${index}`;
                    return (
                        <Card key={`group-mapping-${index}`}>
                            <CardContent className="space-y-4 pt-6">
                                <Field>
                                    <FieldLabel htmlFor={`idp-group-condition-${index}`}>
                                        Condition <RequiredMark />
                                    </FieldLabel>
                                    <Input
                                        id={`idp-group-condition-${index}`}
                                        value={mapping.condition}
                                        disabled={disabled}
                                        aria-required="true"
                                        aria-invalid={showErrors && !!conditionError}
                                        aria-describedby={
                                            showErrors && conditionError
                                                ? `idp-group-condition-${index}-error`
                                                : `idp-group-condition-${index}-hint`
                                        }
                                        onChange={event => patchMapping(index, { condition: event.target.value })}
                                    />
                                    <FieldDescription id={`idp-group-condition-${index}-hint`}>
                                        The condition which should be validated to associate below groups at login time.
                                    </FieldDescription>
                                    {showErrors && conditionError ? (
                                        <FieldError id={`idp-group-condition-${index}-error`}>{conditionError}</FieldError>
                                    ) : null}
                                </Field>
                                <IdentityProviderMappingMultiSelect
                                    id={`idp-group-groups-${index}`}
                                    label="Group"
                                    values={mapping.groups}
                                    options={groups}
                                    invalid={groupsInvalid}
                                    error={groupsError}
                                    disabled={disabled}
                                    placeholder="Select groups"
                                    emptyMessage="No groups available"
                                    onChange={selected => patchMapping(index, { groups: selected })}
                                />
                                <Field>
                                    <FieldLabel htmlFor={expressionsId}>Group name expressions</FieldLabel>
                                    <Textarea
                                        id={expressionsId}
                                        value={mapping.groupExpressions}
                                        disabled={disabled}
                                        rows={3}
                                        className="font-mono"
                                        aria-invalid={groupsInvalid || expressionsInvalid}
                                        aria-describedby={
                                            expressionsInvalid ? `${expressionsId}-hint ${expressionsId}-error` : `${expressionsId}-hint`
                                        }
                                        onChange={event => patchMapping(index, { groupExpressions: event.target.value })}
                                    />
                                    <FieldDescription id={`${expressionsId}-hint`}>
                                        One single-line expression per line, e.g. {"{#jsonPath(#profile, '$.groups')}"}. Each line must
                                        resolve to group names, matched exactly against existing group names. Text around an expression, or
                                        several expressions on one line, make up a single name, e.g. {"team-{#profile['department']}"}. The
                                        syntax is checked on save.
                                    </FieldDescription>
                                    {expressionsInvalid ? <FieldError id={`${expressionsId}-error`}>{expressionsError}</FieldError> : null}
                                </Field>
                                <div className="flex justify-end">
                                    <Button
                                        type="button"
                                        variant="outline"
                                        disabled={disabled}
                                        onClick={() => onChange(mappings.filter((_, mappingIndex) => mappingIndex !== index))}
                                    >
                                        <Trash2Icon className="size-4" aria-hidden />
                                        Delete
                                    </Button>
                                </div>
                            </CardContent>
                        </Card>
                    );
                })}
                <Button type="button" variant="outline" disabled={disabled} onClick={() => onChange([...mappings, emptyGroupMapping()])}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add group mapping
                </Button>
            </CardContent>
        </Card>
    );
}
