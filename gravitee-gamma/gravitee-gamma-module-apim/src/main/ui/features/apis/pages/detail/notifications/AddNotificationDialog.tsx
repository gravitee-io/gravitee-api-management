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
    Button,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react';

import type { ApiNotifier } from '../../../types/notification';
import { CHANNEL_ICON } from '../../../utils/notificationFormatters';

// ─── Channel options ──────────────────────────────────────────────────────────

interface ChannelOption {
    notifierId: string;
    label: string;
    type: 'CONSOLE' | 'EMAIL' | 'WEBHOOK';
}

/** Derives channel options from available notifiers; CONSOLE is always added first. */
function buildChannelOptions(notifiers: ApiNotifier[]): ChannelOption[] {
    const options: ChannelOption[] = [{ notifierId: '__PORTAL__', label: 'Console', type: 'CONSOLE' }];
    for (const n of notifiers) {
        if (n.type === 'EMAIL') {
            options.push({ notifierId: n.id, label: n.name || 'Email', type: 'EMAIL' });
        } else if (n.type === 'WEBHOOK') {
            options.push({ notifierId: n.id, label: n.name || 'Webhook', type: 'WEBHOOK' });
        }
    }
    return options;
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface AddNotificationDialogProps {
    open: boolean;
    notifiers: ApiNotifier[];
    isPending: boolean;
    onClose: () => void;
    onAdd: (name: string, notifierId: string) => void;
}

// ─── Component ────────────────────────────────────────────────────────────────

export function AddNotificationDialog({ open, notifiers, isPending, onClose, onAdd }: Readonly<AddNotificationDialogProps>) {
    const [name, setName] = useState('');
    const [selectedNotifierId, setSelectedNotifierId] = useState('__PORTAL__');

    const channelOptions = useMemo(() => buildChannelOptions(notifiers), [notifiers]);

    // Reset state each time the dialog opens
    useEffect(() => {
        if (open) {
            setName('');
            setSelectedNotifierId('__PORTAL__');
        }
    }, [open]);

    const isValid = name.trim().length > 0;

    const handleSubmit = useCallback(
        (e: FormEvent) => {
            e.preventDefault();
            if (!isValid || isPending) return;
            onAdd(name.trim(), selectedNotifierId);
        },
        [isValid, isPending, onAdd, name, selectedNotifierId],
    );

    return (
        <Dialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) onClose();
            }}
        >
            <DialogContent style={{ maxWidth: '28rem' }}>
                <DialogHeader>
                    <DialogTitle>Add notification</DialogTitle>
                </DialogHeader>

                <form onSubmit={handleSubmit} className="space-y-4 pt-2">
                    <div className="space-y-2">
                        <Label htmlFor="notif-name">Name</Label>
                        <Input
                            id="notif-name"
                            value={name}
                            onChange={e => setName(e.target.value)}
                            placeholder="e.g. Ops webhook"
                            required
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="notif-channel">Channel</Label>
                        <Select value={selectedNotifierId} onValueChange={setSelectedNotifierId}>
                            <SelectTrigger id="notif-channel" className="w-full">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {channelOptions.map(opt => {
                                    const Icon = CHANNEL_ICON[opt.type];
                                    return (
                                        <SelectItem key={opt.notifierId} value={opt.notifierId}>
                                            <span className="flex items-center gap-2">
                                                <Icon className="size-3.5" />
                                                {opt.label}
                                            </span>
                                        </SelectItem>
                                    );
                                })}
                            </SelectContent>
                        </Select>
                    </div>

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!isValid || isPending}>
                            {isPending ? 'Adding…' : 'Add'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
