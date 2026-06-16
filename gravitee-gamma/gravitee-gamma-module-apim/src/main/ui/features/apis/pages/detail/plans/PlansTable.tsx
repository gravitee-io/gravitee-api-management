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
    DataTable,
    DataTableEmptyState,
    type DataTableProps,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import {
    ArrowDownIcon,
    ArrowUpIcon,
    CircleXIcon,
    GlobeIcon,
    MoreVerticalIcon,
    SearchIcon,
    TriangleAlertIcon,
} from '@gravitee/graphene-core/icons';
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

type Cell<T> = { row: { index: number; original: T } };

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

    const columns: DataTableProps<ManagedPlan>['columns'] = [
        ...(canUpdate
            ? [
                  {
                      id: 'reorder',
                      header: () => <span className="sr-only">Reorder</span>,
                      enableSorting: false,
                      enableHiding: false,
                      size: 72,
                      cell: ({ row }: Cell<ManagedPlan>) => {
                          const idx = row.index;
                          const plan = row.original;
                          return (
                              <div className="flex items-center gap-0.5">
                                  <Button
                                      type="button"
                                      variant="ghost"
                                      size="icon"
                                      className="size-7"
                                      disabled={idx === 0 || reorderMutation.isPending}
                                      onClick={() => handleReorder(idx, plan, 'up')}
                                      title="Move up"
                                      aria-label="Move up"
                                  >
                                      <ArrowUpIcon className="size-3.5" aria-hidden />
                                  </Button>
                                  <Button
                                      type="button"
                                      variant="ghost"
                                      size="icon"
                                      className="size-7"
                                      disabled={idx === plans.length - 1 || reorderMutation.isPending}
                                      onClick={() => handleReorder(idx, plan, 'down')}
                                      title="Move down"
                                      aria-label="Move down"
                                  >
                                      <ArrowDownIcon className="size-3.5" aria-hidden />
                                  </Button>
                              </div>
                          );
                      },
                  },
              ]
            : []),
        {
            id: 'Name',
            accessorFn: (row: ManagedPlan) => row.name,
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: Cell<ManagedPlan>) => {
                const plan = row.original;
                return (
                    <div>
                        <button type="button" className="text-left font-medium hover:underline" onClick={() => navigate(plan.id)}>
                            {plan.name}
                        </button>
                        {plan.description ? <p className="text-xs text-muted-foreground line-clamp-1">{plan.description}</p> : null}
                    </div>
                );
            },
        },
        {
            id: 'Security',
            header: 'Security',
            enableSorting: false,
            cell: ({ row }: Cell<ManagedPlan>) => (
                <Badge variant="secondary" className="text-xs font-mono">
                    {PLAN_SECURITY_LABELS[row.original.security.type]}
                </Badge>
            ),
        },
        {
            id: 'Status',
            header: 'Status',
            enableSorting: false,
            cell: ({ row }: Cell<ManagedPlan>) => <PlanStatusBadge status={row.original.status} />,
        },
        {
            id: 'Validation',
            accessorFn: (row: ManagedPlan) => row.validation,
            header: 'Validation',
            enableSorting: false,
            cell: ({ row }: Cell<ManagedPlan>) => (
                <span className="text-sm text-muted-foreground">{row.original.validation === 'AUTO' ? 'Auto' : 'Manual'}</span>
            ),
        },
        ...(canUpdate
            ? [
                  {
                      id: 'actions',
                      header: () => <span className="sr-only">Actions</span>,
                      enableSorting: false,
                      enableHiding: false,
                      cell: ({ row }: Cell<ManagedPlan>) => {
                          const plan = row.original;
                          if (plan.status === 'CLOSED') return null;
                          const showPublish = plan.status === 'STAGING';
                          const showDeprecate = plan.status === 'PUBLISHED';
                          if (!showPublish && !showDeprecate) {
                              return (
                                  <div className="flex justify-end">
                                      <Button
                                          variant="ghost"
                                          size="icon"
                                          className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                          aria-label={`Close ${plan.name}`}
                                          title="Close plan"
                                          onClick={() => openDialog(plan, 'close')}
                                      >
                                          <CircleXIcon className="size-4" aria-hidden />
                                      </Button>
                                  </div>
                              );
                          }
                          return (
                              <div className="flex justify-end">
                                  <DropdownMenu>
                                      <DropdownMenuTrigger asChild>
                                          <Button variant="ghost" size="icon" aria-label={`Actions for ${plan.name}`}>
                                              <MoreVerticalIcon className="size-4" aria-hidden />
                                          </Button>
                                      </DropdownMenuTrigger>
                                      <DropdownMenuContent align="end" className="w-auto min-w-48">
                                          {showPublish ? (
                                              <DropdownMenuItem onSelect={() => openDialog(plan, 'publish')}>
                                                  <GlobeIcon className="size-3.5" />
                                                  Publish
                                              </DropdownMenuItem>
                                          ) : null}
                                          {showDeprecate ? (
                                              <DropdownMenuItem onSelect={() => openDialog(plan, 'deprecate')}>
                                                  <TriangleAlertIcon className="size-3.5" />
                                                  Deprecate
                                              </DropdownMenuItem>
                                          ) : null}
                                          {showPublish || showDeprecate ? <DropdownMenuSeparator /> : null}
                                          <DropdownMenuItem className="text-destructive" onSelect={() => openDialog(plan, 'close')}>
                                              <CircleXIcon className="size-3.5" />
                                              Close
                                          </DropdownMenuItem>
                                      </DropdownMenuContent>
                                  </DropdownMenu>
                              </div>
                          );
                      },
                  },
              ]
            : []),
    ];

    return (
        <>
            <DataTable
                aria-label="Plans"
                columns={columns}
                data={plans}
                loading={isLoading}
                skeletonCount={perPage}
                serverSide
                pagination={{
                    page,
                    pageSize: perPage,
                    totalCount,
                    pageSizeOptions: [10, 25, 50, 100],
                    onPageChange: onPage,
                    onPageSizeChange: onPerPage,
                }}
                emptyMessage={
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<SearchIcon />}
                        title="No plans match the selected status"
                        description="Try selecting a different status filter."
                    />
                }
            />

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
        </>
    );
}
