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
import { Alert, AlertDescription, FileUpload, type FileRejection } from '@gravitee/graphene-core';
import { useState } from 'react';

import type { PortalPageContentType } from '../../../types/apiDocumentation';
import { DOCUMENTATION_FILE_ACCEPT, detectPageContentType, MAX_DOCUMENTATION_FILE_SIZE_MB } from '../../../utils/documentationFile';
import { PAGE_CONTENT_TYPE_LABELS } from '../../../utils/pageContentType';

export interface ImportedFile {
    file: File;
    content: string;
    contentType: PortalPageContentType;
}

/**
 * Reads the chosen file and tells its page type, or says why it cannot be imported. The file is held by the caller, so
 * it survives this field being unmounted.
 */
export function ImportFileField({
    value,
    onChange,
    disabled,
}: Readonly<{ value: ImportedFile | null; onChange: (file: ImportedFile | null) => void; disabled: boolean }>) {
    const [error, setError] = useState<string | null>(null);

    function refuse(message: string) {
        onChange(null);
        setError(message);
    }

    async function handleFilesAdd([file]: File[]) {
        if (!file) return;
        let content: string;
        try {
            content = await file.text();
        } catch {
            refuse(`'${file.name}' could not be read.`);
            return;
        }
        const contentType = detectPageContentType(file.name, content);
        if (!contentType) {
            refuse(`Cannot tell whether '${file.name}' is OpenAPI or AsyncAPI: it needs a root openapi, swagger or asyncapi property.`);
            return;
        }
        setError(null);
        onChange({ file, content, contentType });
    }

    function handleFilesReject([rejection]: FileRejection[]) {
        if (!rejection) return;
        const tooLarge = rejection.errors.some(rejectionError => rejectionError.code === 'file-too-large');
        refuse(
            tooLarge
                ? `'${rejection.file.name}' is larger than ${MAX_DOCUMENTATION_FILE_SIZE_MB} MB.`
                : 'Only .md, .yaml, .yml and .json files can be imported.',
        );
    }

    return (
        <>
            <FileUpload
                label="Choose a file to import"
                hint={`Markdown, OpenAPI or AsyncAPI: .md, .yaml, .yml or .json, up to ${MAX_DOCUMENTATION_FILE_SIZE_MB} MB.`}
                accept={DOCUMENTATION_FILE_ACCEPT}
                maxFileSize={MAX_DOCUMENTATION_FILE_SIZE_MB * 1024 * 1024}
                maxFiles={1}
                disabled={disabled}
                items={value ? [{ id: value.file.name, file: value.file, status: 'success' }] : []}
                errors={error ? [error] : []}
                onFilesAdd={files => void handleFilesAdd(files)}
                onFilesReject={handleFilesReject}
                onFileRemove={() => onChange(null)}
            />
            {value ? (
                <Alert>
                    <AlertDescription>
                        {value.file.name} will be imported as {PAGE_CONTENT_TYPE_LABELS[value.contentType]}.
                    </AlertDescription>
                </Alert>
            ) : null}
        </>
    );
}
