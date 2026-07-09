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
import { Alert, AlertDescription, Button, Separator } from '@gravitee/graphene-core';
import { ArrowLeftIcon, Loader2Icon, RocketIcon } from '@gravitee/graphene-core/icons';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { ImportSourceOptionsFields } from './detail/general/ImportSourceOptionsFields';
import { notify } from '../../../shared/notify';
import { useCreateApiFromImport } from '../hooks/useCreateApiFromImport';
import { useImportSourceOptions } from '../hooks/useImportSourceOptions';
import type { ApiImportFormat } from '../types';

const FORMAT_LABELS: Record<ApiImportFormat, string> = {
    gravitee: 'Gravitee definition',
    openapi: 'OpenAPI specification',
    wsdl: 'WSDL',
};

export function ImportApiForm({ format }: Readonly<{ format: ApiImportFormat }>) {
    const navigate = useNavigate();
    const sourceOptions = useImportSourceOptions(format);
    const { mutate, isPending, error, isSuccess, data } = useCreateApiFromImport();

    useEffect(() => {
        if (isSuccess && data) {
            notify.success('API created');
            navigate(`../../${data.id}/overview`);
        }
    }, [isSuccess, data, navigate]);

    const canSubmit = !isPending && sourceOptions.canSubmit;
    const errorMessage = error
        ? error instanceof Error
            ? error.message
            : 'Failed to create the API. Please check your details and try again.'
        : null;

    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Import {FORMAT_LABELS[format]}</h1>
                <p className="text-sm text-muted-foreground">Provide the source and any options, then create the API.</p>
            </div>

            <div className="space-y-4 rounded-xl border bg-card p-6">
                <ImportSourceOptionsFields format={format} state={sourceOptions} />
            </div>

            {errorMessage && (
                <Alert variant="destructive">
                    <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
            )}

            <Separator />

            <div className="flex items-center justify-between">
                <Button variant="outline" onClick={() => navigate('..')} disabled={isPending}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Cancel
                </Button>
                <Button onClick={() => mutate(sourceOptions.buildSubmission())} disabled={!canSubmit}>
                    {isPending ? (
                        <Loader2Icon className="size-4 animate-spin" aria-hidden />
                    ) : (
                        <RocketIcon className="size-4" aria-hidden />
                    )}
                    Create API
                </Button>
            </div>
        </div>
    );
}
