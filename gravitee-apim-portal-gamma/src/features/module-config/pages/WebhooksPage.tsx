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
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
    Switch,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify/notify';
import { MODULE_CONFIG_SECTION_META } from '../types';

const WEBHOOK_EVENTS = [
    'portal.published',
    'subscription.created',
    'user.signed_up',
    'page.updated',
] as const;

type WebhookEvent = (typeof WEBHOOK_EVENTS)[number];

interface WebhookEndpoint {
    readonly id: string;
    readonly url: string;
    readonly events: readonly WebhookEvent[];
    readonly enabled: boolean;
}

const SAMPLE_WEBHOOKS: readonly WebhookEndpoint[] = [
    {
        id: 'wh-1',
        url: 'https://hooks.example.com/portal-events',
        events: ['portal.published', 'page.updated'],
        enabled: true,
    },
    {
        id: 'wh-2',
        url: 'https://ops.acme.io/webhooks/subscriptions',
        events: ['subscription.created'],
        enabled: true,
    },
    {
        id: 'wh-3',
        url: 'https://crm.acme.io/ingest/signup',
        events: ['user.signed_up'],
        enabled: false,
    },
];

function createWebhookId(): string {
    return `wh-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export function WebhooksPage() {
    const [webhooks, setWebhooks] = useState<WebhookEndpoint[]>([...SAMPLE_WEBHOOKS]);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [webhookToDelete, setWebhookToDelete] = useState<WebhookEndpoint | null>(null);

    const meta = MODULE_CONFIG_SECTION_META.webhooks;

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={() => setDialogOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add webhook
                </Button>
            </div>

            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[40rem] border-collapse text-left text-sm">
                            <caption className="sr-only">Webhook endpoints</caption>
                            <thead className="border-b border-border/70 bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
                                <tr>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        URL
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Events
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Enabled
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {webhooks.length === 0 ? (
                                    <tr>
                                        <td colSpan={4} className="px-5 py-10 text-center text-muted-foreground">
                                            No webhooks configured.
                                        </td>
                                    </tr>
                                ) : (
                                    webhooks.map(webhook => (
                                        <tr key={webhook.id} className="border-b border-border/60 last:border-b-0">
                                            <td className="px-5 py-4 align-middle font-medium">{webhook.url}</td>
                                            <td className="px-5 py-4 align-middle text-muted-foreground">
                                                {webhook.events.join(', ')}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <Switch
                                                    checked={webhook.enabled}
                                                    onCheckedChange={checked =>
                                                        setWebhooks(current =>
                                                            current.map(item =>
                                                                item.id === webhook.id
                                                                    ? { ...item, enabled: checked === true }
                                                                    : item,
                                                            ),
                                                        )
                                                    }
                                                    aria-label={`${webhook.enabled ? 'Disable' : 'Enable'} webhook`}
                                                />
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="sm"
                                                    aria-label={`Delete webhook ${webhook.url}`}
                                                    onClick={() => setWebhookToDelete(webhook)}
                                                >
                                                    <Trash2Icon className="size-4" aria-hidden />
                                                </Button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            <AddWebhookDialog
                open={dialogOpen}
                onOpenChange={setDialogOpen}
                onAdd={input => {
                    setWebhooks(current => [
                        ...current,
                        {
                            id: createWebhookId(),
                            url: input.url,
                            events: input.events,
                            enabled: true,
                        },
                    ]);
                    notify.success('Webhook added.');
                }}
            />

            <ConfirmDialog
                open={webhookToDelete !== null}
                onOpenChange={open => {
                    if (!open) {
                        setWebhookToDelete(null);
                    }
                }}
                title="Delete webhook?"
                description={
                    webhookToDelete
                        ? `Stop sending events to "${webhookToDelete.url}"?`
                        : undefined
                }
                confirmLabel="Delete"
                destructive
                onConfirm={() => {
                    if (!webhookToDelete) {
                        return;
                    }
                    setWebhooks(current => current.filter(item => item.id !== webhookToDelete.id));
                    setWebhookToDelete(null);
                    notify.success('Webhook deleted.');
                }}
            />
        </div>
    );
}

function AddWebhookDialog({
    open,
    onOpenChange,
    onAdd,
}: {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onAdd: (input: { url: string; events: WebhookEvent[] }) => void;
}) {
    const [url, setUrl] = useState('');
    const [events, setEvents] = useState<WebhookEvent[]>(['portal.published']);

    useEffect(() => {
        if (!open) {
            setUrl('');
            setEvents(['portal.published']);
        }
    }, [open]);

    const toggleEvent = (eventName: WebhookEvent, checked: boolean) => {
        setEvents(current =>
            checked ? [...current, eventName] : current.filter(item => item !== eventName),
        );
    };

    const canSubmit = url.trim().length > 0 && events.length > 0;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 28rem)' }}>
                <DialogHeader>
                    <DialogTitle>Add webhook</DialogTitle>
                    <DialogDescription>
                        Deliver selected portal events to an HTTPS endpoint. Delivery is mocked in this POC.
                    </DialogDescription>
                </DialogHeader>
                <form
                    className="space-y-4 py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!canSubmit) {
                            return;
                        }
                        onAdd({ url: url.trim(), events });
                        onOpenChange(false);
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="webhook-url">Endpoint URL</FieldLabel>
                        <Input
                            id="webhook-url"
                            value={url}
                            onChange={event => setUrl(event.target.value)}
                            placeholder="https://example.com/hooks/portal"
                            autoFocus
                            required
                        />
                    </Field>
                    <div className="space-y-2">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                            Events
                        </p>
                        {WEBHOOK_EVENTS.map(eventName => (
                            <label key={eventName} className="flex items-center gap-2 text-sm">
                                <Checkbox
                                    checked={events.includes(eventName)}
                                    onCheckedChange={checked => toggleEvent(eventName, checked === true)}
                                />
                                {eventName}
                            </label>
                        ))}
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit}>
                            Add webhook
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
