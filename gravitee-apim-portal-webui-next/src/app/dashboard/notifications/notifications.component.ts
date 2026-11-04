/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, EMPTY, finalize, throwError } from 'rxjs';

import { notificationListBreadcrumb } from './notification-breadcrumbs';
import { deriveNotificationTypeLabel } from './notification-type-label';
import { BadgeComponent } from '../../../components/badge/badge.component';
import { DropdownSearchComponent, SelectOption } from '../../../components/dropdown-search/dropdown-search.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginatedTableComponent, TableAction, TableColumn } from '../../../components/paginated-table/paginated-table.component';
import { TableCellDirective } from '../../../components/paginated-table/table-cell.directive';
import { PortalNotification, PortalNotificationReference, PortalNotificationsResponse } from '../../../entities/notification';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { PortalNotificationService } from '../../../services/portal-notification.service';
import { parsePageParam, parseSizeParam } from '../../../utils/common.utils';

type NotificationReadStatus = 'read' | 'unread' | 'all';

interface NotificationFilters {
  apiId: string | null;
  applicationId: string | null;
  status: NotificationReadStatus;
  page: number;
  size: number;
}

interface NotificationRow {
  id: string;
  createdAt: string;
  type: string;
  apiName: string | null;
  applicationName: string | null;
  message: string;
  isRead: boolean;
}

// Unread is the default status: it is the one left out of the URL.
const DEFAULT_STATUS: NotificationReadStatus = 'unread';
const MARK_AS_READ_ACTION_ID = 'read';
const DELETED_API_LABEL = $localize`:@@notificationsDeletedApi:Deleted API`;
const DELETED_APPLICATION_LABEL = $localize`:@@notificationsDeletedApplication:Deleted application`;

