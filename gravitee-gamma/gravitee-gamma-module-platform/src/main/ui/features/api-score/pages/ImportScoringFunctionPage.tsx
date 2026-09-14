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
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify';
import { ApiScoreGoBack } from '../components/ApiScoreGoBack';
import { FileDropzone, FUNCTION_FILE_ACCEPT, type PickedScoringFile } from '../components/FileDropzone';
import { useCreateScoringFunction, useScoringFunctions } from '../hooks/useScoringFunctions';
import { functionFileNameErrors, isValidFunctionFileName } from '../utils/rulesetFormat';

export function ImportScoringFunctionPage() {
    const navigate = useNavigate();
    const functionsQuery = useScoringFunctions();
    const createFunction = useCreateScoringFunction();
    const [name, setName] = useState('');
    const [payload, setPayload] = useState('');
    const [overwriteOpen, setOverwriteOpen] = useState(false);

    useEffect(() => {
        if (!functionsQuery.isError) return;
        notify.error(functionsQuery.error, 'Functions error!');
    }, [functionsQuery.error, functionsQuery.isError]);

    const canImport = isValidFunctionFileName(name) && Boolean(payload) && !createFunction.isPending && !functionsQuery.isLoading;
    const existingNames = functionsQuery.functions.map(fn => fn.name);

    function applyPickedFile(picked: PickedScoringFile) {
        if (!picked.content) {
            setName('');
            setPayload('');
            return;
        }
        const errors = functionFileNameErrors(picked.name);
        errors.forEach(message => notify.error(new Error(message)));
        if (errors.length > 0) {
            setName('');
            setPayload('');
            return;
        }
        setName(picked.name);
        setPayload(picked.content);
    }

    function handleEmptyFile() {
        setName('');
        setPayload('');
        notify.error(new Error('The file can not be empty'));
    }

    async function submitFunction(successMessage: string, errorFallback: string) {
        try {
            await createFunction.mutateAsync({ name, payload });
            notify.success(successMessage);
            navigate('..', { relative: 'path' });
        } catch (error) {
            notify.error(error, errorFallback);
        }
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canImport) return;
        if (existingNames.includes(name)) {
            setOverwriteOpen(true);
            return;
        }
        await submitFunction('Function imported.', 'Function import error!');
    }

    async function confirmOverwrite() {
        setOverwriteOpen(false);
        await submitFunction('Function overwritten successfully!', 'Function overwrite error!');
    }

    return (
        <div className="space-y-6" data-testid="import-scoring-function-page">
            <ApiScoreGoBack to=".." />
            <Card>
                <CardHeader>
                    <CardTitle>Import a Custom Function</CardTitle>
                    <CardDescription>
                        Custom functions let you define specific logic or operations that extend the rulesets.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form className="space-y-8" onSubmit={event => void handleSubmit(event)}>
                        <FileDropzone
                            accept={FUNCTION_FILE_ACCEPT}
                            formatsHint="js"
                            onFile={applyPickedFile}
                            onEmptyFile={handleEmptyFile}
                            onReadError={error => notify.error(error, 'Failed to read file')}
                        />
                        <div className="flex flex-wrap gap-2">
                            <Button type="submit" data-testid="import-button" disabled={!canImport}>
                                {createFunction.isPending ? 'Importing…' : 'Import'}
                            </Button>
                            <Button asChild type="button" variant="outline">
                                <Link to=".." relative="path">
                                    Cancel
                                </Link>
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
            <ConfirmDialog
                open={overwriteOpen}
                onOpenChange={setOverwriteOpen}
                title="Overwrite Function?"
                description="A function with the same name already exists. Overwriting it will replace the existing function and its contents. This action cannot be undone. Are you sure you want to proceed?"
                confirmLabel="Overwrite"
                onConfirm={() => void confirmOverwrite()}
            />
        </div>
    );
}
