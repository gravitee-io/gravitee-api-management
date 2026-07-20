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
import { Button, Card, CardContent, Checkbox, Input, RadioGroup, RadioGroupItem } from '@gravitee/graphene-core';
import { GripVerticalIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useState, type DragEvent } from 'react';

import {
    FIELD_PALETTE,
    FIELD_TYPE_LABELS,
    type FormField,
    type FormFieldPatch,
    type FormFieldType,
} from '../types';

const FIELD_TYPE_MIME = 'application/x-gravitee-field-type';
const FIELD_INDEX_MIME = 'application/x-gravitee-field-index';

interface FormBuilderPanelProps {
    readonly fields: readonly FormField[];
    readonly onAddField: (type: FormFieldType, index?: number) => void;
    readonly onUpdateField: (fieldId: string, patch: FormFieldPatch) => void;
    readonly onRemoveField: (fieldId: string) => void;
    readonly onMoveField: (fromIndex: number, toIndex: number) => void;
}

export function FormBuilderPanel({
    fields,
    onAddField,
    onUpdateField,
    onRemoveField,
    onMoveField,
}: FormBuilderPanelProps) {
    const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

    const handlePaletteDragStart = (event: DragEvent, type: FormFieldType) => {
        event.dataTransfer.setData(FIELD_TYPE_MIME, type);
        event.dataTransfer.effectAllowed = 'copy';
    };

    const handleFieldDragStart = (event: DragEvent, index: number) => {
        event.dataTransfer.setData(FIELD_INDEX_MIME, String(index));
        event.dataTransfer.effectAllowed = 'move';
    };

    const handleCanvasDragOver = (event: DragEvent, index: number) => {
        event.preventDefault();
        setDragOverIndex(index);
    };

    const handleCanvasDrop = (event: DragEvent, index: number) => {
        event.preventDefault();
        setDragOverIndex(null);

        const type = event.dataTransfer.getData(FIELD_TYPE_MIME) as FormFieldType;
        if (type && FIELD_PALETTE.includes(type)) {
            onAddField(type, index);
            return;
        }

        const fromRaw = event.dataTransfer.getData(FIELD_INDEX_MIME);
        if (fromRaw === '') {
            return;
        }
        const fromIndex = Number(fromRaw);
        if (Number.isNaN(fromIndex) || fromIndex === index || fromIndex === index - 1) {
            return;
        }
        const targetIndex = fromIndex < index ? index - 1 : index;
        onMoveField(fromIndex, targetIndex);
    };

    const handleCanvasDragLeave = () => setDragOverIndex(null);

    return (
        <section aria-labelledby="form-builder-heading" className="space-y-3">
            <div className="space-y-0.5">
                <h2 id="form-builder-heading" className="text-base font-semibold tracking-tight">
                    Form builder
                </h2>
                <p className="text-xs text-muted-foreground">
                    Drag field types onto the builder. Preview updates live on the right.
                </p>
            </div>

            <div className="flex w-full flex-row items-stretch gap-4">
                <Card className="w-44 shrink-0">
                    <CardContent className="space-y-2 p-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Field types</p>
                        {FIELD_PALETTE.map(type => (
                            <div
                                key={type}
                                draggable
                                onDragStart={event => handlePaletteDragStart(event, type)}
                                className="cursor-grab rounded-lg border border-border/60 bg-muted/40 px-3 py-2 text-sm active:cursor-grabbing"
                            >
                                {FIELD_TYPE_LABELS[type]}
                            </div>
                        ))}
                    </CardContent>
                </Card>

                <Card
                    className="min-h-72 min-w-0 flex-1"
                    onDragOver={event => {
                        event.preventDefault();
                        if (fields.length === 0) {
                            setDragOverIndex(0);
                        }
                    }}
                    onDrop={event => handleCanvasDrop(event, fields.length)}
                    onDragLeave={handleCanvasDragLeave}
                >
                    <CardContent className="space-y-3 p-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Builder</p>
                        {fields.length === 0 ? (
                            <div
                                className={`flex min-h-56 flex-col items-center justify-center rounded-xl border border-dashed px-6 text-center ${
                                    dragOverIndex === 0 ? 'border-primary bg-primary/5' : 'border-border/70 bg-muted/20'
                                }`}
                                onDragOver={event => handleCanvasDragOver(event, 0)}
                                onDrop={event => handleCanvasDrop(event, 0)}
                            >
                                <p className="text-sm font-medium">Drop fields here</p>
                                <p className="mt-1 text-xs text-muted-foreground">
                                    Text box, text area, drop down, radio button, or checkbox
                                </p>
                            </div>
                        ) : (
                            <ul className="space-y-2">
                                {fields.map((field, index) => (
                                    <li key={field.id}>
                                        <div
                                            className={`mb-2 h-2 rounded ${dragOverIndex === index ? 'bg-primary/30' : 'bg-transparent'}`}
                                            onDragOver={event => handleCanvasDragOver(event, index)}
                                            onDrop={event => handleCanvasDrop(event, index)}
                                        />
                                        <FormFieldCard
                                            field={field}
                                            onDragStart={event => handleFieldDragStart(event, index)}
                                            onUpdate={patch => onUpdateField(field.id, patch)}
                                            onRemove={() => onRemoveField(field.id)}
                                        />
                                    </li>
                                ))}
                                <div
                                    className={`h-8 rounded border border-dashed ${
                                        dragOverIndex === fields.length
                                            ? 'border-primary bg-primary/10'
                                            : 'border-transparent'
                                    }`}
                                    onDragOver={event => handleCanvasDragOver(event, fields.length)}
                                    onDrop={event => handleCanvasDrop(event, fields.length)}
                                />
                            </ul>
                        )}
                    </CardContent>
                </Card>

                <Card className="min-h-72 min-w-0 flex-1" aria-labelledby="form-preview-heading">
                    <CardContent className="space-y-4 p-4">
                        <div className="space-y-0.5">
                            <p
                                id="form-preview-heading"
                                className="text-xs font-medium uppercase tracking-wide text-muted-foreground"
                            >
                                Preview
                            </p>
                            <p className="text-xs text-muted-foreground">
                                How the subscription form will appear to consumers.
                            </p>
                        </div>

                        {fields.length === 0 ? (
                            <div className="flex min-h-56 items-center justify-center rounded-xl border border-dashed border-border/70 bg-muted/20 px-6 text-center">
                                <p className="text-sm text-muted-foreground">
                                    Add fields in the builder to see a live preview.
                                </p>
                            </div>
                        ) : (
                            <div className="space-y-4 rounded-xl border border-border/60 bg-muted/20 p-4">
                                {fields.map(field => (
                                    <div key={field.id} className="space-y-2">
                                        {field.type !== 'checkbox' && <FieldPreviewLabel field={field} />}
                                        <FieldPreview field={field} />
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>
        </section>
    );
}

function fieldLabelText(field: FormField): string {
    return field.label || FIELD_TYPE_LABELS[field.type];
}

function RequiredMarker() {
    return (
        <span className="text-destructive" aria-hidden="true">
            {' '}
            *
        </span>
    );
}

function FieldPreviewLabel({ field }: { readonly field: FormField }) {
    return (
        <p className="text-sm font-medium">
            {fieldLabelText(field)}
            {field.required && <RequiredMarker />}
        </p>
    );
}

function parseOptions(raw: string): string[] {
    return raw
        .split(',')
        .map(option => option.trim())
        .filter(Boolean);
}

function FormFieldCard({
    field,
    onDragStart,
    onUpdate,
    onRemove,
}: {
    readonly field: FormField;
    readonly onDragStart: (event: DragEvent) => void;
    readonly onUpdate: (patch: FormFieldPatch) => void;
    readonly onRemove: () => void;
}) {
    const [optionsDraft, setOptionsDraft] = useState(() => field.options.join(', '));

    useEffect(() => {
        setOptionsDraft(field.options.join(', '));
    }, [field.id]);

    const handleOptionsChange = (raw: string) => {
        setOptionsDraft(raw);
        onUpdate({ options: parseOptions(raw) });
    };

    const showValidation = field.type === 'text' || field.type === 'textarea';
    const showExpression = field.type === 'dropdown' || field.type === 'radio';

    return (
        <div draggable onDragStart={onDragStart} className="rounded-xl border border-border/60 bg-card p-4 shadow-sm">
            <div className="flex items-start gap-2">
                <GripVerticalIcon className="mt-2 size-4 shrink-0 cursor-grab text-muted-foreground" aria-hidden />
                <div className="min-w-0 flex-1 space-y-2">
                    <div className="flex items-center justify-between gap-2">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                            {FIELD_TYPE_LABELS[field.type]}
                        </p>
                        <div className="flex items-center gap-2">
                            <label className="flex items-center gap-1.5 text-xs text-muted-foreground">
                                <Checkbox
                                    checked={field.required}
                                    onCheckedChange={checked => onUpdate({ required: checked === true })}
                                />
                                Required
                            </label>
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={onRemove}
                                aria-label={`Remove ${field.label}`}
                            >
                                <Trash2Icon className="size-4" aria-hidden />
                            </Button>
                        </div>
                    </div>
                    <Input
                        value={field.label}
                        onChange={event => onUpdate({ label: event.target.value })}
                        aria-label="Field label"
                        placeholder="Field label"
                    />
                    {showValidation && (
                        <Input
                            value={field.validation}
                            onChange={event => onUpdate({ validation: event.target.value })}
                            aria-label="Validation regex"
                            placeholder="Validation (regex), e.g. ^[A-Za-z0-9]+$"
                        />
                    )}
                    {showExpression && (
                        <>
                            <Input
                                value={field.expression}
                                onChange={event => onUpdate({ expression: event.target.value })}
                                aria-label="EL expression"
                                placeholder="{#api.metadata['key']}"
                            />
                            <Input
                                value={optionsDraft}
                                onChange={event => handleOptionsChange(event.target.value)}
                                aria-label="Field options"
                                placeholder="Fallback options, comma-separated"
                            />
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

function FieldPreview({ field }: { readonly field: FormField }) {
    switch (field.type) {
        case 'text':
            return (
                <Input
                    placeholder={fieldLabelText(field)}
                    required={field.required}
                    pattern={field.validation.trim() || undefined}
                    title={field.validation.trim() ? `Must match: ${field.validation}` : undefined}
                />
            );
        case 'textarea':
            return (
                <textarea
                    rows={4}
                    placeholder={fieldLabelText(field)}
                    aria-label={fieldLabelText(field)}
                    required={field.required}
                    title={field.validation.trim() ? `Must match: ${field.validation}` : undefined}
                    className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
            );
        case 'dropdown': {
            const options = field.options.length > 0 ? field.options : [];
            return (
                <select
                    key={options.join('|')}
                    className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                    aria-label={fieldLabelText(field)}
                    required={field.required}
                    defaultValue=""
                >
                    <option value="" disabled>
                        Select an option
                    </option>
                    {options.map(option => (
                        <option key={option} value={option}>
                            {option}
                        </option>
                    ))}
                </select>
            );
        }
        case 'radio':
            return (
                <RadioGroup className="gap-2" aria-required={field.required || undefined}>
                    {(field.options.length > 0 ? field.options : ['Option']).map(option => (
                        <label key={option} className="flex items-center gap-2 text-sm">
                            <RadioGroupItem value={option} required={field.required} />
                            {option}
                        </label>
                    ))}
                </RadioGroup>
            );
        case 'checkbox':
            return (
                <label className="flex items-center gap-2 text-sm">
                    <Checkbox required={field.required} />
                    {fieldLabelText(field)}
                    {field.required && <RequiredMarker />}
                </label>
            );
    }
}
