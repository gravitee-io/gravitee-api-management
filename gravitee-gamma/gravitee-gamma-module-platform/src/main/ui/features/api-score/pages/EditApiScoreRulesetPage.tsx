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
    CardDescription,
    CardHeader,
    CardTitle,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    Skeleton,
    Textarea,
} from '@gravitee/graphene-core';
import { CheckIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify';
import { ApiScoreGoBack } from '../components/ApiScoreGoBack';
import { RulesetPayloadPreview } from '../components/RulesetPayloadPreview';
import { useDeleteScoringRuleset, useScoringRuleset, useUpdateScoringRuleset } from '../hooks/useScoringRulesets';
import type { ScoringRuleset } from '../types/rulesets';
import { isValidRulesetName, RULESET_DESCRIPTION_MAX, rulesetNameError } from '../utils/rulesetFormat';

function fieldsFromRuleset(ruleset: ScoringRuleset) {
    return { name: ruleset.name, description: ruleset.description ?? '' };
}

export function EditApiScoreRulesetPage() {
    const { rulesetId } = useParams<{ rulesetId: string }>();
    const navigate = useNavigate();
    const rulesetQuery = useScoringRuleset(rulesetId);
    const updateRuleset = useUpdateScoringRuleset(rulesetId ?? '');
    const deleteRuleset = useDeleteScoringRuleset();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [saved, setSaved] = useState({ name: '', description: '' });
    const [nameTouched, setNameTouched] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const hydratedId = useRef<string | undefined>(undefined);

    useEffect(() => {
        if (!rulesetQuery.isError) return;
        notify.error(rulesetQuery.error, 'Ruleset error');
    }, [rulesetQuery.error, rulesetQuery.isError]);

    useEffect(() => {
        const ruleset = rulesetQuery.data;
        if (!ruleset || !rulesetId) return;
        if (hydratedId.current === rulesetId) return;
        hydratedId.current = rulesetId;
        const fields = fieldsFromRuleset(ruleset);
        setName(fields.name);
        setDescription(fields.description);
        setSaved(fields);
        setNameTouched(false);
    }, [rulesetId, rulesetQuery.data]);

    const nameError = rulesetNameError(name, 'Name');
    const isDirty = name !== saved.name || description !== saved.description;
    const isValid = isValidRulesetName(name) && description.length <= RULESET_DESCRIPTION_MAX;
    const showSaveBar = isDirty;

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!isDirty || !isValid || updateRuleset.isPending) return;
        const trimmedName = name.trim();
        try {
            await updateRuleset.mutateAsync({ name: trimmedName, description });
            setName(trimmedName);
            setSaved({ name: trimmedName, description });
            notify.success('Ruleset updated.');
        } catch (error) {
            notify.error(error, 'Ruleset update error!');
        }
    }

    async function handleDiscard() {
        const result = await rulesetQuery.refetch();
        if (result.data) {
            hydratedId.current = rulesetId;
            const fields = fieldsFromRuleset(result.data);
            setName(fields.name);
            setDescription(fields.description);
            setSaved(fields);
            setNameTouched(false);
        }
    }

    async function confirmDelete() {
        if (!rulesetId) return;
        try {
            await deleteRuleset.mutateAsync(rulesetId);
            notify.success('Ruleset successfully deleted!');
            navigate('../..', { relative: 'path' });
        } catch (error) {
            notify.error(error, 'Delete ruleset error!');
        }
    }

    return (
        <div className="space-y-6" data-testid="edit-api-score-ruleset-page">
            <ApiScoreGoBack to="../.." />
            {rulesetQuery.isLoading ? <Skeleton className="h-64 w-full rounded-lg" /> : null}
            {!rulesetQuery.isLoading && rulesetQuery.data ? (
                <form className="space-y-6" onSubmit={event => void handleSubmit(event)}>
                    <Card>
                        <CardHeader>
                            <CardTitle>Edit Ruleset</CardTitle>
                            <CardDescription>
                                Rulesets let you define rules that governs how your APIs behaves and interacts.
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            <div className="space-y-4">
                                <h3 className="text-base font-semibold">Ruleset Information</h3>
                                <p className="text-muted-foreground text-sm">
                                    To update your ruleset, delete the current one and upload the new version.
                                </p>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="ruleset-name" required>
                                        Name
                                    </FieldLabel>
                                    <Input
                                        id="ruleset-name"
                                        data-testid="name-input"
                                        value={name}
                                        onChange={event => {
                                            setNameTouched(true);
                                            setName(event.target.value);
                                        }}
                                        onBlur={() => setNameTouched(true)}
                                        aria-invalid={nameTouched && Boolean(nameError)}
                                    />
                                    <FieldDescription>
                                        Use this custom name to organize and identify specific ruleset more easily.
                                    </FieldDescription>
                                    {nameTouched && nameError ? <FieldError>{nameError}</FieldError> : null}
                                </Field>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="ruleset-description">Description</FieldLabel>
                                    <Textarea
                                        id="ruleset-description"
                                        data-testid="description"
                                        value={description}
                                        maxLength={RULESET_DESCRIPTION_MAX}
                                        rows={3}
                                        onChange={event => setDescription(event.target.value)}
                                    />
                                    <FieldDescription>
                                        {description.length}/{RULESET_DESCRIPTION_MAX}
                                    </FieldDescription>
                                </Field>
                            </div>
                            <div className="space-y-3">
                                <h3 className="text-base font-semibold">Raw file preview</h3>
                                <RulesetPayloadPreview payload={rulesetQuery.data.payload} />
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="border-destructive/40">
                        <CardHeader>
                            <CardTitle>Danger Zone</CardTitle>
                        </CardHeader>
                        <CardContent className="flex flex-wrap items-center justify-between gap-3">
                            <span className="text-sm">Delete this ruleset</span>
                            <Button
                                type="button"
                                variant="destructive"
                                size="sm"
                                data-testid="delete-ruleset-button"
                                onClick={() => setDeleteOpen(true)}
                            >
                                Delete ruleset
                            </Button>
                        </CardContent>
                    </Card>

                    {showSaveBar ? (
                        <div className="bg-background sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 border-t py-4">
                            <p className="text-muted-foreground mr-auto text-sm">You have unsaved changes.</p>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => void handleDiscard()}
                                disabled={updateRuleset.isPending}
                            >
                                Discard
                            </Button>
                            <Button type="submit" size="sm" disabled={!isValid || updateRuleset.isPending}>
                                <CheckIcon className="size-4" aria-hidden />
                                {updateRuleset.isPending ? 'Saving…' : 'Save'}
                            </Button>
                        </div>
                    ) : null}
                </form>
            ) : null}

            <ConfirmDialog
                open={deleteOpen}
                onOpenChange={setDeleteOpen}
                title="Delete this ruleset"
                description="Please note that once your ruleset is deleted, it cannot be restored."
                confirmLabel="Delete ruleset"
                destructive
                isPending={deleteRuleset.isPending}
                onConfirm={() => void confirmDelete()}
            />
        </div>
    );
}
