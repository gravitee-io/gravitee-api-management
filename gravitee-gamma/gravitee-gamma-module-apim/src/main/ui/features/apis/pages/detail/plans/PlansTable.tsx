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
    Badge,
    Button,
    DataTablePagination,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { ArrowDownIcon, ArrowUpIcon, CircleXIcon, EyeIcon, GlobeIcon, PencilIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { PlanStatusBadge } from './PlanStatusBadge';
import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import { usePlanTransition, useReorderPlan } from '../../../hooks/usePlans';
import type { ManagedPlan, PlanContext } from '../../../types/plan';
import { PLAN_SECURITY_LABELS } from '../../../types/plan';

type PlanTransitionDialogAction = 'publish' | 'deprecate' | 'close';

const PLAN_ACTION_CONFIG: Record<
    PlanTransitionDialogAction,
    { title: string; body: string; confirmLabel: string; destructive: boolean; success: string }
> = {
    publish: {
        title: 'Publish plan?',
        body: 'This will make the plan available for subscriptions.',
        confirmLabel: 'Publish',
        destructive: false,
        success: 'Plan published',
    },
    deprecate: {
        title: 'Deprecate plan?',
        body: 'Existing subscriptions continue. No new subscriptions will be accepted.',
        confirmLabel: 'Deprecate',
        destructive: false,
        success: 'Plan deprecated',
    },
    close: {
        title: 'Close plan?',
        body: 'All active subscriptions will be closed. This cannot be undone.',
        confirmLabel: 'Close',
        destructive: true,
        success: 'Plan closed',
    },
};

interface PlansTableProps {
    ctx: PlanContext;
    plans: ManagedPlan[];
    totalCount: number;
    page: number;
    perPage: number;
    isLoading: boolean;
    canUpdate: boolean;
    onPage: (p: number) => void;
    onPerPage: (n: number) => void;
}

interface PendingAction {
    planId: string;
    plan: ManagedPlan;
    action: PlanTransitionDialogAction;
}

export function PlansTable({ ctx, plans, totalCount, page, perPage, isLoading, canUpdate, onPage, onPerPage }: Readonly<PlansTableProps>) {
    const navigate = useNavigate();
    const [pending, setPending] = useState<PendingAction | null>(null);

    const transitionMutation = usePlanTransition(ctx);
    const reorderMutation = useReorderPlan(ctx);

    const isMutating = transitionMutation.isPending;

    const openDialog = useCallback((plan: ManagedPlan, action: PlanTransitionDialogAction) => {
        setPending({ planId: plan.id, plan, action });
    }, []);

    const handleConfirm = useCallback(() => {
        if (!pending) return;
        const { success } = PLAN_ACTION_CONFIG[pending.action];
        transitionMutation.mutate(
            { planId: pending.planId, action: pending.action },
            {
                onSuccess: () => {
                    notify.success(success);
                    setPending(null);
                },
                onError: error => notify.error(error, 'Failed to update plan.'),
            },
        );
    }, [pending, transitionMutation]);

    const handleReorder = useCallback(
        (idx: number, plan: ManagedPlan, direction: 'up' | 'down') => {
            if (direction === 'up' && idx === 0) return;
            if (direction === 'down' && idx === plans.length - 1) return;
            // Use 1-based target position (matching legacy console behaviour) so the
            // backend can renumber correctly even when order values have gaps.
            const newOrder = direction === 'up' ? idx : idx + 2;
            reorderMutation.mutate(
                { planId: plan.id, fullPlan: plan, newOrder },
                { onError: error => notify.error(error, 'Failed to reorder plan.') },
            );
        },
        [plans, reorderMutation],
    );

    const pagination = (
        <DataTablePagination
            page={page}
            pageSize={perPage}
            totalCount={totalCount}
            pageSizeOptions={[10, 25, 50, 100]}
            onPageChange={onPage}
            onPageSizeChange={onPerPage}
        />
    );

    return (
        <div className="space-y-2">
            {pagination}
            <div className="rounded-md border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            {canUpdate && <TableHead className="w-16" />}
                            <TableHead>Name</TableHead>
                            <TableHead>Security</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Validation</TableHead>
                            <TableHead />
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {isLoading &&
                            Array.from({ length: perPage }).map((_, i) => (
                                <TableRow key={i}>
                                    {Array.from({ length: canUpdate ? 6 : 5 }).map((__, j) => (
                                        <TableCell key={j}>
                                            <Skeleton className="h-4 w-full rounded" />
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))}

                        {!isLoading && plans.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={canUpdate ? 6 : 5} className="py-10 text-center text-sm text-muted-foreground">
                                    No plans match the selected status.
                                </TableCell>
                            </TableRow>
                        )}

                        {!isLoading &&
                            plans.map((plan, idx) => (
                                <TableRow key={plan.id}>
                                    {canUpdate && (
                                        <TableCell>
                                            <div className="flex items-center gap-0.5">
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-7"
                                                            disabled={idx === 0 || reorderMutation.isPending}
                                                            onClick={() => handleReorder(idx, plan, 'up')}
                                                        >
                                                            <ArrowUpIcon className="size-3.5" aria-hidden />
                                                            <span className="sr-only">Move up</span>
                                                        </Button>
                                                    </TooltipTrigger>
                                                    <TooltipContent>Move up</TooltipContent>
                                                </Tooltip>
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-7"
                                                            disabled={idx === plans.length - 1 || reorderMutation.isPending}
                                                            onClick={() => handleReorder(idx, plan, 'down')}
                                                        >
                                                            <ArrowDownIcon className="size-3.5" aria-hidden />
                                                            <span className="sr-only">Move down</span>
                                                        </Button>
                                                    </TooltipTrigger>
                                                    <TooltipContent>Move down</TooltipContent>
                                                </Tooltip>
                                            </div>
                                        </TableCell>
                                    )}
                                    <TableCell>
                                        <p className="font-medium">{plan.name}</p>
                                        {plan.description && (
                                            <p className="text-xs text-muted-foreground line-clamp-1">{plan.description}</p>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <Badge variant="secondary" className="text-xs font-mono">
                                            {PLAN_SECURITY_LABELS[plan.security.type]}
                                        </Badge>
                                    </TableCell>
                                    <TableCell>
                                        <PlanStatusBadge status={plan.status} />
                                    </TableCell>
                                    <TableCell className="text-sm text-muted-foreground">
                                        {plan.validation === 'AUTO' ? 'Auto' : 'Manual'}
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex items-center gap-1 justify-end">
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-7"
                                                        onClick={() => navigate(plan.id)}
                                                    >
                                                        {canUpdate && plan.status !== 'CLOSED' ? (
                                                            <PencilIcon className="size-3.5" aria-hidden />
                                                        ) : (
                                                            <EyeIcon className="size-3.5" aria-hidden />
                                                        )}
                                                        <span className="sr-only">
                                                            {canUpdate && plan.status !== 'CLOSED' ? 'Edit' : 'View'} plan
                                                        </span>
                                                    </Button>
                                                </TooltipTrigger>
                                                <TooltipContent>
                                                    {canUpdate && plan.status !== 'CLOSED' ? 'Edit plan' : 'View plan'}
                                                </TooltipContent>
                                            </Tooltip>

                                            {canUpdate && plan.status === 'STAGING' && (
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-7 text-success hover:text-success"
                                                            onClick={() => openDialog(plan, 'publish')}
                                                        >
                                                            <GlobeIcon className="size-3.5" aria-hidden />
                                                            <span className="sr-only">Publish</span>
                                                        </Button>
                                                    </TooltipTrigger>
                                                    <TooltipContent>Publish plan</TooltipContent>
                                                </Tooltip>
                                            )}

                                            {canUpdate && plan.status === 'PUBLISHED' && (
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-7 text-warning hover:text-warning"
                                                            onClick={() => openDialog(plan, 'deprecate')}
                                                        >
                                                            <TriangleAlertIcon className="size-3.5" aria-hidden />
                                                            <span className="sr-only">Deprecate</span>
                                                        </Button>
                                                    </TooltipTrigger>
                                                    <TooltipContent>Deprecate plan</TooltipContent>
                                                </Tooltip>
                                            )}

                                            {canUpdate && plan.status !== 'CLOSED' && (
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-7 text-destructive hover:text-destructive"
                                                            onClick={() => openDialog(plan, 'close')}
                                                        >
                                                            <CircleXIcon className="size-3.5" aria-hidden />
                                                            <span className="sr-only">Close</span>
                                                        </Button>
                                                    </TooltipTrigger>
                                                    <TooltipContent>Close plan</TooltipContent>
                                                </Tooltip>
                                            )}
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                    </TableBody>
                </Table>
            </div>

            {pagination}

            {pending && (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && setPending(null)}
                    title={PLAN_ACTION_CONFIG[pending.action].title}
                    description={PLAN_ACTION_CONFIG[pending.action].body}
                    confirmLabel={PLAN_ACTION_CONFIG[pending.action].confirmLabel}
                    destructive={PLAN_ACTION_CONFIG[pending.action].destructive}
                    isPending={isMutating}
                    onConfirm={handleConfirm}
                />
            )}
        </div>
    );
}
