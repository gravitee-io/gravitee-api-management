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
import type { FormField, FormFieldType } from '../types';
import { FIELD_PALETTE, FIELD_TYPE_LABELS, normalizeFormField } from '../types';

export const SUBSCRIPTION_FORM_BUILDER_KIND = 'gravitee-subscription-form-builder';
export const SUBSCRIPTION_FORM_BUILDER_VERSION = 1;

export interface FormFieldConfig {
    readonly type: FormFieldType;
    readonly label: string;
    readonly required: boolean;
    readonly options: readonly string[];
    readonly validation: string;
    readonly expression: string;
}

function createFieldId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `field-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export function toFormFieldConfigs(fields: readonly FormField[]): FormFieldConfig[] {
    return fields.map(field => ({
        type: field.type,
        label: field.label,
        required: field.required,
        options: [...field.options],
        validation: field.validation,
        expression: field.expression,
    }));
}

export function serializeBuilderConfig(fields: readonly FormField[]): string {
    const payload = {
        kind: SUBSCRIPTION_FORM_BUILDER_KIND,
        version: SUBSCRIPTION_FORM_BUILDER_VERSION,
        fields: toFormFieldConfigs(fields),
    };
    return `${JSON.stringify(payload, null, 2)}\n`;
}

function isFormFieldType(value: unknown): value is FormFieldType {
    return typeof value === 'string' && (FIELD_PALETTE as readonly string[]).includes(value);
}

function parseFieldConfig(value: unknown, index: number): FormField {
    if (!value || typeof value !== 'object') {
        throw new Error(`Field at index ${index} is invalid.`);
    }
    const raw = value as Record<string, unknown>;
    if (!isFormFieldType(raw.type)) {
        throw new Error(`Field at index ${index} has an unsupported type.`);
    }
    const label = typeof raw.label === 'string' && raw.label.trim() ? raw.label.trim() : FIELD_TYPE_LABELS[raw.type];
    const required = typeof raw.required === 'boolean' ? raw.required : false;
    const options = Array.isArray(raw.options)
        ? raw.options
              .filter((option): option is string => typeof option === 'string')
              .map(option => option.trim())
              .filter(Boolean)
        : [];
    const validation = typeof raw.validation === 'string' ? raw.validation : '';
    const expression = typeof raw.expression === 'string' ? raw.expression : '';

    return normalizeFormField({
        id: createFieldId(),
        type: raw.type,
        label,
        required,
        options,
        validation,
        expression,
    });
}

export function parseBuilderConfig(raw: string): FormField[] {
    let parsed: unknown;
    try {
        parsed = JSON.parse(raw);
    } catch {
        throw new Error('Invalid JSON file.');
    }

    if (!parsed || typeof parsed !== 'object') {
        throw new Error('Invalid subscription form configuration.');
    }

    const payload = parsed as Record<string, unknown>;
    if (payload.kind !== SUBSCRIPTION_FORM_BUILDER_KIND) {
        throw new Error('File is not a subscription form builder configuration.');
    }
    if (payload.version !== SUBSCRIPTION_FORM_BUILDER_VERSION) {
        throw new Error(`Unsupported configuration version: ${String(payload.version)}.`);
    }
    if (!Array.isArray(payload.fields)) {
        throw new Error('Configuration is missing a fields array.');
    }

    return payload.fields.map((field, index) => parseFieldConfig(field, index));
}

export function downloadBuilderConfig(fields: readonly FormField[], fileName: string): void {
    const blob = new Blob([serializeBuilderConfig(fields)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName.endsWith('.json') ? fileName : `${fileName}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
}

export function readBuilderConfigFile(file: File): Promise<FormField[]> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            try {
                resolve(parseBuilderConfig(String(reader.result ?? '')));
            } catch (error) {
                reject(error instanceof Error ? error : new Error('Failed to import configuration.'));
            }
        };
        reader.onerror = () => reject(new Error('Failed to read the selected file.'));
        reader.readAsText(file);
    });
}
