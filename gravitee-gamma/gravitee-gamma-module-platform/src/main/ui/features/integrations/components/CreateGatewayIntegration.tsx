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
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { notify } from '../../../shared/notify';
import { useCreateIntegration } from '../hooks/useCreateIntegration';
import { validateIntegrationForm } from '../utils/integrationForm';

export function CreateGatewayIntegration({
    provider,
    onCancel,
    onSubmittingChange,
}: Readonly<{ provider: string; onCancel: () => void; onSubmittingChange?: (isSubmitting: boolean) => void }>) {
    const navigate = useNavigate();
    const createIntegration = useCreateIntegration();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [nameTouched, setNameTouched] = useState(false);
    const [descriptionTouched, setDescriptionTouched] = useState(false);

    // isPending reaches React one macrotask after mutateAsync starts, so only a ref blocks a second submit in the same task.
    const submitInFlight = useRef(false);

    useEffect(() => {
        onSubmittingChange?.(createIntegration.isPending);
    }, [createIntegration.isPending, onSubmittingChange]);

    const errors = validateIntegrationForm({ name, description });
    const canCreate = !errors.name && !errors.description && !createIntegration.isPending;

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canCreate) return;
        if (submitInFlight.current) return;
        submitInFlight.current = true;

        try {
            const created = await createIntegration.mutateAsync({ name, description, provider });
            notify.success(`Integration ${name} created successfully`);
            navigate(`../${created.id}`);
        } catch (error) {
            notify.error(error, 'Failed to create integration.');
        } finally {
            submitInFlight.current = false;
        }
    }

    return (
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
            <div className="flex justify-end gap-2 border-t pt-4">
                <Button type="button" variant="outline" disabled={createIntegration.isPending} onClick={onCancel}>
                    Cancel
                </Button>
                <Button type="submit" disabled={!canCreate}>
                    Create integration
                </Button>
            </div>
        </form>
    );
}
