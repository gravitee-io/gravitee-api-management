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
import { extractDefaults, type JsonSchema, JsonSchemaForm, jsonSchemaResolver, Skeleton } from '@gravitee/graphene-core';
import { useEffect, useMemo, useRef, useState } from 'react';
import { type FieldValues, type Resolver, useForm } from 'react-hook-form';

import { useEndpointConfigurationSchema } from '../../../../hooks/useEndpointConfigurationSchema';
import { useEndpointSharedConfigurationSchema } from '../../../../hooks/useEndpointSharedConfigurationSchema';
import { adaptSharedConfigurationSchemaForForm, sanitizeSharedConfigurationValues } from '../../../../utils/sharedConfigurationSchema';
import type { SharedConfigFormState } from '../types';

type PluginSchemaValues = Record<string, unknown>;

function PluginSchemaFormFields({
    schema,
    value,
    onChange,
    onValidityChange,
    disabled,
    sanitizeSharedSsl,
}: Readonly<{
    schema: JsonSchema;
    value: PluginSchemaValues;
    onChange: (configuration: PluginSchemaValues) => void;
    onValidityChange?: (valid: boolean) => void;
    disabled: boolean;
    sanitizeSharedSsl: boolean;
}>) {
    const resolver = useMemo<Resolver<FieldValues>>(() => {
        const base = jsonSchemaResolver(schema);
        if (!sanitizeSharedSsl) {
            return base;
        }
        return (values, context, options) => base(sanitizeSharedConfigurationValues(values), context, options);
    }, [schema, sanitizeSharedSsl]);

    const externalValues = sanitizeSharedSsl ? sanitizeSharedConfigurationValues(value) : value;
    const externalKey = JSON.stringify(externalValues);
    const [defaultValues] = useState(() => ({
        ...((extractDefaults(schema) as Record<string, unknown>) ?? {}),
        ...externalValues,
    }));
    const form = useForm<FieldValues>({
        resolver,
        mode: 'onChange',
        criteriaMode: 'all',
        defaultValues,
    });
    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;
    const onValidityChangeRef = useRef(onValidityChange);
    onValidityChangeRef.current = onValidityChange;
    const lastEmitted = useRef(externalKey);
    const appliedSchema = useRef(schema);
    const { isValid } = form.formState;

    useEffect(() => {
        const schemaChanged = appliedSchema.current !== schema;
        if (!schemaChanged && externalKey === lastEmitted.current) {
            return;
        }
        const parsed = JSON.parse(externalKey) as Record<string, unknown>;
        const next = {
            ...((extractDefaults(schema) as Record<string, unknown>) ?? {}),
            ...parsed,
        };
        const sanitized = sanitizeSharedSsl ? sanitizeSharedConfigurationValues(next) : next;
        lastEmitted.current = JSON.stringify(sanitized);
        appliedSchema.current = schema;
        form.reset(sanitized);
    }, [externalKey, form, schema, sanitizeSharedSsl]);

    useEffect(() => {
        const push = (next: Record<string, unknown>) => {
            const nextValues = sanitizeSharedSsl ? sanitizeSharedConfigurationValues(next) : next;
            const serialized = JSON.stringify(nextValues);
            if (serialized === lastEmitted.current) {
                return;
            }
            lastEmitted.current = serialized;
            onChangeRef.current(nextValues);
        };
        push(form.getValues() as Record<string, unknown>);
        const sub = form.watch(values => {
            push(values as Record<string, unknown>);
        });
        return () => sub.unsubscribe();
    }, [form, sanitizeSharedSsl]);

    useEffect(() => {
        void form.trigger();
    }, [form, schema]);

    useEffect(() => {
        onValidityChangeRef.current?.(isValid);
    }, [isValid]);

    return (
        <div className="w-full min-w-0 space-y-6">
            <JsonSchemaForm schema={schema} control={form.control} name="" disabled={disabled} />
        </div>
    );
}

/** Endpoint-level plugin configuration (Classic: first `gio-form-json-schema` on group create). */
export function EndpointConfigurationSchemaForm({
    endpointType,
    value,
    onChange,
    onValidityChange,
    disabled = false,
}: Readonly<{
    endpointType: string;
    value: PluginSchemaValues;
    onChange: (configuration: PluginSchemaValues) => void;
    onValidityChange?: (valid: boolean) => void;
    disabled?: boolean;
}>) {
    const { data: schema, isLoading, isError } = useEndpointConfigurationSchema(endpointType);
    const onValidityChangeRef = useRef(onValidityChange);
    onValidityChangeRef.current = onValidityChange;

    useEffect(() => {
        if (isLoading || isError || !schema) {
            onValidityChangeRef.current?.(false);
        }
    }, [isLoading, isError, schema]);

    if (isLoading) {
        return <Skeleton className="h-24 w-full rounded" />;
    }
    if (isError || !schema) {
        return (
            <p className="text-sm text-muted-foreground">
                Unable to load endpoint configuration. Save stays disabled until the schema loads.
            </p>
        );
    }

    return (
        <PluginSchemaFormFields
            schema={schema}
            value={value}
            onChange={onChange}
            onValidityChange={onValidityChange}
            disabled={disabled}
            sanitizeSharedSsl={false}
        />
    );
}

/** Group shared configuration (Classic: second `gio-form-json-schema` on group create). */
export function SharedConfigurationSchemaForm({
    endpointType,
    value,
    onChange,
    onValidityChange,
    disabled = false,
}: Readonly<{
    endpointType: string;
    value: SharedConfigFormState;
    onChange: (configuration: SharedConfigFormState) => void;
    onValidityChange?: (valid: boolean) => void;
    disabled?: boolean;
}>) {
    const { data: schema, isLoading, isError } = useEndpointSharedConfigurationSchema(endpointType);
    const formSchema = useMemo(
        () => (schema ? adaptSharedConfigurationSchemaForForm(schema as Record<string, unknown>) : undefined),
        [schema],
    );
    const onValidityChangeRef = useRef(onValidityChange);
    onValidityChangeRef.current = onValidityChange;

    useEffect(() => {
        if (isLoading || isError || !formSchema) {
            onValidityChangeRef.current?.(false);
        }
    }, [isLoading, isError, formSchema]);

    if (isLoading) {
        return <Skeleton className="h-24 w-full rounded" />;
    }
    if (isError || !formSchema) {
        return (
            <p className="text-sm text-muted-foreground">
                Unable to load shared configuration. Save stays disabled until the schema loads.
            </p>
        );
    }

    return (
        <PluginSchemaFormFields
            schema={formSchema}
            value={value}
            onChange={onChange}
            onValidityChange={onValidityChange}
            disabled={disabled}
            sanitizeSharedSsl={true}
        />
    );
}
