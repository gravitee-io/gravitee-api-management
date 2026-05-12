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
export type SchemaSelectResult = {
    readonly options: readonly { value: string; label: string }[];
    readonly defaultValue?: string;
} | null;

export function readSchemaSelect(schema: unknown, propName: string): SchemaSelectResult {
    if (!schema || typeof schema !== 'object') return null;
    const properties = (schema as { properties?: unknown }).properties;
    if (!properties || typeof properties !== 'object') return null;
    const prop = (properties as Record<string, unknown>)[propName];
    if (!prop || typeof prop !== 'object') return null;
    const anyProp = prop as { enum?: unknown; default?: unknown; 'x-schema-form'?: unknown };
    const enumValues = Array.isArray(anyProp.enum) ? anyProp.enum.filter((v): v is string => typeof v === 'string') : [];
    if (!enumValues.length) return null;
    const xForm = anyProp['x-schema-form'];
    const titleMap =
        xForm && typeof xForm === 'object' && 'titleMap' in xForm && xForm.titleMap && typeof xForm.titleMap === 'object'
            ? (xForm.titleMap as Record<string, unknown>)
            : undefined;
    const options = enumValues.map(v => ({
        value: v,
        label: typeof titleMap?.[v] === 'string' ? (titleMap[v] as string) : v,
    }));
    const defaultValue = typeof anyProp.default === 'string' ? anyProp.default : undefined;
    return { options, defaultValue };
}
