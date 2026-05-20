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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { ArrowRightIcon, ChevronDownIcon, ChevronUpIcon, CircleCheckIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import { type ComponentType, useState } from 'react';
import { Link } from 'react-router-dom';

import { CircularProgress } from './CircularProgress';

export interface OverviewChecklistItem {
    id: string;
    label: string;
    tooltip: string;
    /** When absent, renders a "Coming soon" badge instead of a link. */
    to?: string;
    icon: ComponentType<{ className?: string }>;
    actionLabel: string;
    done: boolean;
    comingSoon?: boolean;
}

interface OverviewChecklistCardProps {
    description: string;
    items: OverviewChecklistItem[];
    /** When false, counts display as '…' (data still loading). */
    isReady?: boolean;
    /** Total item count to show while loading. Defaults to items.length. */
    totalCountHint?: number;
}

export function OverviewChecklistCard({ description, items, isReady = true, totalCountHint }: Readonly<OverviewChecklistCardProps>) {
    const [open, setOpen] = useState(true);

    const completedCount = items.filter(i => i.done).length;
    const totalCount = totalCountHint ?? items.length;
    const completionPct = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

    return (
        <Card>
            <CardHeader className="pb-0">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div
                            className="rounded-lg p-2"
                            style={{ backgroundColor: 'color-mix(in oklab, var(--color-primary) 10%, transparent)' }}
                        >
                            <CircleCheckIcon className="size-4 text-primary" aria-hidden />
                        </div>
                        <div>
                            <CardTitle className="text-base">Checklist</CardTitle>
                            <CardDescription className="mt-0.5">{description}</CardDescription>
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                        <span className="text-sm font-semibold text-muted-foreground">
                            {isReady ? completedCount : '…'}/{totalCount}
                        </span>
                        <button
                            type="button"
                            onClick={() => setOpen(o => !o)}
                            className="text-muted-foreground hover:text-foreground transition-colors"
                            aria-label={open ? 'Collapse checklist' : 'Expand checklist'}
                        >
                            {open ? <ChevronUpIcon className="size-5" aria-hidden /> : <ChevronDownIcon className="size-5" aria-hidden />}
                        </button>
                    </div>
                </div>
            </CardHeader>

            {open ? (
                <CardContent className="pt-4">
                    <div className="flex flex-row gap-8">
                        <div className="flex-1 space-y-1 min-w-0">
                            <TooltipProvider delayDuration={200}>
                                {items.map(item => {
                                    const ItemIcon = item.icon;
                                    return (
                                        <div
                                            key={item.id}
                                            className={`flex items-center gap-3 rounded-lg px-3 py-2.5 transition-colors${
                                                item.done ? ' opacity-60' : item.comingSoon ? '' : ' hover:bg-accent/50'
                                            }`}
                                        >
                                            <div className="shrink-0">
                                                {item.done ? (
                                                    <CircleCheckIcon className="size-4 text-success" aria-hidden />
                                                ) : (
                                                    <div
                                                        className="size-4 rounded"
                                                        style={{
                                                            border: '2px solid color-mix(in oklab, var(--color-muted-foreground) 30%, transparent)',
                                                        }}
                                                    />
                                                )}
                                            </div>
                                            <ItemIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                            <span
                                                className={`text-sm flex-1 min-w-0${item.done ? ' text-muted-foreground' : ' text-foreground'}`}
                                                style={item.done ? { textDecoration: 'line-through' } : undefined}
                                            >
                                                {item.label}
                                            </span>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <button
                                                        type="button"
                                                        className="text-muted-foreground/40 hover:text-muted-foreground transition-colors shrink-0"
                                                        aria-label={`Info: ${item.label}`}
                                                    >
                                                        <InfoIcon className="size-3.5" aria-hidden />
                                                    </button>
                                                </TooltipTrigger>
                                                <TooltipContent side="top" className="max-w-xs text-xs">
                                                    {item.tooltip}
                                                </TooltipContent>
                                            </Tooltip>
                                            {item.comingSoon ? (
                                                <span className="shrink-0 text-xs text-muted-foreground">Coming soon</span>
                                            ) : (
                                                <Link
                                                    to={item.to ?? '#'}
                                                    className="shrink-0 inline-flex items-center gap-1 rounded-md px-1.5 py-1 text-xs font-medium text-primary transition-colors"
                                                    aria-label={item.actionLabel}
                                                >
                                                    {item.actionLabel}
                                                    <ArrowRightIcon className="size-4 shrink-0" aria-hidden />
                                                </Link>
                                            )}
                                        </div>
                                    );
                                })}
                            </TooltipProvider>
                        </div>

                        <div className="flex flex-col items-center justify-center gap-2 border-l pl-8 pr-2 shrink-0">
                            <CircularProgress pct={completionPct} />
                            <span className="text-xs text-muted-foreground text-center">Completion</span>
                        </div>
                    </div>
                </CardContent>
            ) : null}
        </Card>
    );
}
