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
import { FileUpload, type Accept, type FileUploadItemValue } from '@gravitee/graphene-core';
import { UploadIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

export interface PickedScoringFile {
    name: string;
    content: string;
}

function readUploadAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '');
        reader.onerror = () => reject(reader.error ?? new Error('Failed to read file'));
        reader.readAsText(file);
    });
}

export function FileDropzone({
    accept,
    formatsHint,
    onFile,
    onEmptyFile,
    onReadError,
}: Readonly<{
    accept: Accept;
    formatsHint: string;
    onFile: (file: PickedScoringFile) => void;
    onEmptyFile: () => void;
    onReadError?: (error: unknown) => void;
}>) {
    const [items, setItems] = useState<FileUploadItemValue[]>([]);

    async function handleAdd(files: File[]) {
        const file = files[0];
        if (!file) return;
        try {
            const content = await readUploadAsText(file);
            const item: FileUploadItemValue = { id: file.name, file, status: content ? 'success' : 'error' };
            setItems([item]);
            if (!content) {
                onEmptyFile();
                return;
            }
            onFile({ name: file.name, content });
        } catch (error) {
            setItems([{ id: file.name, file, status: 'error' }]);
            onReadError?.(error);
        }
    }

    return (
        <div data-testid="file-dropzone">
            <FileUpload
                accept={accept}
                multiple={false}
                maxFiles={1}
                items={items}
                icon={<UploadIcon className="size-8" aria-hidden />}
                label="Drag and drop a file"
                hint={`Supported file formats: ${formatsHint}`}
                onFilesAdd={handleAdd}
                onFileRemove={() => {
                    setItems([]);
                    onFile({ name: '', content: '' });
                }}
            />
        </div>
    );
}

export const RULESET_FILE_ACCEPT: Accept = {
    'application/json': ['.json'],
    'application/x-yaml': ['.yml', '.yaml'],
    'text/yaml': ['.yml', '.yaml'],
    'text/x-yaml': ['.yml', '.yaml'],
};

export const FUNCTION_FILE_ACCEPT: Accept = {
    'text/javascript': ['.js'],
    'application/javascript': ['.js'],
};
