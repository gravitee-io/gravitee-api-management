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
import { adaptNotifierSchemaForForm, isNotifierConfigurationComplete } from './notifierSchema';

describe('adaptNotifierSchemaForForm', () => {
    it('sets uniqueItems on enum arrays for Graphene multi-select', () => {
        const adapted = adaptNotifierSchemaForForm({
            type: 'object',
            properties: {
                channels: { type: 'array', items: { type: 'string', enum: ['a', 'b'] } },
            },
        }) as { properties?: { channels?: { uniqueItems?: boolean } } };
        expect(adapted.properties?.channels?.uniqueItems).toBe(true);
    });
});

describe('isNotifierConfigurationComplete', () => {
    it('requires nested object fields when parent value is missing', () => {
        const schema = {
            type: 'object',
            required: ['target'],
            properties: {
                target: {
                    type: 'object',
                    required: ['email'],
                    properties: { email: { type: 'string' } },
                },
            },
        };
        expect(isNotifierConfigurationComplete(schema, {})).toBe(false);
    });
});
