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

import { Button, Card, CardContent, Field, FieldDescription, FieldError, FieldLabel, Input } from '@gravitee/graphene-core';
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
                                    required
                                    invalid={showErrors && !!groupsError}
                                    error={groupsError}
                                    disabled={disabled}
                                    placeholder="Select groups"
                                    emptyMessage="No groups available"
                                    onChange={selected => patchMapping(index, { groups: selected })}
                                />
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
