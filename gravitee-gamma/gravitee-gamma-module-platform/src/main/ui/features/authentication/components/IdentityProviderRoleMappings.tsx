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
    Card,
    CardContent,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { IdentityProviderMappingMultiSelect, type IdentityProviderMappingOption } from './IdentityProviderMappingMultiSelect';
import { RequiredMark } from './RequiredMark';
import { emptyRoleMapping, type IdentityProviderRoleMappingForm } from '../utils/identityProviderForm';

function environmentRows(
    catalog: readonly IdentityProviderMappingOption[],
    mapping: IdentityProviderRoleMappingForm,
): IdentityProviderMappingOption[] {
    const byId = new Map(catalog.map(environment => [environment.id, environment]));
    const ids = [...new Set([...catalog.map(environment => environment.id), ...Object.keys(mapping.environments)])];
    return ids.map(id => byId.get(id) ?? { id, name: id });
}

export function IdentityProviderRoleMappings({
    mappings,
    environments,
    organizationRoles,
    environmentRoles,
    showErrors,
    errors,
    disabled,
    onChange,
}: Readonly<{
    mappings: IdentityProviderRoleMappingForm[];
    environments: readonly IdentityProviderMappingOption[];
    organizationRoles: readonly IdentityProviderMappingOption[];
    environmentRoles: readonly IdentityProviderMappingOption[];
    showErrors: boolean;
    errors: Record<string, string>;
    disabled: boolean;
    onChange: (mappings: IdentityProviderRoleMappingForm[]) => void;
}>) {
    const environmentIds = environments.map(environment => environment.id);

    function patchMapping(index: number, patch: Partial<IdentityProviderRoleMappingForm>) {
        onChange(mappings.map((mapping, mappingIndex) => (mappingIndex === index ? { ...mapping, ...patch } : mapping)));
    }

    return (
        <Card>
            <CardContent className="space-y-4 pt-6">
                <h2 className="text-base font-semibold">Roles Mapping</h2>
                {mappings.map((mapping, index) => {
                    const conditionError = errors[`roleMappings.${index}.condition`];
                    const organizationsError = errors[`roleMappings.${index}.organizations`];
                    return (
                        <Card key={`role-mapping-${index}`}>
                            <CardContent className="space-y-4 pt-6">
                                <Field>
                                    <FieldLabel htmlFor={`idp-role-condition-${index}`}>
                                        Condition <RequiredMark />
                                    </FieldLabel>
                                    <Input
                                        id={`idp-role-condition-${index}`}
                                        value={mapping.condition}
                                        disabled={disabled}
                                        aria-required="true"
                                        aria-invalid={showErrors && !!conditionError}
                                        aria-describedby={
                                            showErrors && conditionError
                                                ? `idp-role-condition-${index}-error`
                                                : `idp-role-condition-${index}-hint`
                                        }
                                        onChange={event => patchMapping(index, { condition: event.target.value })}
                                    />
                                    <FieldDescription id={`idp-role-condition-${index}-hint`}>
                                        The condition which should be validated to associate below roles at login time.
                                    </FieldDescription>
                                    {showErrors && conditionError ? (
                                        <FieldError id={`idp-role-condition-${index}-error`}>{conditionError}</FieldError>
                                    ) : null}
                                </Field>
                                <IdentityProviderMappingMultiSelect
                                    id={`idp-role-org-${index}`}
                                    label="Organization roles"
                                    values={mapping.organizations}
                                    options={organizationRoles}
                                    required
                                    invalid={showErrors && !!organizationsError}
                                    error={organizationsError}
                                    disabled={disabled}
                                    placeholder="Select organization roles"
                                    emptyMessage="No organization roles available"
                                    onChange={organizations => patchMapping(index, { organizations })}
                                />
                                <Table aria-label={`Environment roles for mapping ${index + 1}`}>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Name</TableHead>
                                            <TableHead>Description</TableHead>
                                            <TableHead>Roles selected</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {environmentRows(environments, mapping).map(environment => (
                                            <TableRow key={environment.id}>
                                                <TableCell>{environment.name}</TableCell>
                                                <TableCell className="text-muted-foreground">
                                                    {environment.description?.trim() || '—'}
                                                </TableCell>
                                                <TableCell>
                                                    <IdentityProviderMappingMultiSelect
                                                        id={`idp-role-env-${index}-${environment.id}`}
                                                        label="Roles"
                                                        values={mapping.environments[environment.id] ?? []}
                                                        options={environmentRoles}
                                                        disabled={disabled}
                                                        placeholder="Select roles"
                                                        emptyMessage="No environment roles available"
                                                        hideLabel
                                                        onChange={roles =>
                                                            patchMapping(index, {
                                                                environments: {
                                                                    ...mapping.environments,
                                                                    [environment.id]: roles,
                                                                },
                                                            })
                                                        }
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
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
                <Button
                    type="button"
                    variant="outline"
                    disabled={disabled}
                    onClick={() => onChange([...mappings, emptyRoleMapping(environmentIds)])}
                >
                    <PlusIcon className="size-4" aria-hidden />
                    Add role mapping
                </Button>
            </CardContent>
        </Card>
    );
}
