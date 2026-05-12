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
import { Field, FieldContent, FieldDescription, FieldError, FieldLabel, Input, Textarea } from '@gravitee/graphene-core';
import { FileTextIcon } from '@gravitee/graphene-core/icons';
import type React from 'react';

import type { ApiCreationState } from '../../types/models';

type ApiDetailsStepProps = Readonly<{
    details: ApiCreationState['details'];
    errors: Record<string, string>;
    updateField: (path: string, value: unknown) => void;
}>;

const DESCRIPTION_MAX = 250;

export function ApiDetailsStep({ details, errors, updateField }: ApiDetailsStepProps) {
    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <div className="flex items-center gap-2">
                    <FileTextIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                    <h2 className="text-base font-semibold">API Details</h2>
                </div>
                <p className="text-sm text-muted-foreground">
                    Required fields are marked with <span className="text-destructive">*</span>.
                </p>
            </div>
            {/* API Name + Version — same row */}
            <div className="grid grid-cols-2 gap-4">
                <Field orientation="vertical">
                    <FieldLabel htmlFor="api-name">
                        API Name <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldContent>
                        <Input
                            id="api-name"
                            value={details.name}
                            aria-invalid={Boolean(errors['details.name'])}
                            placeholder="e.g. Payments API"
                            onChange={e => updateField('details.name', e.target.value)}
                        />
                    </FieldContent>
                    <FieldDescription>Public name shown to consumers in the Developer Portal.</FieldDescription>
                    <FieldError errors={errors['details.name'] ? [{ message: errors['details.name'] }] : undefined} />
                </Field>

                <Field orientation="vertical">
                    <FieldLabel htmlFor="api-version">
                        Version <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldContent>
                        <Input
                            id="api-version"
                            value={details.version}
                            aria-invalid={Boolean(errors['details.version'])}
                            placeholder="1.0.0"
                            onChange={e => updateField('details.version', e.target.value)}
                        />
                    </FieldContent>
                    <FieldDescription>For example 1.1, 1.1.1.</FieldDescription>
                    <FieldError errors={errors['details.version'] ? [{ message: errors['details.version'] }] : undefined} />
                </Field>
            </div>

            {/* Description — full width */}
            <Field orientation="vertical">
                <FieldLabel htmlFor="api-description">Description</FieldLabel>
                <FieldContent>
                    <Textarea
                        id="api-description"
                        className="w-full resize-none"
                        style={{ fieldSizing: 'fixed' } as unknown as React.CSSProperties}
                        value={details.description}
                        aria-invalid={Boolean(errors['details.description'])}
                        placeholder="Describe how your API works and what it does."
                        maxLength={DESCRIPTION_MAX}
                        rows={4}
                        onChange={e => updateField('details.description', e.target.value)}
                    />
                </FieldContent>
                <div className="flex items-center justify-between">
                    <FieldDescription>Helps consumers understand what your API offers.</FieldDescription>
                    <span className="text-xs text-muted-foreground tabular-nums">
                        {details.description.length}/{DESCRIPTION_MAX}
                    </span>
                </div>
                <FieldError errors={errors['details.description'] ? [{ message: errors['details.description'] }] : undefined} />
            </Field>
        </div>
    );
}
