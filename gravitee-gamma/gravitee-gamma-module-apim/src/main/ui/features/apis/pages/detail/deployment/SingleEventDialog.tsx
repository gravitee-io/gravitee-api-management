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
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { extractDefinition, formatDate } from './utils';
import type { ApiEvent } from '../../../types/api';

type Props = Readonly<{
    event: ApiEvent;
    onRollback: (eventId: string) => Promise<void>;
    onClose: () => void;
    isRollingBack: boolean;
}>;

export function SingleEventDialog({ event, onRollback, onClose, isRollingBack }: Props) {
    const version = event.properties.DEPLOYMENT_NUMBER ?? '—';
    const definition = useMemo(() => extractDefinition(event), [event]);

    return (
        <Dialog open onOpenChange={open => !open && onClose()}>
            <DialogContent
                className="flex flex-col overflow-hidden p-0 gap-0"
                style={{ width: 'min(90vw, 42rem)', maxWidth: 'min(90vw, 42rem)', maxHeight: 'min(85vh, 720px)' }}
                showCloseButton={false}
            >
                <DialogHeader className="flex-none flex-row items-start justify-between border-b px-6 py-4 gap-3">
                    <div>
                        <DialogTitle>Version {version}</DialogTitle>
                        <DialogDescription className="mt-0.5">
                            {formatDate(event.createdAt)} &nbsp;·&nbsp; {event.initiator.displayName}
                            {event.properties.DEPLOYMENT_LABEL ? <> &nbsp;·&nbsp; {event.properties.DEPLOYMENT_LABEL}</> : null}
                        </DialogDescription>
                    </div>
                    <DialogClose asChild>
                        <Button variant="ghost" size="icon" className="size-8 shrink-0 mt-0.5">
                            <XIcon className="size-4" />
                        </Button>
                    </DialogClose>
                </DialogHeader>

                <div className="flex-1 min-h-0 overflow-y-auto px-6 py-5">
                    <p className="text-xs font-medium text-muted-foreground mb-2">API definition</p>
                    <pre className="rounded-lg bg-muted px-4 py-3 text-xs font-mono leading-relaxed overflow-x-auto whitespace-pre-wrap break-all">
                        {definition}
                    </pre>
                </div>

                <DialogFooter className="flex-none flex-row justify-end border-t px-6 pt-3 pb-5 gap-3">
                    <DialogClose asChild>
                        <Button variant="outline" size="sm">
                            Close
                        </Button>
                    </DialogClose>
                    <Button size="sm" disabled={isRollingBack} onClick={() => onRollback(event.id)}>
                        {isRollingBack ? 'Rolling back…' : `Rollback to v${version}`}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
