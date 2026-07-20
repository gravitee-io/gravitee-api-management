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
import { parseBuilderConfig, serializeBuilderConfig } from './subscription-form-config';
import type { FormField } from '../types';

describe('subscription-form-config', () => {
    const fields: FormField[] = [
        {
            id: 'field-1',
            type: 'text',
            label: 'Name',
            required: true,
            options: [],
            validation: '^.+$',
            expression: '',
        },
        {
            id: 'field-2',
            type: 'dropdown',
            label: 'Country',
            required: false,
            options: ['US', 'FR'],
            validation: '',
            expression: "{#api.metadata['countries']}",
        },
    ];

    it('should serialize builder fields without ids', () => {
        const json = serializeBuilderConfig(fields);
        const parsed = JSON.parse(json);

        expect(parsed.kind).toBe('gravitee-subscription-form-builder');
        expect(parsed.version).toBe(1);
        expect(parsed.fields).toEqual([
            {
                type: 'text',
                label: 'Name',
                required: true,
                options: [],
                validation: '^.+$',
                expression: '',
            },
            {
                type: 'dropdown',
                label: 'Country',
                required: false,
                options: ['US', 'FR'],
                validation: '',
                expression: "{#api.metadata['countries']}",
            },
        ]);
        expect(json).not.toContain('field-1');
    });

    it('should parse builder config and regenerate field ids', () => {
        const imported = parseBuilderConfig(serializeBuilderConfig(fields));

        expect(imported).toHaveLength(2);
        expect(imported[0]?.id).toBeTruthy();
        expect(imported[0]?.id).not.toBe('field-1');
        expect(imported[0]?.label).toBe('Name');
        expect(imported[0]?.required).toBe(true);
        expect(imported[1]?.expression).toBe("{#api.metadata['countries']}");
        expect(imported[1]?.options).toEqual(['US', 'FR']);
    });

    it('should reject invalid configuration files', () => {
        expect(() => parseBuilderConfig('{"kind":"other","version":1,"fields":[]}')).toThrow(
            /not a subscription form builder/i,
        );
        expect(() =>
            parseBuilderConfig('{"kind":"gravitee-subscription-form-builder","version":99,"fields":[]}'),
        ).toThrow(/Unsupported configuration version/i);
    });
});
