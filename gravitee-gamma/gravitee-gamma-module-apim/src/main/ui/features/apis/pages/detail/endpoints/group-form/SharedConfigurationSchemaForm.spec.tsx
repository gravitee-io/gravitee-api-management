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
import { render, screen, waitFor } from '@testing-library/react';
import type { Control } from 'react-hook-form';

import { EndpointConfigurationSchemaForm, SharedConfigurationSchemaForm } from './SharedConfigurationSchemaForm';
import { useEndpointConfigurationSchema } from '../../../../hooks/useEndpointConfigurationSchema';
import { useEndpointSharedConfigurationSchema } from '../../../../hooks/useEndpointSharedConfigurationSchema';

jest.mock('../../../../hooks/useEndpointConfigurationSchema', () => ({
    useEndpointConfigurationSchema: jest.fn(() => ({ data: undefined, isLoading: false, isError: false })),
}));

jest.mock('../../../../hooks/useEndpointSharedConfigurationSchema', () => ({
    useEndpointSharedConfigurationSchema: jest.fn(() => ({
        data: {
            type: 'object',
            properties: {
                tcp: {
                    type: 'object',
                    properties: {
                        connectTimeout: { type: 'integer', default: 3000 },
                    },
                },
            },
        },
        isLoading: false,
        isError: false,
    })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    extractDefaults: () => ({ tcp: { connectTimeout: 3000 } }),
    jsonSchemaResolver: () => async (values: unknown) => ({ values, errors: {} }),
    JsonSchemaForm: ({ control }: { control: Control }) => {
        const { useWatch } = jest.requireActual('react-hook-form');
        const values = useWatch({ control });
        return <pre data-testid="schema-values">{JSON.stringify(values)}</pre>;
    },
    Skeleton: () => <div data-testid="skeleton" />,
}));

const sharedSchema = {
    data: {
        type: 'object',
        properties: {
            tcp: {
                type: 'object',
                properties: {
                    connectTimeout: { type: 'integer', default: 3000 },
                },
            },
        },
    },
    isLoading: false,
    isError: false,
};

describe('SharedConfigurationSchemaForm', () => {
    beforeEach(() => {
        jest.mocked(useEndpointSharedConfigurationSchema).mockReturnValue(sharedSchema);
    });

    it('pushes schema values to the parent and reports validity', async () => {
        const onChange = jest.fn();
        const onValidityChange = jest.fn();

        render(
            <SharedConfigurationSchemaForm endpointType="tcp-proxy" value={{}} onChange={onChange} onValidityChange={onValidityChange} />,
        );

        await waitFor(() => {
            expect(onChange).toHaveBeenCalledWith({ tcp: { connectTimeout: 3000 } });
            expect(onValidityChange).toHaveBeenCalledWith(true);
        });
    });

    it('applies a later value from the parent', async () => {
        const onChange = jest.fn();
        const { rerender } = render(
            <SharedConfigurationSchemaForm endpointType="tcp-proxy" value={{}} onChange={onChange} onValidityChange={jest.fn()} />,
        );

        await waitFor(() => {
            expect(onChange).toHaveBeenCalled();
        });
        onChange.mockClear();

        rerender(
            <SharedConfigurationSchemaForm
                endpointType="tcp-proxy"
                value={{ tcp: { connectTimeout: 9 } }}
                onChange={onChange}
                onValidityChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByTestId('schema-values')).toHaveTextContent('"connectTimeout":9');
        });
    });

    it('reports invalid and explains that Save stays disabled when the schema fails to load', () => {
        jest.mocked(useEndpointSharedConfigurationSchema).mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        });
        const onValidityChange = jest.fn();

        render(
            <SharedConfigurationSchemaForm endpointType="tcp-proxy" value={{}} onChange={jest.fn()} onValidityChange={onValidityChange} />,
        );

        expect(screen.getByText(/save stays disabled until the schema loads/i)).toBeInTheDocument();
        expect(onValidityChange).toHaveBeenCalledWith(false);
    });
});

describe('EndpointConfigurationSchemaForm', () => {
    it('reports invalid and explains that Save stays disabled when the schema fails to load', () => {
        jest.mocked(useEndpointConfigurationSchema).mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        });
        const onValidityChange = jest.fn();

        render(
            <EndpointConfigurationSchemaForm
                endpointType="http-proxy"
                value={{}}
                onChange={jest.fn()}
                onValidityChange={onValidityChange}
            />,
        );

        expect(screen.getByText(/save stays disabled until the schema loads/i)).toBeInTheDocument();
        expect(onValidityChange).toHaveBeenCalledWith(false);
    });
});
