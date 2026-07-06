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
import { cn } from '@gravitee/graphene-core';
import { FileUpIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

interface FileDropFieldProps {
    /** Allowed extensions without the dot, e.g. `['yml','yaml','json']`. */
    extensions: readonly string[];
    /** Called with the selected file's name and text content once read. */
    onFileRead: (fileName: string, content: string) => void;
    /** Name of the currently selected file, if any (controlled by the parent). */
    fileName?: string | null;
}

/** Click-or-drop file field that reads the picked file's text content. Reused for rulesets and functions. */
export function FileDropField({ extensions, onFileRead, fileName }: FileDropFieldProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [error, setError] = useState<string | null>(null);
    const [dragging, setDragging] = useState(false);

    const accept = extensions.map(ext => `.${ext}`).join(',');
    const extLabel = extensions.map(ext => ext.toUpperCase()).join(', ');

    const hasAllowedExtension = (name: string) => extensions.some(ext => name.toLowerCase().endsWith(`.${ext.toLowerCase()}`));

    const handleFile = async (file: File) => {
        if (!hasAllowedExtension(file.name)) {
            setError(`Unsupported file type. Allowed: ${extLabel}.`);
            return;
        }
        setError(null);
        onFileRead(file.name, await file.text());
    };

    return (
        <div className="space-y-2">
            <div
                role="button"
                tabIndex={0}
                onClick={() => inputRef.current?.click()}
                onKeyDown={e => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
                onDragOver={e => {
                    e.preventDefault();
                    setDragging(true);
                }}
                onDragLeave={() => setDragging(false)}
                onDrop={e => {
                    e.preventDefault();
                    setDragging(false);
                    const file = e.dataTransfer.files?.[0];
                    if (file) void handleFile(file);
                }}
                className={cn(
                    'flex cursor-pointer items-center justify-center rounded-lg border-2 border-dashed p-6 transition-colors',
                    dragging ? 'border-primary/60 bg-primary/5' : 'bg-muted/40 hover:border-primary/40',
                )}
            >
                <div className="text-center space-y-1">
                    <FileUpIcon className="size-7 text-muted-foreground mx-auto" aria-hidden />
                    <p className="text-sm font-medium">{fileName ?? 'Drop a file here or click to browse'}</p>
                    <p className="text-xs text-muted-foreground">Supported formats: {extLabel.toLowerCase()}</p>
                </div>
            </div>
            {error && <p className="text-xs text-destructive">{error}</p>}
            <input
                ref={inputRef}
                type="file"
                accept={accept}
                className="sr-only"
                onChange={e => {
                    const file = e.target.files?.[0];
                    if (file) void handleFile(file);
                    e.target.value = '';
                }}
            />
        </div>
    );
}
