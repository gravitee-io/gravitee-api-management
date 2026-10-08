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
import { Alert, AlertDescription } from '@gravitee/graphene-core';

import { EndpointConfigurationSchemaForm, SharedConfigurationSchemaForm } from './SharedConfigurationSchemaForm';
import type { EndpointConfigurationFormState, SharedConfigFormState } from '../types';

interface ConfigurationStepProps {
    config: SharedConfigFormState;
    onChange: (next: SharedConfigFormState) => void;
    onSharedConfigValidityChange?: (valid: boolean) => void;
    /** Create-group flow: endpoint `configuration` schema, then group `sharedConfiguration` schema (Console parity). */
    showDefaultEndpointConfiguration?: boolean;
    defaultEndpointConfiguration?: EndpointConfigurationFormState;
    onDefaultEndpointConfigurationChange?: (next: EndpointConfigurationFormState) => void;
    onEndpointConfigurationValidityChange?: (valid: boolean) => void;
    /** Endpoint plugin id — loads plugin JSON Schemas (e.g. http-proxy, tcp-proxy). */
    endpointType: string;
    /** Disables all configuration controls (e.g. Kubernetes-managed / no update permission). */
    disabled?: boolean;
}

export function ConfigurationStep({
    config,
    onChange,
    onSharedConfigValidityChange,
    showDefaultEndpointConfiguration = false,
    defaultEndpointConfiguration = {},
    onDefaultEndpointConfigurationChange,
    onEndpointConfigurationValidityChange,
    endpointType,
    disabled = false,
}: Readonly<ConfigurationStepProps>) {
    return (
        <fieldset disabled={disabled} className="m-0 min-w-0 space-y-6 border-0 p-0 disabled:opacity-60">
            {showDefaultEndpointConfiguration && (
                <div className="space-y-4">
                    <Alert>
                        <AlertDescription>
                            <span className="font-medium">Inherited configuration</span>
                            <span className="block text-muted-foreground">
                                Endpoints associated to this endpoint group will automatically inherit its configuration.
                            </span>
                        </AlertDescription>
                    </Alert>
                    <EndpointConfigurationSchemaForm
                        endpointType={endpointType}
                        value={defaultEndpointConfiguration}
                        onChange={next => onDefaultEndpointConfigurationChange?.(next)}
                        onValidityChange={onEndpointConfigurationValidityChange}
                        disabled={disabled}
                    />
                </div>
            )}

            <SharedConfigurationSchemaForm
                endpointType={endpointType}
                value={config}
                onChange={onChange}
                onValidityChange={onSharedConfigValidityChange}
                disabled={disabled}
            />
        </fieldset>
    );
}
