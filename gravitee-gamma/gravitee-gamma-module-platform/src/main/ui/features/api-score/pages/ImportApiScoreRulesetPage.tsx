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
    Textarea,
} from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { notify } from '../../../shared/notify';
import { ApiScoreGoBack } from '../components/ApiScoreGoBack';
import { FileDropzone, RULESET_FILE_ACCEPT, type PickedScoringFile } from '../components/FileDropzone';
import { RulesetFormatCards } from '../components/RulesetFormatCards';
import { useCreateScoringRuleset } from '../hooks/useScoringRulesets';
import { GRAVITEE_API_DEFINITION, type DefinitionFormatSelection, type RulesetFormat } from '../types/rulesets';
import { isValidRulesetName, resolveImportedRulesetFormat, RULESET_DESCRIPTION_MAX, rulesetNameError } from '../utils/rulesetFormat';

export function ImportApiScoreRulesetPage() {
    const navigate = useNavigate();
    const createRuleset = useCreateScoringRuleset();
    const [definitionFormat, setDefinitionFormat] = useState<DefinitionFormatSelection | ''>('');
    const [graviteeApiFormat, setGraviteeApiFormat] = useState<RulesetFormat | ''>('');
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [file, setFile] = useState<PickedScoringFile | null>(null);
    const [nameTouched, setNameTouched] = useState(false);

    const format = resolveImportedRulesetFormat(definitionFormat, graviteeApiFormat);
    const nameError = rulesetNameError(name, 'Ruleset name');
    const canImport = Boolean(format && isValidRulesetName(name) && file?.content && !createRuleset.isPending);

    function handleDefinitionFormatChange(value: DefinitionFormatSelection) {
        setDefinitionFormat(value);
        if (value !== GRAVITEE_API_DEFINITION) {
            setGraviteeApiFormat('');
        }
    }

    function handleFile(picked: PickedScoringFile) {
        setFile(picked.content ? picked : null);
    }

    function handleEmptyFile() {
        setFile(null);
        notify.error(new Error('The file can not be empty'));
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canImport || !format || !file) return;

        try {
            await createRuleset.mutateAsync({
                format,
                name: name.trim(),
                description,
                payload: file.content,
            });
            notify.success('Ruleset imported.');
            navigate('..', { relative: 'path' });
        } catch (error) {
            notify.error(error, 'Ruleset import error!');
        }
    }

    return (
        <div className="space-y-6" data-testid="import-api-score-ruleset-page">
            <ApiScoreGoBack to=".." />
            <Card>
                <CardHeader>
                    <CardTitle>Import a Ruleset</CardTitle>
                    <CardDescription>
                        Custom rulesets allow you to enforce your organization&apos;s API design, quality and security standards.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form className="space-y-8" onSubmit={event => void handleSubmit(event)}>
                        <RulesetFormatCards
                            definitionFormat={definitionFormat}
                            graviteeApiFormat={graviteeApiFormat}
                            onDefinitionFormatChange={handleDefinitionFormatChange}
                            onGraviteeApiFormatChange={setGraviteeApiFormat}
                        />

                        <div className="space-y-4">
                            <h3 className="text-base font-semibold">Ruleset Information</h3>
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="ruleset-name" required>
                                    Set Your Custom Name
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
                                    rows={2}
                                    onChange={event => setDescription(event.target.value)}
                                />
                                <FieldDescription>
                                    {description.length}/{RULESET_DESCRIPTION_MAX}
                                </FieldDescription>
                            </Field>
                        </div>

                        <div className="space-y-3">
                            <h3 className="text-base font-semibold">File</h3>
                            <FileDropzone
                                accept={RULESET_FILE_ACCEPT}
                                formatsHint="yml, yaml, json"
                                onFile={handleFile}
                                onEmptyFile={handleEmptyFile}
                                onReadError={error => notify.error(error, 'Failed to read file')}
                            />
                        </div>

                        <div className="flex flex-wrap gap-2">
                            <Button type="submit" data-testid="import-button" disabled={!canImport}>
                                {createRuleset.isPending ? 'Importing…' : 'Import'}
                            </Button>
                            <Button asChild type="button" variant="outline" data-testid="cancel-button">
                                <Link to=".." relative="path">
                                    Cancel
                                </Link>
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
