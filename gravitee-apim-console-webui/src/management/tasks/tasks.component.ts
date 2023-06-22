/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { StateService } from '@uirouter/angularjs';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { TaskService } from '../../services-ngx/task.service';
import { PagedResult } from '../../entities/pagedResult';
import { PromotionApprovalTaskData, Task } from '../../entities/task/task';
import { UIRouterState } from '../../ajs-upgraded-providers';
import { PromotionService } from '../../services-ngx/promotion.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { Workflow } from '../../entities/workflow/workflow';

class TaskData {
  icon: string;
  title: string;
  message: string;
  details: string;
  action: string;
  createdAt: number;
  type: string;
  data: unknown;
}
@Component({
  selector: 'tasks',
  template: require('./tasks.component.html'),
  styles: [require('./tasks.component.scss')],
})
export class TasksComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject();
  tasks: PagedResult<Task>;
  data: TaskData[];
  loading = false;

  constructor(
    private readonly taskService: TaskService,
    private readonly promotionService: PromotionService,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.taskService
      .getTasks()
      .pipe(
        map((result) => {
          this.tasks = result;
          this.data = result.data
            .map((task) => {
              const data: TaskData = {
                icon: this.getIcon(task),
                title: this.getTitle(task),
                message: this.getMessage(task),
                action: this.getActionLabel(task),
                details: this.getDetails(task),
                createdAt: task.created_at,
                type: task.type,
                data: task.data,
              };
              return data;
            })
            .sort((task1, task2) => task2.createdAt - task1.createdAt);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => (this.loading = false));
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  go(task: TaskData): void {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL': {
        const { api, id } = task.data as any;
        this.ajsState.go('management.apis.detail.portal.subscriptions.subscription', {
          apiId: api,
          subscriptionId: id,
        });
        break;
      }
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES': {
        const { referenceId } = task.data as Workflow;
        this.ajsState.go('management.apis.detail.portal.general', { apiId: referenceId });
        break;
      }
      case 'USER_REGISTRATION_APPROVAL': {
        const { id } = task.data as any;
        this.ajsState.go('organization.user-edit', { userId: id });
        break;
      }
    }
  }

  openRejectDialog(task: TaskData) {
    const { promotionId } = task.data as PromotionApprovalTaskData;
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Reject Promotion Request',
          content: `After having rejected this promotion you will not be able to accept it without asking the author to create a new promotion`,
          confirmButton: 'Reject',
        },
        role: 'alertdialog',
        id: 'rejectPromotionConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.promotionService.processPromotion(promotionId, false)),
        tap(() => this.snackBarService.success(`API promotion rejected`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({ next: () => this.removeTask(task), error: ({ error }) => this.snackBarService.error(error.message) });
  }

  openAcceptDialog(task: TaskData) {
    const { isApiUpdate, apiName, targetEnvironmentName, promotionId } = task.data as PromotionApprovalTaskData;

    const message = isApiUpdate
      ? `Since the API <code>${apiName}</code> has already been promoted to <strong>${targetEnvironmentName}</strong> environment, accepting this promotion will update it.`
      : `Accepting this promotion will create a new API in <strong>${targetEnvironmentName}</strong> environment.`;

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Accept Promotion Request',
          content: message,
          confirmButton: 'Accept',
        },
        role: 'alertdialog',
        id: 'acceptPromotionConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.promotionService.processPromotion(promotionId, true)),
        tap(() => this.snackBarService.success(`API promotion accepted`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({ next: () => this.removeTask(task), error: ({ error }) => this.snackBarService.error(error.message) });
  }

  private getMessage(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL': {
        const appName = this.tasks.metadata[task.data.application].name;
        const planName = this.tasks.metadata[task.data.plan].name;
        const apiId = this.tasks.metadata[task.data.plan].api;
        const apiName = this.tasks.metadata[apiId].name;
        return `The application <code>${appName}</code> requested a subscription for API <code>${apiName}</code> (plan: ${planName})`;
      }
      case 'IN_REVIEW':
        return `The API <code>${this.tasks.metadata[task.data.referenceId].name}</code> is ready to be reviewed`;
      case 'REQUEST_FOR_CHANGES': {
        let message = `The API <code>${
          this.tasks.metadata[task.data.referenceId].name
        }</code> has been reviewed and some changes are requested by the reviewer`;
        if (task.data.comment) {
          message += ': ' + task.data.comment;
        }
        return message;
      }
      case 'USER_REGISTRATION_APPROVAL':
        return `The registration of the user <strong>${task.data.displayName}</strong> has to be validated`;
      case 'PROMOTION_APPROVAL':
        return `<strong>${task.data.authorDisplayName}</strong> requested the promotion of API
      <code>${task.data.apiName}</code> from environment <strong>${task.data.sourceEnvironmentName}</strong> to
      environment <strong>${task.data.targetEnvironmentName}</strong>`;
      default:
        return '';
    }
  }

  private getDetails(task: Task): string {
    if (task.type === 'PROMOTION_APPROVAL') {
      return task.data.isApiUpdate
        ? 'Since the API has already been promoted to this environment, accepting this promotion will update the existing API.'
        : 'Accepting this promotion will create a new API in the specified environment.';
    }
    return '';
  }

  private getTitle(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        return 'Subscription';
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES':
        return 'API review';
      case 'USER_REGISTRATION_APPROVAL':
        return 'User registration';
      case 'PROMOTION_APPROVAL':
        return 'API promotion request';
    }
  }

  private getActionLabel(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        return 'Validate';
      case 'IN_REVIEW':
        return 'Review';
      case 'REQUEST_FOR_CHANGES':
        return 'Make changes';
      case 'USER_REGISTRATION_APPROVAL':
        return 'Validate';
      default:
        return 'Details';
    }
  }

  private getIcon(task: Task): string {
    switch (task.type) {
      case 'SUBSCRIPTION_APPROVAL':
        return 'gio:key';
      case 'IN_REVIEW':
      case 'REQUEST_FOR_CHANGES':
        return 'gio:eye-empty';
      case 'USER_REGISTRATION_APPROVAL':
        return 'gio:user-check';
      case 'PROMOTION_APPROVAL':
        return 'gio:upload-cloud';
      default:
        return '';
    }
  }

  private removeTask(taskToRemove: TaskData): void {
    this.data = this.data.filter((task) => task !== taskToRemove);
  }
}
