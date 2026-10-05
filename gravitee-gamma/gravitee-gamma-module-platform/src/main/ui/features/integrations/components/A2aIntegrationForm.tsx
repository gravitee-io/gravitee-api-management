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
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useRef, useState, type FormEvent } from 'react';

import { type A2aIntegrationFormValues, isA2aIntegrationFormValid, validateA2aIntegrationForm } from '../utils/a2aIntegrationForm';

interface WellKnownUrlEntry {
    id: number;
    value: string;
}

export function A2aIntegrationForm({
    onSubmit,
    isSubmitting = false,
}: Readonly<{ onSubmit: (values: A2aIntegrationFormValues) => void; isSubmitting?: boolean }>) {
    const nextEntryId = useRef(0);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [entries, setEntries] = useState<WellKnownUrlEntry[]>([]);
    const [nameTouched, setNameTouched] = useState(false);
    const [descriptionTouched, setDescriptionTouched] = useState(false);
    const [touchedEntryIds, setTouchedEntryIds] = useState<ReadonlySet<number>>(new Set());

    const values: A2aIntegrationFormValues = { name, description, wellKnownUrls: entries.map(entry => entry.value) };
    const errors = validateA2aIntegrationForm(values);
    const canSubmit = isA2aIntegrationFormValid(errors) && !isSubmitting;

    function touchEntry(id: number) {
        setTouchedEntryIds(current => new Set(current).add(id));
    }

    function addEntry() {
        const id = nextEntryId.current++;
        setEntries(current => [...current, { id, value: '' }]);
    }

    function updateEntry(id: number, value: string) {
        touchEntry(id);
        setEntries(current => current.map(entry => (entry.id === id ? { ...entry, value } : entry)));
    }

    function removeEntry(id: number) {
        setEntries(current => current.filter(entry => entry.id !== id));
    }

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canSubmit) return;

        onSubmit(values);
    }

    return (
        <form className="space-y-4" onSubmit={handleSubmit}>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="a2a-integration-name" required>
                    Name
                </FieldLabel>
                <Input
                    id="a2a-integration-name"
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
                <FieldLabel htmlFor="a2a-integration-description">Description</FieldLabel>
                <Textarea
                    id="a2a-integration-description"
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
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel required>Well-known URLs</FieldLabel>
                <div className="space-y-2">
                    {entries.map((entry, index) => {
                        const entryError = touchedEntryIds.has(entry.id) ? errors.wellKnownUrls[index] : undefined;
                        return (
                            <div key={entry.id} className="flex items-start gap-2">
                                <div className="flex-1 space-y-1.5">
                                    <Input
                                        aria-label={`Well-known URL ${index + 1}`}
                                        value={entry.value}
                                        placeholder="https://agent.example.com/.well-known/agent-card.json"
                                        onChange={event => updateEntry(entry.id, event.target.value)}
                                        onBlur={() => touchEntry(entry.id)}
                                        aria-invalid={Boolean(entryError)}
                                    />
                                    {entryError ? <FieldError>{entryError}</FieldError> : null}
                                </div>
                                {entries.length > 1 ? (
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        aria-label={`Remove well-known URL ${index + 1}`}
                                        onClick={() => removeEntry(entry.id)}
                                    >
                                        <Trash2Icon className="size-4" aria-hidden />
                                    </Button>
                                ) : null}
                            </div>
                        );
                    })}
                </div>
                <Button type="button" variant="outline" size="sm" onClick={addEntry}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add another URL
                </Button>
                {(nameTouched || descriptionTouched) && errors.missingWellKnownUrls ? (
                    <FieldError>{errors.missingWellKnownUrls}</FieldError>
                ) : null}
            </Field>
            <Button type="submit" disabled={!canSubmit}>
                Create
            </Button>
        </form>
    );
}
