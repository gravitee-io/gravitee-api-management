/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { cn } from '@gravitee/graphene-core';
import { useCallback, useRef, useState } from 'react';

const MAX_SIZE_BYTES = 1024 * 1024;

function fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '');
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function initialsFromName(name: string): string {
    return (
        name
            .split(/\s+/)
            .map(part => part[0] ?? '')
            .join('')
            .toUpperCase()
            .slice(0, 2) || '?'
    );
}

export function AvatarPicker({
    preview,
    fallbackName,
    onSelect,
    onUseDefault,
}: Readonly<{
    preview?: string;
    fallbackName: string;
    onSelect: (dataUrl: string) => void;
    onUseDefault: () => void;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [fileError, setFileError] = useState<string | null>(null);
    const [failedPreview, setFailedPreview] = useState<string | undefined>(undefined);
    const imageFailed = preview !== undefined && failedPreview === preview;
    const showingCustom = Boolean(preview) && !imageFailed;
    const caption = showingCustom ? 'Change picture' : 'Upload picture';

    const handleFile = useCallback(
        async (file: File) => {
            if (file.size > MAX_SIZE_BYTES) {
                setFileError('Image exceeds the maximum authorized size (1MB)');
                return;
            }
            if (!file.type.startsWith('image/')) {
                setFileError('Please choose an image file.');
                return;
            }
            try {
                const dataUrl = await fileToBase64(file);
                if (!dataUrl.startsWith('data:image/')) {
                    setFileError('Please choose an image file.');
                    return;
                }
                setFileError(null);
                onSelect(dataUrl);
            } catch {
                setFileError('Could not read that image. Try another file.');
            }
        },
        [onSelect],
    );

    return (
        <div className="space-y-2 text-center">
            <button
                type="button"
                onClick={() => inputRef.current?.click()}
                className={cn(
                    'group relative flex size-24 items-center justify-center overflow-hidden rounded-full border-2 border-dashed border-border bg-muted transition-colors',
                    'cursor-pointer hover:border-primary/50',
                )}
                aria-label={caption}
            >
                {showingCustom ? (
                    <img
                        key={preview}
                        src={preview}
                        alt="Avatar"
                        className="size-full object-cover transition-opacity group-hover:opacity-10 group-focus-visible:opacity-10"
                        onError={() => setFailedPreview(preview)}
                    />
                ) : (
                    <span className="relative z-0 flex size-full items-center justify-center rounded-full bg-primary/10 text-lg font-semibold text-primary">
                        {initialsFromName(fallbackName)}
                    </span>
                )}
                <span className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center bg-background px-2 text-center text-xs leading-tight font-medium text-foreground opacity-0 transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100">
                    {caption}
                </span>
            </button>
            {fileError ? <p className="text-xs text-destructive">{fileError}</p> : null}
            {showingCustom ? (
                <button
                    type="button"
                    className="text-sm text-destructive hover:underline"
                    onClick={event => {
                        event.stopPropagation();
                        onUseDefault();
                    }}
                >
                    Use default
                </button>
            ) : null}
            <input
                ref={inputRef}
                type="file"
                accept="image/*"
                aria-label="Choose avatar image"
                className="sr-only"
                onChange={event => {
                    const file = event.target.files?.[0];
                    if (file) {
                        void handleFile(file);
                    }
                    event.target.value = '';
                }}
            />
        </div>
    );
}
