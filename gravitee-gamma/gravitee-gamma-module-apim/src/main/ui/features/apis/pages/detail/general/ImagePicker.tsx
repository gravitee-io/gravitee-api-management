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
import { UploadIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useRef, useState } from 'react';

const MAX_SIZE_BYTES = 500 * 1024;

function fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

export function ImagePicker({
    label,
    preview,
    width,
    height,
    onSelect,
    onRemove,
    disabled,
}: Readonly<{
    label: string;
    preview?: string;
    width: number;
    height: number;
    onSelect: (base64: string) => void;
    onRemove: () => void;
    disabled?: boolean;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [sizeError, setSizeError] = useState<string | null>(null);
    const [imgError, setImgError] = useState(false);

    // Reset error state when preview URL changes (e.g. after upload invalidates the query)
    useEffect(() => {
        setImgError(false);
    }, [preview]);

    const handleFile = useCallback(
        async (file: File) => {
            if (file.size > MAX_SIZE_BYTES) {
                setSizeError('File exceeds 500 KB limit.');
                return;
            }
            setSizeError(null);
            const base64 = await fileToBase64(file);
            onSelect(base64);
        },
        [onSelect],
    );

    return (
        <div className="space-y-1 text-center">
            <div
                role="button"
                tabIndex={0}
                onClick={() => !disabled && inputRef.current?.click()}
                onKeyDown={e => !disabled && (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
                className={cn(
                    'flex items-center justify-center rounded-xl border-dashed border-border transition-colors overflow-hidden',
                    disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:border-primary/50',
                )}
                style={{
                    width,
                    height,
                    borderWidth: '2px',
                    backgroundColor: 'color-mix(in oklab, var(--color-muted) 40%, transparent)',
                }}
                aria-label={`Upload ${label}`}
            >
                {preview && !imgError ? (
                    <img src={preview} alt={label} className="w-full h-full object-cover" onError={() => setImgError(true)} />
                ) : (
                    <UploadIcon className="size-6 text-muted-foreground" />
                )}
            </div>
            <p className="text-muted-foreground" style={{ fontSize: '10px' }}>
                {label}
            </p>
            {sizeError && <p className="text-xs text-destructive">{sizeError}</p>}
            {preview && !imgError && !disabled && (
                <button
                    type="button"
                    onClick={onRemove}
                    className="text-destructive hover:underline block mx-auto"
                    style={{ fontSize: '10px' }}
                >
                    Remove
                </button>
            )}
            <input
                ref={inputRef}
                type="file"
                accept="image/png,image/jpeg,image/svg+xml"
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
