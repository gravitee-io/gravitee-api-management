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
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Skeleton,
} from '@gravitee/graphene-core';
import { CopyIcon, InfoIcon } from '@gravitee/graphene-core/icons';

import { copyTextToClipboardWithNotifyHandler } from '../../../shared/copyToClipboard';
import type { EnvironmentEntrypointConfig } from '../types/entrypoint';

function CopyableField({
    id,
    label,
    value,
    hint,
}: Readonly<{
    id: string;
    label: string;
    value: string;
    hint?: string;
}>) {
    return (
        <div className="space-y-2">
            <Label htmlFor={id}>{label}</Label>
            <div className="flex w-full gap-2">
                <Input id={id} value={value} readOnly className="min-w-0 flex-1 bg-muted/40" />
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    className="shrink-0"
                    onClick={() => copyTextToClipboardWithNotifyHandler(value, 'Copied to clipboard')}
                    disabled={!value}
                    aria-label={`Copy ${label}`}
                >
                    <CopyIcon className="size-4" aria-hidden />
                </Button>
            </div>
            {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
        </div>
    );
}

function EnvironmentConfigCard({ config }: Readonly<{ config: EnvironmentEntrypointConfig }>) {
    const portal = config.portalSettings.portal ?? {};
    const httpEntrypoint = portal.entrypoint ?? '';
    const tcpPort = portal.tcpPort !== undefined && portal.tcpPort !== null ? String(portal.tcpPort) : '';
    const kafkaDomain = portal.kafkaDomain ?? '';
    const kafkaPort = portal.kafkaPort !== undefined && portal.kafkaPort !== null ? String(portal.kafkaPort) : '';
    const envId = config.environment.id;

    return (
        <div className="space-y-4 border-t pt-4 first:border-t-0 first:pt-0">
            <h4 className="flex flex-wrap items-center gap-2 text-sm font-medium">
                Default values for environment:
                <Badge variant="secondary">{config.environment.name || config.environment.id}</Badge>
            </h4>
            <div className="space-y-4">
                <CopyableField id={`http-entrypoint-${envId}`} label="Default HTTP entrypoint" value={httpEntrypoint} />
                <CopyableField id={`tcp-port-${envId}`} label="Default TCP port" value={tcpPort} />
                <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_12rem]">
                    <CopyableField
                        id={`kafka-domain-${envId}`}
                        label="Default Kafka Bootstrap Domain Pattern"
                        value={kafkaDomain}
                        hint="To be configured according to the gateway configuration. e.g: {apiHost}.mycompany.org"
                    />
                    <CopyableField id={`kafka-port-${envId}`} label="Default Kafka port" value={kafkaPort} />
                </div>
            </div>
        </div>
    );
}

export function EntrypointConfigurationSection({
    configs,
    failedEnvironmentNames,
    isLoading,
    isError,
}: Readonly<{
    configs: EnvironmentEntrypointConfig[];
    failedEnvironmentNames: string[];
    isLoading: boolean;
    isError: boolean;
}>) {
    return (
        <Card>
            <CardHeader>
                <CardTitle>Entrypoint Configuration</CardTitle>
                <CardDescription>Default entrypoint values to be shown in the Developer Portal</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                {isLoading ? (
                    <div className="space-y-3">
                        {Array.from({ length: 3 }).map((_, i) => (
                            <Skeleton key={i} className="h-24 w-full rounded-md" />
                        ))}
                    </div>
                ) : isError ? (
                    <Alert variant="destructive">
                        <AlertDescription>Failed to load environments. Please refresh and try again.</AlertDescription>
                    </Alert>
                ) : (
                    <>
                        {failedEnvironmentNames.length > 0 ? (
                            <Alert>
                                <InfoIcon className="size-4" aria-hidden />
                                <AlertDescription>
                                    Could not load entrypoint defaults for: {failedEnvironmentNames.join(', ')}.
                                </AlertDescription>
                            </Alert>
                        ) : null}
                        {configs.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No environments found.</p>
                        ) : (
                            configs.map(config => <EnvironmentConfigCard key={config.environment.id} config={config} />)
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
}
