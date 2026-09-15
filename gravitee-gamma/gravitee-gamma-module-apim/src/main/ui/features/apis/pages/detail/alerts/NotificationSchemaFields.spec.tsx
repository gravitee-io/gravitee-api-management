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
import { render, waitFor } from '@testing-library/react';

import { NotificationSchemaFields, stableConfigurationKey } from './NotificationSchemaFields';

const mockWatch = jest.fn();

jest.mock('@tanstack/react-query', () => ({
    useQuery: jest.fn(() => ({
        data: {
            type: 'object',
            properties: {
                to: { type: 'string', default: 'ops@example.com' },
                subject: { type: 'string' },
            },
        },
        isLoading: false,
        isError: false,
    })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    extractDefaults: () => ({ to: 'ops@example.com' }),
    jsonSchemaResolver: () => async () => ({ values: {}, errors: {} }),
    JsonSchemaForm: () => null,
    Skeleton: () => null,
}));

jest.mock('react-hook-form', () => ({
    useForm: () => ({
        control: {},
        watch: (callback: (values: { configuration: Record<string, unknown> }) => void) => {
            mockWatch(callback);
            callback({
                configuration: {
                    to: 'ops@example.com',
                    subject: 'Alert fired',
                },
            });
            return { unsubscribe: jest.fn() };
        },
    }),
}));

describe('stableConfigurationKey', () => {
    it('treats key order as equivalent', () => {
        expect(stableConfigurationKey({ a: 1, b: 2 })).toBe(stableConfigurationKey({ b: 2, a: 1 }));
    });
});

describe('NotificationSchemaFields', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('does not notify parent when watch emits the merged baseline only', async () => {
        const onChange = jest.fn();

        render(
            <NotificationSchemaFields
                environmentId="env-1"
                notifierId="email-notifier"
                value={{ subject: 'Alert fired' }}
                onChange={onChange}
                disabled={false}
            />,
        );

        await waitFor(() => expect(mockWatch).toHaveBeenCalled());
        expect(onChange).not.toHaveBeenCalled();
    });
});
