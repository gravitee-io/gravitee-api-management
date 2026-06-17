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
import { Button } from '@gravitee/graphene-core';
import { ClipboardCheck } from 'lucide-react';
import { useState } from 'react';

import { TasksSheet } from './TasksSheet';
import { usePendingTaskCount } from '../useTasks';

export function PendingTasksBadge() {
    const [open, setOpen] = useState(false);
    const count = usePendingTaskCount();
    const hasPending = count !== null && count > 0;

    return (
        <>
            <Button
                variant="ghost"
                size="icon-sm"
                className="relative"
                onClick={() => setOpen(true)}
                aria-label={hasPending ? `Tasks and approvals, ${count} pending` : 'Tasks and approvals'}
            >
                <ClipboardCheck className="size-4" aria-hidden />
                {hasPending && (
                    <span className="absolute -right-0.5 -top-0.5 flex min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold leading-4 text-primary-foreground">
                        {count}
                    </span>
                )}
            </Button>
            <TasksSheet open={open} onOpenChange={setOpen} />
        </>
    );
}
