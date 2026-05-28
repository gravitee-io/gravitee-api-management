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
import { cn, FileUploadInput } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState, type CSSProperties } from 'react';

const SLOT_SIZE_PX = 114;
const IMAGE_ACCEPT = { 'image/*': [] } as const;
/** Inline size: arbitrary Tailwind sizes may be absent from the federated CSS bundle. */
const SLOT_BORDER_RADIUS_PX = 6;
const SLOT_DIMENSIONS_STYLE: CSSProperties = {
    width: SLOT_SIZE_PX,
    height: SLOT_SIZE_PX,
    minWidth: SLOT_SIZE_PX,
    minHeight: SLOT_SIZE_PX,
    borderWidth: 2,
    borderStyle: 'dotted',
    borderColor: 'var(--color-foreground)',
    borderRadius: SLOT_BORDER_RADIUS_PX,
    backgroundColor: 'color-mix(in oklab, var(--color-muted) 40%, transparent)',
};
const SLOT_CLASS_NAME = 'relative box-border shrink-0 overflow-hidden';
const PREVIEW_BLUR_STYLE: CSSProperties = { filter: 'blur(6px)' };
const HOVER_OVERLAY_STYLE: CSSProperties = {
    backgroundColor: 'color-mix(in oklab, var(--color-background) 45%, transparent)',
};
const HOVER_OVERLAY_CLASS_NAME = 'pointer-events-none absolute inset-0 z-[12] flex items-center justify-center px-2';

function UploadHintText({ className }: Readonly<{ className?: string }>) {
    return (
        <span className={cn('text-center text-xs leading-[1.8] text-foreground', className)}>
            Click here or drag an image
            <br />
            Max 500KB
        </span>
    );
}

function ImageRemoveButton({
    label,
    onRemove,
}: Readonly<{
    label: string;
    onRemove: () => void;
}>) {
    return (
        <button
            type="button"
            className="absolute top-1 right-1 z-20 flex size-5 items-center justify-center rounded border border-border bg-background shadow-sm"
            aria-label={`Remove ${label}`}
            onClick={e => {
                e.stopPropagation();
                onRemove();
            }}
        >
            <XIcon className="size-3.5 text-foreground hover:text-destructive" aria-hidden />
        </button>
    );
}

function fileToDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function ImageFilePickerSlot({
    fieldLabel,
    variant,
    preview,
    disabled,
    onSelect,
    onRemove,
}: Readonly<{
    fieldLabel: string;
    variant: 'picture' | 'background';
    preview: string | null;
    disabled?: boolean;
    onSelect: (dataUrl: string) => void;
    onRemove: () => void;
}>) {
    const [imgError, setImgError] = useState(false);
    const [hovered, setHovered] = useState(false);
    const [prevPreview, setPrevPreview] = useState(preview);
    if (prevPreview !== preview) {
        setPrevPreview(preview);
        setImgError(false);
    }

    const handleFilesAccepted = useCallback(
        async (files: File[]) => {
            const file = files[0];
            if (!file) return;
            onSelect(await fileToDataUrl(file));
        },
        [onSelect],
    );

    const hasPreview = Boolean(preview && !imgError);
    const showHoverOverlay = hovered && !disabled && hasPreview;
    const uploadLabel = hasPreview ? `Replace ${fieldLabel}` : `Upload ${fieldLabel}`;

    return (
        <div className="flex shrink-0 flex-col" style={{ width: SLOT_SIZE_PX }}>
            <span className="mb-1 block text-xs text-foreground">{fieldLabel}</span>
            <div
                className={SLOT_CLASS_NAME}
                style={SLOT_DIMENSIONS_STYLE}
                onMouseEnter={() => setHovered(true)}
                onMouseLeave={() => setHovered(false)}
            >
                {!hasPreview ? (
                    <div className="pointer-events-none absolute inset-0 z-[1] flex items-center justify-center px-2">
                        <UploadHintText />
                    </div>
                ) : null}

                {hasPreview ? (
                    <>
                        <img
                            src={preview!}
                            alt=""
                            className="absolute inset-0 z-[1] size-full object-cover"
                            style={showHoverOverlay ? PREVIEW_BLUR_STYLE : undefined}
                            onError={() => setImgError(true)}
                        />
                        {!disabled ? <ImageRemoveButton label={fieldLabel} onRemove={onRemove} /> : null}
                    </>
                ) : null}

                {showHoverOverlay ? (
                    <div className={HOVER_OVERLAY_CLASS_NAME} style={HOVER_OVERLAY_STYLE}>
                        <UploadHintText className="relative z-[1]" />
                    </div>
                ) : null}

                <FileUploadInput
                    key={variant}
                    accept={IMAGE_ACCEPT}
                    className={cn(
                        'absolute inset-0 z-[15] size-full cursor-pointer rounded-none border-0 bg-transparent p-0 opacity-0 shadow-none',
                        disabled && 'pointer-events-none',
                    )}
                    disabled={disabled}
                    hint={null}
                    icon={<span aria-hidden className="sr-only" />}
                    label={<span className="sr-only">{uploadLabel}</span>}
                    maxFiles={1}
                    multiple={false}
                    onFilesAccepted={files => void handleFilesAccepted(files)}
                />
            </div>
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
        <div className="flex items-end gap-1.5">
            <ImageFilePickerSlot
                fieldLabel="Application picture"
                variant="picture"
                preview={picture}
                disabled={disabled}
                onSelect={onPictureChange}
                onRemove={() => onPictureChange(null)}
            />
            <ImageFilePickerSlot
                fieldLabel="Application background"
                variant="background"
                preview={background}
                disabled={disabled}
                onSelect={onBackgroundChange}
                onRemove={() => onBackgroundChange(null)}
            />
        </div>
    );
}