@Component({
  selector: 'app-notifications',
  imports: [
    BadgeComponent,
    DatePipe,
    DropdownSearchComponent,
    LoaderComponent,
    MatButton,
    FormsModule,
    PaginatedTableComponent,
    TableCellDirective,
  ],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export default class NotificationsComponent {
  private static readonly DEFAULT_PAGE = 1;
  private static readonly DEFAULT_PAGE_SIZE = 10;
  private static readonly MAX_PAGE_SIZE = 100;

  private readonly notificationService = inject(PortalNotificationService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly tableColumns: TableColumn[] = [
    { id: 'createdAt', label: $localize`:@@notificationsColumnDate:Date` },
    { id: 'resource', label: $localize`:@@notificationsColumnResource:API/App name` },
    { id: 'type', label: $localize`:@@notificationsColumnType:Notification Type` },
    { id: 'message', label: $localize`:@@notificationsColumnMessage:Message` },
  ];
  readonly tableActions: TableAction<NotificationRow>[] = [
    {
      id: MARK_AS_READ_ACTION_ID,
      icon: 'done',
      ariaLabel: $localize`:@@notificationsMarkAsRead:Mark as read`,
      isVisible: row => !row.isRead,
    },
  ];
  readonly statusOptions: SelectOption[] = [
    { value: 'all', label: $localize`:@@notificationsFilterStatusAll:All` },
    { value: 'unread', label: $localize`:@@notificationsFilterStatusUnread:Unread` },
    { value: 'read', label: $localize`:@@notificationsFilterStatusRead:Read` },
  ];
  readonly apiBadgeLabel = $localize`:@@notificationsApiBadge:API`;
  readonly applicationBadgeLabel = $localize`:@@notificationsApplicationBadge:App`;

  readonly actionError = signal<string | null>(null);
  readonly isMarkingAllAsRead = signal(false);

  private readonly queryParams = toSignal(this.activatedRoute.queryParams, { initialValue: {} as Record<string, unknown> });

  readonly filters = computed(() => this.parseParamsToFilters(this.queryParams()));
  readonly apiFilterValue = computed(() => toSelectionValue(this.filters().apiId));
  readonly applicationFilterValue = computed(() => toSelectionValue(this.filters().applicationId));
  readonly statusFilterValue = computed(() => [this.filters().status]);

  readonly filtersResource = rxResource({
    stream: () => this.notificationService.getFilters(),
  });
  private readonly filterReferences = computed(() => (this.filtersResource.hasValue() ? this.filtersResource.value() : undefined));
  readonly apiOptions = computed(() => this.toSelectOptions(this.filterReferences()?.apis, DELETED_API_LABEL));
  readonly applicationOptions = computed(() => this.toSelectOptions(this.filterReferences()?.applications, DELETED_APPLICATION_LABEL));

  readonly notificationsResource = rxResource({
    params: () => this.filters(),
    stream: ({ params }) =>
      this.notificationService
        .list({
          page: params.page,
          size: params.size,
          ...(params.status === 'all' ? {} : { read: params.status === 'read' }),
          ...(params.apiId ? { apiId: params.apiId } : {}),
          ...(params.applicationId ? { applicationId: params.applicationId } : {}),
        })
        .pipe(
          catchError((error: unknown) => {
            // The Portal API answers 400 for a page past the last one, e.g. once notifications were read elsewhere.
            if (params.page > NotificationsComponent.DEFAULT_PAGE && error instanceof HttpErrorResponse && error.status === 400) {
              this.updateQueryParams({ page: NotificationsComponent.DEFAULT_PAGE });
              return EMPTY;
            }
            return throwError(() => error);
          }),
        ),
  });

  // value() throws while the resource is in error, hence the hasValue() guards.
  private readonly notificationsResponse = computed(() =>
    this.notificationsResource.hasValue() ? this.notificationsResource.value() : undefined,
  );
  readonly rows = computed<NotificationRow[]>(() => (this.notificationsResponse()?.data ?? []).map(toNotificationRow));

  readonly totalElements = computed(() => {
    const response = this.notificationsResponse();
    return response?.metadata?.pagination?.total ?? response?.metadata?.data?.total ?? response?.data?.length ?? 0;
  });

  private readonly isFilteredByReference = computed(() => !!this.filters().apiId || !!this.filters().applicationId);

  /** Nothing at all to show: no notification whatever its status, and no API or application filter. */
  readonly hasNoNotification = computed(
    () =>
      !this.notificationsResource.isLoading() &&
      !this.notificationsResource.error() &&
      this.totalElements() === 0 &&
      this.filters().status === 'all' &&
      !this.isFilteredByReference(),
  );

  /** Only the default unread status filters the list out, so the user may still have read notifications. */
  readonly hasNoUnreadNotification = computed(
    () => this.totalElements() === 0 && this.filters().status === DEFAULT_STATUS && !this.isFilteredByReference(),
  );

  constructor() {
    this.breadcrumbService.set([notificationListBreadcrumb()]);
  }

  onApiFilterChange(selection: string[] | null): void {
    this.applyFilter({ apiId: selection?.[0] ?? null });
  }

  onApplicationFilterChange(selection: string[] | null): void {
    this.applyFilter({ applicationId: selection?.[0] ?? null });
  }

  onStatusFilterChange(selection: string[] | null): void {
    const status = parseStatusParam(selection?.[0]);
    this.updateQueryParams({ status: status === DEFAULT_STATUS ? null : status, page: NotificationsComponent.DEFAULT_PAGE });
  }

  onActionClick({ actionId, row }: { actionId: string; row: NotificationRow }): void {
    if (actionId === MARK_AS_READ_ACTION_ID) {
      this.markAsRead(row);
    }
  }

  markAllAsRead(): void {
    this.actionError.set(null);
    this.isMarkingAllAsRead.set(true);
    this.notificationService
      .markAllAsRead()
      .pipe(
        finalize(() => this.isMarkingAllAsRead.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.setNotificationsAsRead(() => true, { isEveryUnreadMarked: true }),
        error: () => {
          this.actionError.set(
            $localize`:@@notificationsMarkAllAsReadError:The notifications could not be marked as read. Try again later.`,
          );
        },
      });
  }

  clearFilters(): void {
    this.updateQueryParams({ apiId: null, applicationId: null, status: null, page: NotificationsComponent.DEFAULT_PAGE });
  }

  onPageChange(page: number): void {
    this.updateQueryParams({ page });
  }

  onPageSizeChange(size: number): void {
    this.updateQueryParams({ size, page: NotificationsComponent.DEFAULT_PAGE });
  }

  private markAsRead(row: NotificationRow): void {
    this.actionError.set(null);
    this.notificationService
      .markAsRead(row.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.setNotificationsAsRead(notification => notification.id === row.id),
        error: () =>
          this.actionError.set($localize`:@@notificationsMarkAsReadError:The notification could not be marked as read. Try again later.`),
      });
  }

  private setNotificationsAsRead(
    isTargeted: (notification: PortalNotification) => boolean,
    { isEveryUnreadMarked }: { isEveryUnreadMarked: boolean } = { isEveryUnreadMarked: false },
  ): void {
    const isUnreadList = this.filters().status === 'unread';
    this.notificationsResource.value.update(response => {
      if (!response) {
        return response;
      }
      if (!isUnreadList) {
        return {
          ...response,
          data: response.data.map(notification => (isTargeted(notification) ? { ...notification, read: true } : notification)),
        };
      }
      const stillUnread = response.data.filter(notification => !isTargeted(notification));
      const currentTotal = response.metadata?.pagination?.total ?? 0;
      // Marking all reads every unread notification of the scope, not only those of the displayed page.
      const total = isEveryUnreadMarked ? 0 : Math.max(currentTotal - (response.data.length - stillUnread.length), 0);
      return { ...response, data: stillUnread, metadata: withPaginationTotal(response.metadata, total) };
    });
    // The page may now be empty while other pages still hold unread notifications.
    if (isUnreadList && this.rows().length === 0 && this.totalElements() > 0) {
      if (this.filters().page > NotificationsComponent.DEFAULT_PAGE) {
        this.updateQueryParams({ page: this.filters().page - 1 });
      } else {
        this.notificationsResource.reload();
      }
    }
  }

  private applyFilter(filter: Partial<Pick<NotificationFilters, 'apiId' | 'applicationId'>>): void {
    this.updateQueryParams({ ...filter, page: NotificationsComponent.DEFAULT_PAGE });
  }

  private updateQueryParams(queryParams: Record<string, unknown>): void {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  private parseParamsToFilters(params: Record<string, unknown>): NotificationFilters {
    return {
      apiId: parseIdParam(params['apiId']),
      applicationId: parseIdParam(params['applicationId']),
      status: parseStatusParam(params['status']),
      page: parsePageParam(params['page'], NotificationsComponent.DEFAULT_PAGE),
      size: parseSizeParam(params['size'], NotificationsComponent.DEFAULT_PAGE_SIZE, NotificationsComponent.MAX_PAGE_SIZE),
    };
  }

  private toSelectOptions(references: PortalNotificationReference[] | undefined, deletedLabel: string): SelectOption[] {
    return (references ?? []).map(reference => ({ value: reference.id, label: reference.name ?? deletedLabel }));
  }
}

function toNotificationRow(notification: PortalNotification): NotificationRow {
  return {
    id: notification.id,
    createdAt: notification.created_at,
    type: deriveNotificationTypeLabel(notification.hook),
    apiName: notification.api ? (notification.api.name ?? DELETED_API_LABEL) : null,
    applicationName: notification.application ? (notification.application.name ?? DELETED_APPLICATION_LABEL) : null,
    message: notification.message,
    isRead: notification.read,
  };
}

function withPaginationTotal(metadata: PortalNotificationsResponse['metadata'], total: number): PortalNotificationsResponse['metadata'] {
  return { ...metadata, pagination: { ...metadata?.pagination, total } };
}

function toSelectionValue(value: string | null): string[] {
  return value ? [value] : [];
}

function parseIdParam(param: unknown): string | null {
  return typeof param === 'string' && param.trim() ? param.trim() : null;
}

function parseStatusParam(param: unknown): NotificationReadStatus {
  return param === 'read' || param === 'all' ? param : DEFAULT_STATUS;
}
