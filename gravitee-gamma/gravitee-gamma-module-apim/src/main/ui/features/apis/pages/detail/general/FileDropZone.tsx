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
import { FileUpIcon } from '@gravitee/graphene-core/icons';
import { useRef } from 'react';
import type { DragEvent } from 'react';

/** OS file drops inside a sheet are delivered to a file input, not to a div. */
export function FileDropZone({
    accept,
    fileName,
    hint,
    onFile,
}: Readonly<{
    accept: string;
    fileName: string | null;
    hint: string;
    onFile: (file: File) => void | Promise<void>;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);

    const allowDrop = (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();
        event.dataTransfer.dropEffect = 'copy';
    };

    return (
        <div
            role="button"
            tabIndex={0}
            onClick={() => inputRef.current?.click()}
            onKeyDown={e => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
            onDragEnter={allowDrop}
            onDragOver={allowDrop}
            onDrop={event => {
                allowDrop(event);
                const file = event.dataTransfer.files?.[0];
                if (file) void onFile(file);
            }}
            className="relative flex items-center justify-center rounded-lg border-dashed bg-muted/40 p-6 cursor-pointer hover:border-primary/40 transition-colors"
            style={{ borderWidth: '2px' }}
        >
            <div className="text-center space-y-1 pointer-events-none">
                <FileUpIcon className="size-7 text-muted-foreground mx-auto" />
                <p className="text-sm font-medium">{fileName ?? 'Drop file here or click to browse'}</p>
                <p className="text-xs text-muted-foreground">{hint}</p>
            </div>
            <input
                ref={inputRef}
                type="file"
                accept={accept}
                aria-label="Import file"
                className="absolute inset-0 cursor-pointer opacity-0"
                onChange={async e => {
                    const file = e.target.files?.[0];
                    if (file) await onFile(file);
                    e.target.value = '';
                }}
            />
        </div>
    );
}
