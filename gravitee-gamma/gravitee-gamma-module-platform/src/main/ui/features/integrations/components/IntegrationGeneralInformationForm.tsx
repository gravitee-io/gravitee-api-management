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
import { Button, Field, FieldError, FieldLabel, Input, Textarea } from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';

import { notify } from '../../../shared/notify';
import { useUpdateIntegration } from '../hooks/useUpdateIntegration';
import type { Integration, UpdateIntegrationRequest } from '../types/integration';
import { integrationErrorMessage } from '../utils/integrationErrorMessage';
import { INTEGRATION_DESCRIPTION_MAX, validateIntegrationForm, type IntegrationFormValues } from '../utils/integrationForm';

interface IntegrationGeneralInformationFormProps {
    integration: Integration;
}

function toFormValues(integration: Pick<Integration, 'name' | 'description'>): IntegrationFormValues {
    return { name: integration.name, description: integration.description ?? '' };
}

function buildUpdateRequest(integration: Integration, values: IntegrationFormValues): UpdateIntegrationRequest {
    return { ...values, groups: integration.groups ?? [] };
}

export function IntegrationGeneralInformationForm({ integration }: IntegrationGeneralInformationFormProps) {
    const updateIntegration = useUpdateIntegration();
    const [baseline, setBaseline] = useState(() => toFormValues(integration));
    const [name, setName] = useState(baseline.name);
    const [description, setDescription] = useState(baseline.description);

    const errors = validateIntegrationForm({ name, description });
    const isDirty = name !== baseline.name || description !== baseline.description;
    const isValid = !errors.name && !errors.description;

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!isDirty || !isValid) return;

        try {
            const updated = await updateIntegration.mutateAsync({
                integrationId: integration.id,
                request: buildUpdateRequest(integration, { name, description }),
            });
            const saved = toFormValues(updated);
            setBaseline(saved);
            setName(saved.name);
            setDescription(saved.description);
            notify.success('Integration successfully updated!');
        } catch (error) {
            notify.error(integrationErrorMessage(error));
        }
    }

    return (
        <form className="space-y-4" onSubmit={event => void handleSubmit(event)}>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="integration-general-name" required>
                    Name
                </FieldLabel>
                <Input
                    id="integration-general-name"
                    value={name}
                    onChange={event => setName(event.target.value)}
                    aria-invalid={Boolean(errors.name)}
                />
                {errors.name ? <FieldError>{errors.name}</FieldError> : null}
            </Field>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="integration-general-description">Description</FieldLabel>
                <Textarea
                    id="integration-general-description"
                    value={description}
                    maxLength={INTEGRATION_DESCRIPTION_MAX}
                    rows={3}
                    onChange={event => setDescription(event.target.value)}
                    aria-invalid={Boolean(errors.description)}
                />
                {errors.description ? <FieldError>{errors.description}</FieldError> : null}
            </Field>
            {isDirty && isValid ? (
                <div className="bg-background sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 border-t py-4">
                    <p className="text-muted-foreground mr-auto text-sm">You have unsaved changes.</p>
                    <Button type="submit" size="sm" disabled={updateIntegration.isPending}>
                        Save
                    </Button>
                </div>
            ) : null}
        </form>
    );
}
