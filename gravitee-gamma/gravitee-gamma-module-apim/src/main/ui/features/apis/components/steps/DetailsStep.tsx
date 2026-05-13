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
import { Input, Label, Textarea } from '@gravitee/graphene-core';
import type { CSSProperties } from 'react';

import { useApiCreation } from '../../store/apiCreationStore';

export function DetailsStep() {
    const { state, dispatch } = useApiCreation();
    const { form, validationErrors: errors } = state;

    function update(patch: Partial<typeof form>) {
        dispatch({ type: 'UPDATE_FORM', patch });
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">API Details</h2>
                <p className="text-sm text-muted-foreground">Name and describe your API proxy.</p>
            </div>

            <div className="space-y-5">
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                        <Label htmlFor="details-api-name">
                            API Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="details-api-name"
                            placeholder="e.g. Payment Service API"
                            value={form.apiName}
                            onChange={e => update({ apiName: e.target.value })}
                            aria-invalid={Boolean(errors['apiName'])}
                        />
                        {errors['apiName'] && <p className="text-xs text-destructive">{errors['apiName']}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="details-api-version">
                            Version <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="details-api-version"
                            placeholder="e.g. 1.0.0"
                            value={form.apiVersion}
                            onChange={e => update({ apiVersion: e.target.value })}
                            aria-invalid={Boolean(errors['apiVersion'])}
                        />
                        {errors['apiVersion'] && <p className="text-xs text-destructive">{errors['apiVersion']}</p>}
                    </div>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="details-description">Description</Label>
                    <Textarea
                        id="details-description"
                        placeholder="Describe what this API does and who should use it."
                        value={form.apiDescription}
                        onChange={e => update({ apiDescription: e.target.value })}
                        maxLength={250}
                        rows={3}
                        style={{ fieldSizing: 'fixed' } as unknown as CSSProperties}
                    />
                    <p className="text-xs text-muted-foreground text-right">{form.apiDescription.length}/250</p>
                </div>
            </div>
        </div>
    );
}
