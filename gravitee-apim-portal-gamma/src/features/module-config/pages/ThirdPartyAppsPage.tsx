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
import {
    Button,
    Card,
    CardContent,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { notify } from '../../../shared/notify/notify';
import { MODULE_CONFIG_SECTION_META } from '../types';

interface ThirdPartyApp {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly connected: boolean;
    readonly clientId: string;
    readonly clientSecret: string;
}

const INITIAL_APPS: readonly ThirdPartyApp[] = [
    {
        id: 'slack',
        name: 'Slack',
        description: 'Post portal publish and subscription alerts to a Slack channel.',
        connected: true,
        clientId: 'slack-demo-client',
        clientSecret: '••••••••',
    },
    {
        id: 'jira',
        name: 'Jira',
        description: 'Create issues when subscription approvals fail or time out.',
        connected: false,
        clientId: '',
        clientSecret: '',
    },
    {
        id: 'hubspot',
        name: 'HubSpot',
        description: 'Sync new developer signups to your CRM contacts list.',
        connected: false,
        clientId: '',
        clientSecret: '',
    },
    {
        id: 'custom-oauth',
        name: 'Custom OAuth app',
        description: 'Register a generic OAuth 2.0 client for your own integration.',
        connected: false,
        clientId: '',
        clientSecret: '',
    },
];

export function ThirdPartyAppsPage() {
    const [apps, setApps] = useState<ThirdPartyApp[]>([...INITIAL_APPS]);
    const [configureApp, setConfigureApp] = useState<ThirdPartyApp | null>(null);

    const meta = MODULE_CONFIG_SECTION_META['third-party-apps'];

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                <p className="text-sm text-muted-foreground">{meta.description}</p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {apps.map(app => (
                    <Card key={app.id}>
                        <CardContent className="flex h-full flex-col gap-4 pt-6">
                            <div className="space-y-1">
                                <div className="flex items-center justify-between gap-2">
                                    <h2 className="text-base font-semibold">{app.name}</h2>
                                    <span
                                        className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                                            app.connected
                                                ? 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400'
                                                : 'bg-muted text-muted-foreground'
                                        }`}
                                    >
                                        {app.connected ? 'Connected' : 'Not connected'}
                                    </span>
                                </div>
                                <p className="text-sm text-muted-foreground">{app.description}</p>
                            </div>
                            <div className="mt-auto flex flex-wrap gap-2">
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={() => setConfigureApp(app)}
                                >
                                    Configure
                                </Button>
                                <Button
                                    type="button"
                                    size="sm"
                                    variant={app.connected ? 'outline' : 'default'}
                                    onClick={() => {
                                        setApps(current =>
                                            current.map(item =>
                                                item.id === app.id
                                                    ? { ...item, connected: !item.connected }
                                                    : item,
                                            ),
                                        );
                                        notify.success(
                                            app.connected
                                                ? `${app.name} disconnected.`
                                                : `${app.name} connected.`,
                                        );
                                    }}
                                >
                                    {app.connected ? 'Disconnect' : 'Connect'}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                ))}
            </div>

            <ConfigureAppDialog
                open={configureApp !== null}
                app={configureApp}
                onOpenChange={open => {
                    if (!open) {
                        setConfigureApp(null);
                    }
                }}
                onSave={(id, values) => {
                    setApps(current =>
                        current.map(item =>
                            item.id === id
                                ? {
                                      ...item,
                                      clientId: values.clientId,
                                      clientSecret: values.clientSecret,
                                      connected: true,
                                  }
                                : item,
                        ),
                    );
                    setConfigureApp(null);
                    notify.success('Integration configuration saved.');
                }}
            />
        </div>
    );
}

function ConfigureAppDialog({
    open,
    app,
    onOpenChange,
    onSave,
}: {
    readonly open: boolean;
    readonly app: ThirdPartyApp | null;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSave: (id: string, values: { clientId: string; clientSecret: string }) => void;
}) {
    const [clientId, setClientId] = useState('');
    const [clientSecret, setClientSecret] = useState('');

    useEffect(() => {
        if (!open || !app) {
            return;
        }
        setClientId(app.clientId === '••••••••' ? '' : app.clientId);
        setClientSecret(app.clientSecret === '••••••••' ? '' : app.clientSecret);
    }, [open, app]);

    const canSubmit = clientId.trim().length > 0 && clientSecret.trim().length > 0 && app !== null;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 28rem)' }}>
                <DialogHeader>
                    <DialogTitle>Configure {app?.name ?? 'app'}</DialogTitle>
                    <DialogDescription>
                        OAuth credentials are stored in memory only for this POC.
                    </DialogDescription>
                </DialogHeader>
                <form
                    className="space-y-4 py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!canSubmit || !app) {
                            return;
                        }
                        onSave(app.id, {
                            clientId: clientId.trim(),
                            clientSecret: clientSecret.trim(),
                        });
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="app-client-id">Client ID</FieldLabel>
                        <Input
                            id="app-client-id"
                            value={clientId}
                            onChange={event => setClientId(event.target.value)}
                            required
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="app-client-secret">Client secret</FieldLabel>
                        <Input
                            id="app-client-secret"
                            type="password"
                            value={clientSecret}
                            onChange={event => setClientSecret(event.target.value)}
                            required
                        />
                    </Field>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit}>
                            Save
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
