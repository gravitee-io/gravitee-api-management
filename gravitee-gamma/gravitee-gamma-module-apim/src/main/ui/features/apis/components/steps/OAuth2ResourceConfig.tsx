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
import {
    extractDefaults,
    type JsonSchema,
    JsonSchemaForm,
    jsonSchemaResolver,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
} from '@gravitee/graphene-core';
import { useEffect, useMemo } from 'react';
import { type FieldValues, type Resolver, useForm } from 'react-hook-form';

import { useResourcePlugins, useResourceSchema } from '../../hooks/useApiResources';
import { useApiCreation } from '../../store/apiCreationStore';
import type { ResourcePlugin } from '../../types/resource';

function isOAuth2Plugin(plugin: ResourcePlugin): boolean {
    return plugin.id.toLowerCase().includes('oauth2');
}

function OAuth2SchemaFields({ schema }: { schema: JsonSchema }) {
    const { state, dispatch } = useApiCreation();
    const storedConfig = state.form.oauth2ResourceConfig;

    const resolver = useMemo<Resolver<FieldValues>>(() => jsonSchemaResolver(schema, { basePath: 'configuration' }), [schema]);
    const defaultValues = useMemo(
        () => ({ configuration: { ...((extractDefaults(schema) ?? {}) as Record<string, unknown>), ...storedConfig } }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [schema],
    );
    const form = useForm<FieldValues>({ resolver, mode: 'onChange', defaultValues });
    const { isValid } = form.formState;

    useEffect(() => {
        const sub = form.watch(values =>
            dispatch({ type: 'UPDATE_FORM', patch: { oauth2ResourceConfig: (values.configuration ?? {}) as Record<string, unknown> } }),
        );
        return () => sub.unsubscribe();
    }, [form, dispatch]);

    useEffect(() => {
        void form.trigger();
    }, [form]);
    useEffect(() => {
        dispatch({ type: 'UPDATE_FORM', patch: { oauth2ResourceValid: isValid } });
    }, [isValid, dispatch]);

    return <JsonSchemaForm schema={schema} control={form.control} name="configuration" />;
}

export function OAuth2ResourceConfig() {
    const { state, dispatch } = useApiCreation();
    const { oauth2ResourceType } = state.form;

    const { data: plugins = [], isLoading: pluginsLoading } = useResourcePlugins();
    const oauthPlugins = useMemo(() => plugins.filter(isOAuth2Plugin), [plugins]);

    const { data: rawSchema, isLoading: schemaLoading } = useResourceSchema(oauth2ResourceType || undefined);
    const schema = rawSchema as JsonSchema | undefined;

    useEffect(() => {
        if (!oauth2ResourceType && oauthPlugins.length > 0) {
            dispatch({ type: 'UPDATE_FORM', patch: { oauth2ResourceType: oauthPlugins[0].id } });
        }
    }, [oauth2ResourceType, oauthPlugins, dispatch]);

    function selectProvider(id: string) {
        dispatch({ type: 'UPDATE_FORM', patch: { oauth2ResourceType: id, oauth2ResourceConfig: {}, oauth2ResourceValid: false } });
    }

    if (pluginsLoading) {
        return <Skeleton className="h-24 w-full rounded" />;
    }

    if (oauthPlugins.length === 0) {
        return (
            <p className="text-xs text-destructive">
                No OAuth2 resource plugin is installed on this gateway. Install an OAuth2 provider plugin to use OAuth 2.0 plans.
            </p>
        );
    }

    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="oauth2-provider">
                    OAuth2 Provider <span className="text-destructive">*</span>
                </Label>
                <Select value={oauth2ResourceType} onValueChange={selectProvider}>
                    <SelectTrigger id="oauth2-provider" className="w-full">
                        <SelectValue placeholder="Select an OAuth2 provider" />
                    </SelectTrigger>
                    <SelectContent>
                        {oauthPlugins.map(p => (
                            <SelectItem key={p.id} value={p.id}>
                                {p.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                    A resource of this type is created on the API and used to validate OAuth 2.0 tokens.
                </p>
            </div>

            {oauth2ResourceType &&
                (schemaLoading || !schema ? (
                    <Skeleton className="h-40 w-full rounded" />
                ) : (
                    <OAuth2SchemaFields key={oauth2ResourceType} schema={schema} />
                ))}
        </div>
    );
}
