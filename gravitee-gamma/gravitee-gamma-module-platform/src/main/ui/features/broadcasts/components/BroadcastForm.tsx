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
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxItem,
    ComboboxList,
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
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { MessageSquareIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import type { BroadcastChannel, BroadcastPayload, BroadcastRecipientOption } from '../types';

const TEXT_MAX = 4000;

const CHANNELS: { id: BroadcastChannel; label: string }[] = [
    { id: 'PORTAL', label: 'Portal Notifications' },
    { id: 'MAIL', label: 'Email' },
    { id: 'HTTP', label: 'POST HTTP Message' },
];

function isValidHttpUrl(value: string): boolean {
    try {
        const { protocol } = new URL(value);
        return protocol === 'https:' || protocol === 'http:';
    } catch {
        return false;
    }
}

function newHeaderId(): string {
    return typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : `header-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function newHeaderRow(): { id: string; key: string; value: string } {
    return { id: newHeaderId(), key: '', value: '' };
}

function RecipientsSelect({
    options,
    selected,
    isLoading,
    onChange,
}: Readonly<{
    options: BroadcastRecipientOption[];
    selected: string[];
    isLoading: boolean;
    onChange: (values: string[]) => void;
}>) {
    const anchorRef = useComboboxAnchor();
    const selectedOptions = options.filter(opt => selected.includes(opt.name));

    if (isLoading) {
        return <Skeleton className="h-9 w-full rounded-md" />;
    }

    return (
        <Combobox
            multiple
            value={selected}
            onValueChange={value => onChange(Array.isArray(value) ? value : [value].filter(Boolean))}
            autoComplete="list"
        >
            <ComboboxChips ref={anchorRef}>
                {selectedOptions.map(opt => (
                    <ComboboxChip key={opt.name} removeAriaLabel={`Remove ${opt.displayName}`}>
                        {opt.displayName}
                    </ComboboxChip>
                ))}
                <ComboboxChipsInput
                    id="broadcast-recipients"
                    placeholder={selected.length === 0 ? 'Select recipients' : ''}
                    aria-label="Recipients"
                    readOnly
                />
            </ComboboxChips>
            <ComboboxContent anchor={anchorRef} align="start">
                <ComboboxEmpty>No recipient roles available.</ComboboxEmpty>
                <ComboboxList>
                    {options.map(opt => (
                        <ComboboxItem key={opt.name} value={opt.name}>
                            {opt.displayName}
                        </ComboboxItem>
                    ))}
                </ComboboxList>
            </ComboboxContent>
        </Combobox>
    );
}

export function BroadcastForm({
    recipientOptions,
    isLoadingRecipients,
    isPending,
    error,
    onSend,
    onCancel,
}: Readonly<{
    recipientOptions: BroadcastRecipientOption[];
    isLoadingRecipients: boolean;
    isPending: boolean;
    error: string | null;
    onSend: (payload: BroadcastPayload) => void;
    onCancel: () => void;
}>) {
    const [channel, setChannel] = useState<BroadcastChannel>('PORTAL');
    const [selectedRecipients, setSelectedRecipients] = useState<string[]>([]);
    const [title, setTitle] = useState('');
    const [url, setUrl] = useState('');
    const [text, setText] = useState('');
    const [useSystemProxy, setUseSystemProxy] = useState(false);
    const [headers, setHeaders] = useState<{ id: string; key: string; value: string }[]>(() => [newHeaderRow()]);

    const isHttp = channel === 'HTTP';
    const charsLeft = TEXT_MAX - text.length;

    const isValid =
        (isHttp || selectedRecipients.length > 0) &&
        (isHttp ? isValidHttpUrl(url) : title.trim().length > 0) &&
        text.trim().length > 0 &&
        text.length <= TEXT_MAX;

    const handleChannelChange = useCallback((value: string) => {
        setChannel(value as BroadcastChannel);
        setTitle('');
        setUrl('');
    }, []);

    const handleSend = useCallback(() => {
        if (!isValid || isPending) {
            return;
        }

        if (isHttp) {
            const params: Record<string, string> = {};
            for (const header of headers) {
                const key = header.key.trim();
                if (key) {
                    params[key] = header.value;
                }
            }
            onSend({
                channel: 'HTTP',
                text: text.trim(),
                recipient: { url: url.trim() },
                params,
                useSystemProxy,
            });
            return;
        }

        onSend({
            channel,
            title: title.trim(),
            text: text.trim(),
            recipient: {
                role_scope: 'ENVIRONMENT',
                role_value: selectedRecipients,
            },
        });
    }, [isValid, isPending, isHttp, headers, text, url, useSystemProxy, onSend, channel, title, selectedRecipients]);

    return (
        <Card className="border-2 border-primary/25">
            <CardHeader className="pb-2">
                <CardTitle className="text-sm">Compose broadcast</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="flex items-start gap-3 rounded-lg border border-primary/20 bg-primary/5 p-3">
                    <MessageSquareIcon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
                    <p className="text-sm text-muted-foreground">
                        Send a one-way message to specified recipients to inform them of any changes or updates.
                    </p>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="broadcast-channel">
                        Channel <span className="text-destructive">*</span>
                    </Label>
                    <Select value={channel} onValueChange={handleChannelChange}>
                        <SelectTrigger id="broadcast-channel" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {CHANNELS.map(item => (
                                <SelectItem key={item.id} value={item.id}>
                                    {item.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {!isHttp && (
                    <div className="space-y-2">
                        <Label htmlFor="broadcast-recipients">
                            Recipients <span className="text-destructive">*</span>
                        </Label>
                        <RecipientsSelect
                            options={recipientOptions}
                            selected={selectedRecipients}
                            isLoading={isLoadingRecipients}
                            onChange={setSelectedRecipients}
                        />
                    </div>
                )}

                {!isHttp && (
                    <div className="space-y-2">
                        <Label htmlFor="broadcast-title">
                            Title <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="broadcast-title"
                            value={title}
                            onChange={event => setTitle(event.target.value)}
                            placeholder="e.g. Scheduled maintenance — 2 Jun"
                            required
                        />
                    </div>
                )}

                {isHttp && (
                    <>
                        <div className="space-y-2">
                            <p className="text-sm font-medium">HTTP headers</p>
                            <div className="space-y-2">
                                {headers.map(header => (
                                    <div key={header.id} className="flex items-center gap-2">
                                        <Input
                                            value={header.key}
                                            placeholder="Header name"
                                            onChange={event =>
                                                setHeaders(current =>
                                                    current.map(row => (row.id === header.id ? { ...row, key: event.target.value } : row)),
                                                )
                                            }
                                            className="flex-1"
                                            aria-label="Header name"
                                        />
                                        <Input
                                            value={header.value}
                                            placeholder="Value"
                                            onChange={event =>
                                                setHeaders(current =>
                                                    current.map(row =>
                                                        row.id === header.id ? { ...row, value: event.target.value } : row,
                                                    ),
                                                )
                                            }
                                            className="flex-1"
                                            aria-label="Header value"
                                        />
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="ghost"
                                            className="size-8 shrink-0 p-0 text-destructive hover:text-destructive"
                                            aria-label="Remove header"
                                            onClick={() => setHeaders(current => current.filter(row => row.id !== header.id))}
                                        >
                                            <Trash2Icon className="size-3.5" aria-hidden />
                                        </Button>
                                    </div>
                                ))}
                            </div>
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                className="gap-1.5"
                                onClick={() => setHeaders(current => [...current, newHeaderRow()])}
                            >
                                <PlusIcon className="size-3.5" aria-hidden />
                                Add header
                            </Button>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="broadcast-url">
                                URL <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="broadcast-url"
                                type="url"
                                value={url}
                                onChange={event => setUrl(event.target.value)}
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

                <div className="flex flex-col gap-2">
                    <Label htmlFor="broadcast-text">
                        Message <span className="text-destructive">*</span>
                    </Label>
                    <Textarea
                        id="broadcast-text"
                        value={text}
                        onChange={event => setText(event.target.value)}
                        placeholder="Describe the change, maintenance window, or update…"
                        rows={4}
                        maxLength={TEXT_MAX}
                    />
                    <p className={`text-right text-xs ${charsLeft <= 20 ? 'text-destructive' : 'text-muted-foreground'}`}>
                        {charsLeft} / {TEXT_MAX}
                    </p>
                </div>

                {error && (
                    <Alert variant="destructive">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}

                <Separator />

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
