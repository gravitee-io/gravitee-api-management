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
import { Badge, Button, Card, CardContent, toast } from '@gravitee/graphene-core';
import { ArrowRightIcon, CloudUploadIcon, EyeIcon, KeyIcon, MessageSquareWarningIcon, UserCheckIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { getModuleLabel, useModulesStore } from '../../../features/modules';
import { formatRelativeTime } from '../tasks.mapping';
import type { TaskAreaKey, TaskIconKey, TaskView } from '../tasks.types';

const ICONS: Record<TaskIconKey, LucideIcon> = {
    subscription: KeyIcon,
    review: EyeIcon,
    changes: MessageSquareWarningIcon,
    registration: UserCheckIcon,
    promotion: CloudUploadIcon,
};

const AREA_CLASS: Record<TaskAreaKey, string> = {
    apim: 'border-primary/30 text-primary',
    mcp: 'border-highlight/30 text-highlight',
    ai: 'border-highlight/30 text-highlight',
    llm: 'border-highlight/30 text-highlight',
    kafka: 'border-warning/30 text-warning',
    users: 'border-success/30 text-success',
};

export function TaskRow({ task, onNavigate }: { task: TaskView; onNavigate?: () => void }) {
    const navigate = useNavigate();
    const modules = useModulesStore(s => s.modules);
    const Icon = ICONS[task.iconKey];

    const hasTarget = Boolean(task.to && task.toModuleId);
    const targetModule = task.toModuleId ? modules.find(m => m.id === task.toModuleId) : undefined;
    const targetLabel = task.toModuleId ? getModuleLabel(task.toModuleId, targetModule?.name) : undefined;

    const handleAction = () => {
        if (!task.to || !task.toModuleId) {
            return;
        }
        if (!targetModule) {
            toast.error(`${targetLabel} is not available in this environment.`);
            return;
        }
        navigate(task.to);
        onNavigate?.();
    };

    return (
        <Card>
            <CardContent className="flex items-start gap-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted">
                    <Icon className="size-4 text-muted-foreground" aria-hidden />
                </div>
                <div className="min-w-0 flex-1 space-y-3">
                    <div className="flex items-start justify-between gap-3">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-sm font-semibold">{task.categoryLabel}</span>
                            <Badge variant="outline" className={`text-xs ${AREA_CLASS[task.area.key]}`}>
                                {task.area.label}
                            </Badge>
                        </div>
                        <span className="shrink-0 text-xs text-muted-foreground">{formatRelativeTime(task.createdAt)}</span>
                    </div>
                    <div className="space-y-0.5">
                        <p className="truncate text-sm text-foreground">{task.title}</p>
                        {task.subtitle && <p className="truncate text-xs text-muted-foreground">{task.subtitle}</p>}
                    </div>
                    {task.comment && (
                        <p className="line-clamp-3 break-words rounded-md bg-muted/50 px-3 py-2 text-xs text-muted-foreground">
                            {task.comment}
                        </p>
                    )}
                    {hasTarget && (
                        <div className="flex items-center gap-3 pt-1">
                            <Button size="sm" onClick={handleAction}>
                                {task.actionLabel}
                                <ArrowRightIcon className="size-3" aria-hidden />
                            </Button>
                            {targetLabel && <span className="text-xs text-muted-foreground">Opens {targetLabel}</span>}
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
