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
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Checkbox,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
    Skeleton,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { MessageSquareIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import type { BroadcastChannel, BroadcastPayload, RecipientOption } from '../../../types/broadcast';

// ─── Constants ────────────────────────────────────────────────────────────────

const TEXT_MAX = 250;

function isValidHttpUrl(value: string): boolean {
    try {
        const { protocol } = new URL(value);
        return protocol === 'https:' || protocol === 'http:';
    } catch {
        return false;
    }
}

const CHANNELS: { id: BroadcastChannel; label: string }[] = [
    { id: 'PORTAL', label: 'Portal Notifications' },
    { id: 'MAIL', label: 'Email' },
    { id: 'HTTP', label: 'POST HTTP Message' },
];

// ─── Recipients list ──────────────────────────────────────────────────────────

interface RecipientsListProps {
    options: RecipientOption[];
    selected: string[];
    isLoading: boolean;
    onToggle: (name: string) => void;
}

function RecipientsList({ options, selected, isLoading, onToggle }: Readonly<RecipientsListProps>) {
    if (isLoading) {
        return (
            <div className="border rounded-md p-2 space-y-2">
                {[1, 2, 3].map(i => (
                    <Skeleton key={i} className="h-6 w-full rounded" />
                ))}
            </div>
        );
    }
    return (
        <div className="border rounded-md max-h-52 overflow-y-auto" role="group" aria-label="Recipients">
            {options.map((opt, idx) => (
                <label
                    key={opt.name}
                    className="flex items-center gap-3 px-3 py-2 cursor-pointer hover:bg-muted transition-colors"
                    style={idx > 0 ? { borderTop: '1px solid var(--color-border)' } : undefined}
                >
                    <Checkbox
                        checked={selected.includes(opt.name)}
                        onCheckedChange={() => onToggle(opt.name)}
                        aria-label={opt.displayName}
                    />
                    <span className="text-sm">{opt.displayName}</span>
                </label>
            ))}
        </div>
    );
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface BroadcastFormProps {
    recipientOptions: RecipientOption[];
    isLoadingRecipients: boolean;
    isPending: boolean;
    error: string | null;
    onSend: (payload: BroadcastPayload) => void;
    onCancel: () => void;
}

// ─── Component ────────────────────────────────────────────────────────────────

export function BroadcastForm({ recipientOptions, isLoadingRecipients, isPending, error, onSend, onCancel }: Readonly<BroadcastFormProps>) {
    const [channel, setChannel] = useState<BroadcastChannel>('PORTAL');
    const [selectedRecipients, setSelectedRecipients] = useState<string[]>([]);
    const [title, setTitle] = useState('');
    const [url, setUrl] = useState('');
    const [text, setText] = useState('');
    const [useSystemProxy, setUseSystemProxy] = useState(false);

    const isHttp = channel === 'HTTP';
    const charsLeft = TEXT_MAX - text.length;

    const isValid =
        selectedRecipients.length > 0 &&
        (isHttp ? isValidHttpUrl(url) : title.trim().length > 0) &&
        text.trim().length > 0 &&
        text.length <= TEXT_MAX;

    const handleChannelChange = useCallback((value: string) => {
        setChannel(value as BroadcastChannel);
        setTitle('');
        setUrl('');
    }, []);

    const toggleRecipient = useCallback((name: string) => {
        setSelectedRecipients(prev => (prev.includes(name) ? prev.filter(r => r !== name) : [...prev, name]));
    }, []);

    const handleSend = useCallback(() => {
        if (!isValid || isPending) return;

        if (isHttp) {
            onSend({
                channel: 'HTTP',
                text: text.trim(),
                recipient: { url: url.trim() },
                params: {},
                useSystemProxy,
            });
        } else {
            onSend({
                channel,
                title: title.trim(),
                text: text.trim(),
                recipient: {
                    role_scope: 'APPLICATION',
                    role_value: selectedRecipients,
                },
            });
        }
    }, [isValid, isPending, isHttp, selectedRecipients, channel, title, text, url, useSystemProxy, onSend]);

    return (
        <Card style={{ border: '2px solid color-mix(in oklab, var(--color-primary) 25%, transparent)' }}>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm">Compose broadcast</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                {/* Info banner */}
                <div
                    className="flex items-start gap-3 rounded-lg p-3"
                    style={{
                        backgroundColor: 'color-mix(in oklab, var(--color-primary) 6%, transparent)',
                        border: '1px solid color-mix(in oklab, var(--color-primary) 20%, transparent)',
                    }}
                >
                    <MessageSquareIcon className="size-4 shrink-0 mt-0.5 text-primary" aria-hidden />
                    <p className="text-sm text-muted-foreground">
                        Send a one-way message to specified recipients to inform them of any changes or updates.
                    </p>
                </div>

                {/* Channel */}
                <div className="space-y-2">
                    <Label htmlFor="broadcast-channel">
                        Channel <span className="text-destructive">*</span>
                    </Label>
                    <Select value={channel} onValueChange={handleChannelChange}>
                        <SelectTrigger id="broadcast-channel" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {CHANNELS.map(c => (
                                <SelectItem key={c.id} value={c.id}>
                                    {c.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {/* Recipients */}
                <div className="space-y-2">
                    <Label>
                        Recipients <span className="text-destructive">*</span>
                    </Label>
                    <RecipientsList
                        options={recipientOptions}
                        selected={selectedRecipients}
                        isLoading={isLoadingRecipients}
                        onToggle={toggleRecipient}
                    />
                    {selectedRecipients.length > 0 && (
                        <p className="text-xs text-muted-foreground">
                            {selectedRecipients.length} recipient{selectedRecipients.length !== 1 ? 's' : ''} selected
                        </p>
                    )}
                </div>

                {/* Title — PORTAL / MAIL only */}
                {!isHttp && (
                    <div className="space-y-2">
                        <Label htmlFor="broadcast-title">
                            Title <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="broadcast-title"
                            value={title}
                            onChange={e => setTitle(e.target.value)}
                            placeholder="e.g. Scheduled maintenance — 2 Jun"
                            required
                        />
                    </div>
                )}

                {/* URL + system proxy — HTTP only */}
                {isHttp && (
                    <>
                        <div className="space-y-2">
                            <Label htmlFor="broadcast-url">
                                URL <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="broadcast-url"
                                type="url"
                                value={url}
                                onChange={e => setUrl(e.target.value)}
                                placeholder="https://hooks.example.com/notify"
                                required
                            />
                        </div>
                        <div className="flex items-center gap-3">
                            <Switch
                                id="broadcast-proxy"
                                checked={useSystemProxy}
                                onCheckedChange={setUseSystemProxy}
                                aria-label="Use system proxy"
                            />
                            <Label htmlFor="broadcast-proxy" className="cursor-pointer font-normal">
                                Use system proxy
                            </Label>
                        </div>
                    </>
                )}

                {/* Text */}
                <div className="flex flex-col gap-2">
                    <Label htmlFor="broadcast-text">
                        Message <span className="text-destructive">*</span>
                    </Label>
                    <Textarea
                        id="broadcast-text"
                        value={text}
                        onChange={e => setText(e.target.value)}
                        placeholder="Describe the change, maintenance window, or update…"
                        rows={4}
                        maxLength={TEXT_MAX}
                    />
                    <p
                        className="text-xs text-right"
                        style={{ color: charsLeft <= 20 ? 'var(--color-destructive)' : 'var(--color-muted-foreground)' }}
                    >
                        {charsLeft} / {TEXT_MAX}
                    </p>
                </div>

                {/* Error */}
                {error && (
                    <Alert variant="destructive">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}

                <Separator />

                {/* Actions */}
                <div className="flex items-center justify-end gap-3">
                    <Button type="button" variant="outline" size="sm" onClick={onCancel} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" size="sm" onClick={handleSend} disabled={!isValid || isPending}>
                        {isPending ? 'Sending…' : 'Send'}
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}
