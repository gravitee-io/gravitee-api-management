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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Checkbox,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
    cn,
} from '@gravitee/graphene-core';
import { ArrowRightIcon, ChevronDownIcon, ChevronUpIcon, CircleCheckIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import { type ComponentType, useState } from 'react';
import { Link } from 'react-router-dom';

import { CircularProgress } from './CircularProgress';

export interface OverviewChecklistItem {
    readonly actionLabel: string;
    readonly done: boolean;
    readonly icon: ComponentType<{ className?: string }>;
    readonly id: string;
    readonly label: string;
    readonly to: string;
    readonly tooltip: string;
}

interface OverviewChecklistCardProps {
    readonly description: string;
    readonly isReady?: boolean;
    readonly items: OverviewChecklistItem[];
    readonly totalCountHint?: number;
    /** Called when user clicks a checklist row to toggle completion. */
    readonly onToggle?: (id: string, newDone: boolean) => void;
}

export function OverviewChecklistCard({
    description,
    isReady = true,
    items,
    onToggle,
    totalCountHint,
}: Readonly<OverviewChecklistCardProps>) {
    const [open, setOpen] = useState(true);
    const completedCount = items.filter(item => item.done).length;
    const totalCount = totalCountHint ?? items.length;
    const completionPct = totalCount > 0 ? Math.min(100, Math.round((completedCount / totalCount) * 100)) : 0;

    return (
        <Card>
            <CardHeader className="pb-0">
                <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                        <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10">
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
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8 text-muted-foreground"
                            onClick={() => setOpen(current => !current)}
                            aria-label={open ? 'Collapse checklist' : 'Expand checklist'}
                        >
                            {open ? <ChevronUpIcon className="size-5" aria-hidden /> : <ChevronDownIcon className="size-5" aria-hidden />}
                        </Button>
                    </div>
                </div>
            </CardHeader>

            {open ? (
                <CardContent className="pt-4">
                    <div className="flex flex-row gap-8">
                        <div className="min-w-0 flex-1 space-y-1">
                            <TooltipProvider delayDuration={200}>
                                {items.map(item => {
                                    const ItemIcon = item.icon;
                                    const isToggleable = Boolean(onToggle);

                                    return (
                                        <div
                                            key={item.id}
                                            className={cn('flex items-center gap-3 rounded-lg px-3 py-2.5 transition-colors', {
                                                'cursor-pointer hover:bg-accent/50': isToggleable,
                                                'opacity-60': item.done,
                                            })}
                                            onClick={isToggleable ? () => onToggle!(item.id, !item.done) : undefined}
                                            role={isToggleable ? 'checkbox' : undefined}
                                            aria-checked={isToggleable ? item.done : undefined}
                                            aria-label={isToggleable ? item.label : undefined}
                                            tabIndex={isToggleable ? 0 : undefined}
                                            onKeyDown={
                                                isToggleable
                                                    ? event => {
                                                          if (event.key === 'Enter' || event.key === ' ') {
                                                              event.preventDefault();
                                                              onToggle!(item.id, !item.done);
                                                          }
                                                      }
                                                    : undefined
                                            }
                                        >
                                            <div className="shrink-0">
                                                {item.done ? (
                                                    <CircleCheckIcon className="size-4 text-success" aria-hidden />
                                                ) : (
                                                    <Checkbox
                                                        checked={false}
                                                        className="pointer-events-none size-4"
                                                        aria-hidden
                                                        tabIndex={-1}
                                                    />
                                                )}
                                            </div>
                                            <ItemIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                            <span
                                                className={cn('min-w-0 flex-1 text-sm', {
                                                    'line-through text-muted-foreground': item.done,
                                                    'text-foreground': !item.done,
                                                })}
                                            >
                                                {item.label}
                                            </span>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-7 shrink-0 text-muted-foreground/60"
                                                        aria-label={`Info: ${item.label}`}
                                                        onClick={event => event.stopPropagation()}
                                                    >
                                                        <InfoIcon className="size-3.5" aria-hidden />
                                                    </Button>
                                                </TooltipTrigger>
                                                <TooltipContent side="top" className="max-w-xs text-xs">
                                                    {item.tooltip}
                                                </TooltipContent>
                                            </Tooltip>
                                            <Button asChild variant="link" size="sm" className="h-auto shrink-0 px-1.5 py-1 text-primary">
                                                <Link to={item.to} aria-label={item.actionLabel} onClick={event => event.stopPropagation()}>
                                                    {item.actionLabel}
                                                    <ArrowRightIcon className="size-4 shrink-0" aria-hidden />
                                                </Link>
                                            </Button>
                                        </div>
                                    );
                                })}
                            </TooltipProvider>
                        </div>

                        <div className="flex shrink-0 flex-col items-center justify-center gap-2 border-l pl-8 pr-2">
                            <CircularProgress pct={completionPct} />
                            <span className="text-center text-xs text-muted-foreground">Completion</span>
                        </div>
                    </div>
                </CardContent>
            ) : null}
        </Card>
    );
}
