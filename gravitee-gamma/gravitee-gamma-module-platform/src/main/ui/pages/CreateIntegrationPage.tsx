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

import { Button, Field, FieldError, FieldLabel, Input, PageFocused, Textarea } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { CreateA2aIntegration } from '../features/integrations/components/CreateA2aIntegration';
import { IntegrationProviderSelector } from '../features/integrations/components/IntegrationProviderSelector';
import { useCreateIntegration } from '../features/integrations/hooks/useCreateIntegration';
import { validateIntegrationForm } from '../features/integrations/utils/integrationForm';
import { A2A_PROVIDER } from '../features/integrations/utils/integrationKind';
import { notify } from '../shared/notify';

export function CreateIntegrationPage() {
    const navigate = useNavigate();
    const createIntegration = useCreateIntegration();
    const [provider, setProvider] = useState<string | undefined>(undefined);
    const [a2aSubmitting, setA2aSubmitting] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [nameTouched, setNameTouched] = useState(false);
    const [descriptionTouched, setDescriptionTouched] = useState(false);

    const errors = validateIntegrationForm({ name, description });
    const canCreate = Boolean(provider && !errors.name && !errors.description && !createIntegration.isPending);

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canCreate || !provider) return;

        try {
            const created = await createIntegration.mutateAsync({ name, description, provider });
            notify.success(`Integration ${name} created successfully`);
            navigate(`../${created.id}`);
        } catch (error) {
            notify.error(error, 'Failed to create integration.');
        }
    }

    return (
        <PageFocused>
            <div className="space-y-6">
                <div className="space-y-2">
                    <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" onClick={() => navigate('..')}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Integrations
                    </Button>
                    <h1 className="text-2xl font-semibold tracking-tight">Create a new integration</h1>
                </div>
                <IntegrationProviderSelector value={provider} onChange={setProvider} disabled={a2aSubmitting} />
                {provider === A2A_PROVIDER ? <CreateA2aIntegration onSubmittingChange={setA2aSubmitting} /> : null}
                {provider && provider !== A2A_PROVIDER ? (
                    <form className="space-y-4" onSubmit={event => void handleSubmit(event)}>
                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="integration-name" required>
                                Name
                            </FieldLabel>
                            <Input
                                id="integration-name"
                                value={name}
                                onChange={event => {
                                    setNameTouched(true);
                                    setName(event.target.value);
                                }}
                                onBlur={() => setNameTouched(true)}
                                aria-invalid={nameTouched && Boolean(errors.name)}
                            />
                            {nameTouched && errors.name ? <FieldError>{errors.name}</FieldError> : null}
                        </Field>
                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="integration-description">Description</FieldLabel>
                            <Textarea
                                id="integration-description"
                                value={description}
                                rows={3}
                                onChange={event => {
                                    setDescriptionTouched(true);
                                    setDescription(event.target.value);
                                }}
                                onBlur={() => setDescriptionTouched(true)}
                                aria-invalid={descriptionTouched && Boolean(errors.description)}
                            />
                            {descriptionTouched && errors.description ? <FieldError>{errors.description}</FieldError> : null}
                        </Field>
                        <Button type="submit" disabled={!canCreate}>
                            Create
                        </Button>
                    </form>
                ) : null}
            </div>
        </PageFocused>
    );
}
