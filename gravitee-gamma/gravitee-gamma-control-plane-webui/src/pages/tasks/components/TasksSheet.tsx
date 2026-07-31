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
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@gravitee/graphene-core';

import { TasksBody } from './TasksBody';
import { useTasks } from '../useTasks';

function TasksSheetContent({ onClose }: { onClose: () => void }) {
    const { tasks, loading, error, reload } = useTasks();
    return (
        <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-4">
            <TasksBody tasks={tasks} loading={loading} error={error} reload={reload} compact onNavigate={onClose} />
        </div>
    );
}

export function TasksSheet({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" style={{ maxWidth: '40rem' }}>
                <SheetHeader>
                    <SheetTitle>Tasks &amp; Approvals</SheetTitle>
                    <SheetDescription>Review pending tasks and approvals across your Gravitee products.</SheetDescription>
                </SheetHeader>
                <TasksSheetContent onClose={() => onOpenChange(false)} />
            </SheetContent>
        </Sheet>
    );
}
