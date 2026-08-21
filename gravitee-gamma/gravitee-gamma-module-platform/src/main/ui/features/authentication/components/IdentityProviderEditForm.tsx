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

import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { useEffect, useMemo, useState, type FormEvent } from 'react';

import { IdentityProviderConfigurationFields, IdentityProviderUserProfileFields } from './IdentityProviderConfigurationFields';
import { IdentityProviderGeneralFields } from './IdentityProviderGeneralFields';
import { IdentityProviderGroupMappings } from './IdentityProviderGroupMappings';
import type { IdentityProviderMappingOption } from './IdentityProviderMappingMultiSelect';
import { IdentityProviderRoleMappings } from './IdentityProviderRoleMappings';
import { notify } from '../../../shared/notify';
import { useUpdateIdentityProvider } from '../hooks/useIdentityProviderMutations';
import type { IdentityProvider } from '../types/identityProvider';
import { hasUserProfileMapping } from '../utils/identityProviderDisplay';
import {
    formToUpdatePayload,
    identityProviderToForm,
    isIdentityProviderFormDirty,
    validateIdentityProviderForm,
    type IdentityProviderFormState,
} from '../utils/identityProviderForm';

export function IdentityProviderEditForm({
    provider,
    groups,
    environments,
    organizationRoles,
    environmentRoles,
    canUpdate,
    mappingsDisabled = false,
    onDirtyChange,
    onCancel,
}: Readonly<{
    provider: IdentityProvider;
    groups: readonly IdentityProviderMappingOption[];
    environments: readonly IdentityProviderMappingOption[];
    organizationRoles: readonly IdentityProviderMappingOption[];
    environmentRoles: readonly IdentityProviderMappingOption[];
    canUpdate: boolean;
    mappingsDisabled?: boolean;
    onDirtyChange?: (dirty: boolean) => void;
    onCancel: () => void;
}>) {
    const updateMutation = useUpdateIdentityProvider(provider.id);
    const environmentIds = useMemo(() => environments.map(environment => environment.id), [environments]);
    const [baseline, setBaseline] = useState(() => identityProviderToForm(provider, environmentIds));
    const [form, setForm] = useState<IdentityProviderFormState>(baseline);
    const [showErrors, setShowErrors] = useState(false);

    const errors = validateIdentityProviderForm(form);
    const dirty = isIdentityProviderFormDirty(form, baseline);
    const disabled = !canUpdate || updateMutation.isPending;
    const mappingFieldsDisabled = disabled || mappingsDisabled;
    const showProfile = hasUserProfileMapping(form.type);

    useEffect(() => {
        onDirtyChange?.(dirty);
    }, [dirty, onDirtyChange]);

    function patchForm(patch: Partial<IdentityProviderFormState>) {
        setForm(prev => ({ ...prev, ...patch }));
    }

    function handleDiscard() {
        setForm(baseline);
        setShowErrors(false);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canUpdate) return;
        if (Object.keys(errors).length > 0) {
            setShowErrors(true);
            return;
        }
        try {
            const updated = await updateMutation.mutateAsync(formToUpdatePayload(form));
            const next = identityProviderToForm(updated, environmentIds);
            setBaseline(next);
            setForm(next);
            setShowErrors(false);
            notify.success('Identity provider successfully saved!');
        } catch (error: unknown) {
            notify.error(error, 'Failed to update identity provider');
        }
    }

    return (
        <form className="space-y-6" onSubmit={handleSubmit}>
            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h2 className="text-base font-semibold">General</h2>
                    <IdentityProviderGeneralFields
                        form={form}
                        showErrors={showErrors}
                        errors={errors}
                        disabled={disabled}
                        onChange={patchForm}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <h2 className="text-base font-semibold">Configuration</h2>
                    <IdentityProviderConfigurationFields
                        form={form}
                        showErrors={showErrors}
                        errors={errors}
                        disabled={disabled}
                        onPatch={patch => setForm(prev => ({ ...prev, configuration: { ...prev.configuration, ...patch } }))}
                    />
                </CardContent>
            </Card>

            {showProfile ? (
                <Card>
                    <CardContent className="space-y-4 pt-6">
                        <h2 className="text-base font-semibold">User profile mapping</h2>
                        <IdentityProviderUserProfileFields
                            form={form}
                            showErrors={showErrors}
                            errors={errors}
                            disabled={disabled}
                            onChange={userProfileMapping => patchForm({ userProfileMapping })}
                        />
                    </CardContent>
                </Card>
            ) : null}

            <IdentityProviderGroupMappings
                mappings={form.groupMappings}
                groups={groups}
                showErrors={showErrors}
                errors={errors}
                disabled={mappingFieldsDisabled}
                onChange={groupMappings => patchForm({ groupMappings })}
            />

            <IdentityProviderRoleMappings
                mappings={form.roleMappings}
                environments={environments}
                organizationRoles={organizationRoles}
                environmentRoles={environmentRoles}
                showErrors={showErrors}
                errors={errors}
                disabled={mappingFieldsDisabled}
                onChange={roleMappings => patchForm({ roleMappings })}
            />

            {canUpdate ? (
                <div className="sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 border-t bg-background py-4">
                    <Button type="button" variant="outline" onClick={onCancel} disabled={updateMutation.isPending}>
                        Cancel
                    </Button>
                    <Button type="button" variant="outline" disabled={!dirty || updateMutation.isPending} onClick={handleDiscard}>
                        Discard
                    </Button>
                    <Button type="submit" disabled={!dirty || updateMutation.isPending}>
                        {updateMutation.isPending ? 'Saving…' : 'Update'}
                    </Button>
                </div>
            ) : null}
        </form>
    );
}
