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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Switch } from '@gravitee/graphene-core';

export type ApiKeySource = 'HEADER' | 'BEARER' | 'QUERY_PARAMETER';

export const API_KEY_SOURCE_OPTIONS: { value: ApiKeySource; label: string }[] = [
    { value: 'HEADER', label: "Get API key in headers (with fallback on query parameter 'api-key')" },
    { value: 'BEARER', label: 'Get API key in authorization Bearer header (incompatible with APIv2 and OAuth)' },
    { value: 'QUERY_PARAMETER', label: 'Get API key in query parameter' },
];

export interface ApiKeyConfig {
    propagateApiKey: boolean;
    source: ApiKeySource;
    enableCustomApiKeyHeader: boolean;
    apiKeyHeader: string;
}

export const DEFAULT_API_KEY_CONFIG: ApiKeyConfig = {
    propagateApiKey: false,
    source: 'HEADER',
    enableCustomApiKeyHeader: false,
    apiKeyHeader: 'X-Gravitee-Api-Key',
};

interface ApiKeySecurityFieldsProps {
    value: ApiKeyConfig;
    onChange: (v: ApiKeyConfig) => void;
    readOnly?: boolean;
}

export function ApiKeySecurityFields({ value, onChange, readOnly = false }: Readonly<ApiKeySecurityFieldsProps>) {
    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="propagate-api-key" className="text-sm font-medium">
                        Propagate API Key to upstream API
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        Forward the API Key header to the backend service after gateway validation.
                    </p>
                </div>
                <Switch
                    id="propagate-api-key"
                    checked={value.propagateApiKey}
                    onCheckedChange={checked => onChange({ ...value, propagateApiKey: checked })}
                    disabled={readOnly}
                />
            </div>

            <div className="space-y-2">
                <Label htmlFor="api-key-source">API Key location</Label>
                <Select
                    value={value.source}
                    onValueChange={source =>
                        onChange({
                            ...value,
                            source: source as ApiKeySource,
                            ...(source !== 'HEADER' && { enableCustomApiKeyHeader: false, apiKeyHeader: 'X-Gravitee-Api-Key' }),
                        })
                    }
                    disabled={readOnly}
                >
                    <SelectTrigger id="api-key-source" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {API_KEY_SOURCE_OPTIONS.map(opt => (
                            <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">Where the gateway reads the API key from the incoming request.</p>
            </div>

            {value.source === 'HEADER' && (
                <>
                    <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                        <div className="space-y-0.5">
                            <Label htmlFor="enable-custom-api-key-header" className="text-sm font-medium">
                                Custom API Key header
                            </Label>
                            <p className="text-xs text-muted-foreground">Override the default API Key header name (X-Gravitee-Api-Key).</p>
                        </div>
                        <Switch
                            id="enable-custom-api-key-header"
                            checked={value.enableCustomApiKeyHeader}
                            onCheckedChange={checked =>
                                onChange({
                                    ...value,
                                    enableCustomApiKeyHeader: checked,
                                    apiKeyHeader: checked ? value.apiKeyHeader : 'X-Gravitee-Api-Key',
                                })
                            }
                            disabled={readOnly}
                        />
                    </div>

                    {value.enableCustomApiKeyHeader && (
                        <div className="space-y-2">
                            <Label htmlFor="api-key-header">API Key header name</Label>
                            <Input
                                id="api-key-header"
                                value={value.apiKeyHeader}
                                onChange={e => onChange({ ...value, apiKeyHeader: e.target.value })}
                                placeholder="X-Gravitee-Api-Key"
                                disabled={readOnly}
                            />
                            <p className="text-xs text-muted-foreground">
                                The header name used to pass the API key. Only alphanumeric and special header characters allowed.
                            </p>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
