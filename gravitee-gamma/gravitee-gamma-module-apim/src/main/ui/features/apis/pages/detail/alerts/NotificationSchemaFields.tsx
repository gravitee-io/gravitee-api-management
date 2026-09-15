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
import { extractDefaults, JsonSchemaForm, jsonSchemaResolver, Skeleton } from '@gravitee/graphene-core';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useRef } from 'react';
import { type FieldValues, type Resolver, useForm } from 'react-hook-form';

import { getNotifierSchema } from '../../../services/alertNotifiers';
import { adaptNotifierSchemaForForm } from '../../../utils/notifierSchema';
import { apiAlertKeys } from '../../../utils/queryKeys';

export function NotificationSchemaFields({
    environmentId,
    notifierId,
    value,
    onChange,
    disabled,
}: Readonly<{
    environmentId: string;
    notifierId: string;
    value: Record<string, unknown>;
    onChange: (configuration: Record<string, unknown>) => void;
    disabled: boolean;
}>) {
    const {
        data: schema,
        isLoading,
        isError,
    } = useQuery({
        queryKey: apiAlertKeys.notifierSchema(environmentId, notifierId),
        queryFn: () => getNotifierSchema(environmentId, notifierId),
        enabled: !!environmentId && !!notifierId,
    });

    if (!notifierId) {
        return null;
    }
    if (isLoading) {
        return <Skeleton className="h-24 w-full rounded" />;
    }
    if (isError || !schema) {
        return <p className="text-sm text-muted-foreground">Unable to load notification fields for this channel.</p>;
    }

    return <NotificationSchemaForm schema={schema} value={value} onChange={onChange} disabled={disabled} />;
}

/** Order-insensitive snapshot so schema default merges do not look like user edits. */
export function stableConfigurationKey(config: Record<string, unknown>): string {
    const normalize = (value: unknown): unknown => {
        if (Array.isArray(value)) {
            return value.map(normalize);
        }
        if (value && typeof value === 'object') {
            return Object.fromEntries(
                Object.entries(value as Record<string, unknown>)
                    .sort(([a], [b]) => a.localeCompare(b))
                    .map(([k, v]) => [k, normalize(v)]),
            );
        }
        return value;
    };
    return JSON.stringify(normalize(config));
}

function NotificationSchemaForm({
    schema,
    value,
    onChange,
    disabled,
}: Readonly<{
    schema: Record<string, unknown>;
    value: Record<string, unknown>;
    onChange: (configuration: Record<string, unknown>) => void;
    disabled: boolean;
}>) {
    const formSchema = useMemo(() => adaptNotifierSchemaForForm(schema), [schema]);
    const resolver = useMemo<Resolver<FieldValues>>(() => jsonSchemaResolver(formSchema, { basePath: 'configuration' }), [formSchema]);
    const defaultValues = useMemo(
        () => ({ configuration: { ...((extractDefaults(formSchema) ?? {}) as Record<string, unknown>), ...value } }),
        // Seed from the current value when the schema (channel) mounts; later edits flow through watch.
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [formSchema],
    );
    const form = useForm<FieldValues>({ resolver, mode: 'onChange', defaultValues });
    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;
    const baselineConfiguration = defaultValues.configuration as Record<string, unknown>;
    const lastSentKey = useRef(stableConfigurationKey(baselineConfiguration));

    useEffect(() => {
        lastSentKey.current = stableConfigurationKey(baselineConfiguration);
    }, [baselineConfiguration]);

    useEffect(() => {
        const push = (next: Record<string, unknown>) => {
            const key = stableConfigurationKey(next);
            if (key === lastSentKey.current) {
                return;
            }
            lastSentKey.current = key;
            onChangeRef.current(next);
        };
        const sub = form.watch(values => {
            push((values.configuration ?? {}) as Record<string, unknown>);
        });
        return () => sub.unsubscribe();
    }, [form]);

    return <JsonSchemaForm schema={formSchema} control={form.control} name="configuration" disabled={disabled} />;
}
