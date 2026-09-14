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
import { UploadIcon } from '@gravitee/graphene-core/icons';
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

function AvatarPreview({ src }: { readonly src: string }) {
    const [failed, setFailed] = useState(false);
    if (failed) {
        return <UploadIcon className="size-6 text-muted-foreground" />;
    }
    return <img src={src} alt="Avatar" className="size-full object-cover" onError={() => setFailed(true)} />;
}

export function AvatarPicker({
    preview,
    onSelect,
    onUseDefault,
}: Readonly<{
    preview?: string;
    onSelect: (dataUrl: string) => void;
    onUseDefault: () => void;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [fileError, setFileError] = useState<string | null>(null);

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
            <div
                role="button"
                tabIndex={0}
                onClick={() => inputRef.current?.click()}
                onKeyDown={event => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        inputRef.current?.click();
                    }
                }}
                className={cn(
                    'flex size-24 items-center justify-center overflow-hidden rounded-full border-2 border-dashed border-border transition-colors',
                    'cursor-pointer hover:border-primary/50',
                )}
                aria-label="Upload avatar"
            >
                {preview ? <AvatarPreview key={preview} src={preview} /> : <UploadIcon className="size-6 text-muted-foreground" />}
            </div>
            {fileError ? <p className="text-xs text-destructive">{fileError}</p> : null}
            {preview ? (
                <button type="button" className="text-sm text-destructive hover:underline" onClick={onUseDefault}>
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
