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
import { useCallback, useRef, useState } from 'react';

const MAX_SIZE_BYTES = 500 * 1024;

function fileToDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function ImageSlot({
    label,
    preview,
    width,
    height,
    disabled,
    onSelect,
    onClear,
}: Readonly<{
    label: string;
    preview: string | null;
    width: number;
    height: number;
    disabled?: boolean;
    onSelect: (dataUrl: string) => void;
    onClear: () => void;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [sizeError, setSizeError] = useState<string | null>(null);

    const handleFile = useCallback(
        async (file: File) => {
            if (file.size > MAX_SIZE_BYTES) {
                setSizeError('File exceeds 500 KB limit.');
                return;
            }
            setSizeError(null);
            onSelect(await fileToDataUrl(file));
        },
        [onSelect],
    );

    return (
        <div className="space-y-1.5 text-center">
            <button
                type="button"
                disabled={disabled}
                onClick={() => !disabled && inputRef.current?.click()}
                onContextMenu={e => {
                    if (!disabled && preview) {
                        e.preventDefault();
                        onClear();
                    }
                }}
                className={cn(
                    'flex items-center justify-center overflow-hidden rounded-xl border-2 border-dashed border-border bg-muted/30 transition-colors',
                    disabled ? 'cursor-not-allowed opacity-50 pointer-events-none' : 'cursor-pointer hover:border-primary',
                )}
                style={{ width, height }}
                aria-label={`Upload ${label}`}
                aria-disabled={disabled}
            >
                {preview ? (
                    <img src={preview} alt="" className="size-full object-cover" />
                ) : (
                    <UploadIcon className="size-6 text-muted-foreground/40" aria-hidden />
                )}
            </button>
            <p className="text-xs text-muted-foreground">{label}</p>
            {sizeError ? <p className="text-xs text-destructive">{sizeError}</p> : null}
            <input
                ref={inputRef}
                type="file"
                accept="image/png,image/jpeg,image/svg+xml"
                className="sr-only"
                disabled={disabled}
                tabIndex={-1}
                onChange={e => {
                    const file = e.target.files?.[0];
                    e.target.value = '';
                    if (file) void handleFile(file);
                }}
            />
        </div>
    );
}

export function ApplicationImagePickers({
    picture,
    background,
    disabled,
    onPictureChange,
    onBackgroundChange,
}: Readonly<{
    picture: string | null;
    background: string | null;
    disabled?: boolean;
    onPictureChange: (value: string | null) => void;
    onBackgroundChange: (value: string | null) => void;
}>) {
    return (
        <div className="space-y-3">
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">IMAGES</p>
            <div className="flex items-start gap-3">
                <ImageSlot
                    label="Picture"
                    preview={picture}
                    width={72}
                    height={72}
                    disabled={disabled}
                    onSelect={onPictureChange}
                    onClear={() => onPictureChange(null)}
                />
                <ImageSlot
                    label="Background"
                    preview={background}
                    width={112}
                    height={72}
                    disabled={disabled}
                    onSelect={onBackgroundChange}
                    onClear={() => onBackgroundChange(null)}
                />
            </div>
            <p className="text-xs leading-snug text-muted-foreground">Click to upload. PNG, JPG, SVG. Max 500 KB.</p>
        </div>
    );
}
