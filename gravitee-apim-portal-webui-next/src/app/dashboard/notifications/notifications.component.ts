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
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { BehaviorSubject, catchError, forkJoin, map, merge, startWith, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { notificationListBreadcrumb } from './notification-breadcrumbs';
import { NotificationPreferencesComponent } from './notification-preferences/notification-preferences.component';
import { EnrichedPortalNotification, enrichPortalNotification, PortalNotificationSource } from './portal-notification.mapper';
import { BadgeComponent } from '../../../components/badge/badge.component';
import { DropdownSearchComponent } from '../../../components/dropdown-search/dropdown-search.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginatedTableComponent, TableAction, TableColumn } from '../../../components/paginated-table/paginated-table.component';
import { TableCellDirective } from '../../../components/paginated-table/table-cell.directive';
import { PortalNotification } from '../../../entities/notification/portal-notification';
import { ApplicationService } from '../../../services/application.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { NotificationReadArchiveService } from '../../../services/notification-read-archive.service';
import { UserNotificationInboxService } from '../../../services/user-notification-inbox.service';
import { UserNotificationService } from '../../../services/user-notification.service';

type NotificationReadStatus = 'unread' | 'read' | 'all';
type NotificationsView = 'inbox' | 'preferences';

@Component({
  selector: 'app-notifications',
  imports: [
    BadgeComponent,
    DropdownSearchComponent,
    LoaderComponent,
    MatButton,
    MatButtonToggleModule,
    NotificationPreferencesComponent,
    PaginatedTableComponent,
    ReactiveFormsModule,
    TableCellDirective,
  ],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export default class NotificationsComponent {
  private static readonly DEFAULT_PAGE_SIZE = 10;
  private static readonly ALL_PAGE_SIZE = -1;

  private readonly userNotificationService = inject(UserNotificationService);
  private readonly applicationService = inject(ApplicationService);
  private readonly notificationReadArchiveService = inject(NotificationReadArchiveService);
  private readonly userNotificationInboxService = inject(UserNotificationInboxService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  readonly pageSize = NotificationsComponent.DEFAULT_PAGE_SIZE;
  readonly activeView = signal<NotificationsView>('inbox');
  readonly error = signal<string | null>(null);
  readonly archiveVersion = signal(0);
  readonly currentPage = signal(1);
  readonly isLoading = signal(true);

  readonly apiFilter = new FormControl<string[]>([]);
  readonly applicationFilter = new FormControl<string[]>([]);
  readonly statusFilter = new FormControl<string[]>(['unread']);

  readonly statusOptions = [
    { value: 'unread', label: $localize`:@@notificationsFilterStatusUnread:Unread` },
    { value: 'read', label: $localize`:@@notificationsFilterStatusRead:Read` },
    { value: 'all', label: $localize`:@@notificationsFilterStatusAll:All` },
  ];

  readonly tableColumns: TableColumn[] = [
    { id: 'created_at', label: $localize`:@@notificationsColumnDate:Date`, type: 'date-time' },
    { id: 'resourceName', label: $localize`:@@notificationsColumnResource:API/App name` },
    { id: 'notificationType', label: $localize`:@@notificationsColumnType:Notification Type` },
    { id: 'message', label: $localize`:@@notificationsColumnMessage:Message` },
  ];

  readonly actions: TableAction<EnrichedPortalNotification>[] = [
    {
      id: 'read',
      icon: 'done',
      ariaLabel: $localize`:@@notificationsMarkAsRead:Mark as read`,
      isVisible: row => !row.read,
    },
  ];

  private readonly selectedApiNames = toSignal(this.apiFilter.valueChanges.pipe(startWith(this.apiFilter.value ?? [])), {
    initialValue: [] as string[],
  });
  private readonly selectedApplicationNames = toSignal(
    this.applicationFilter.valueChanges.pipe(startWith(this.applicationFilter.value ?? [])),
    { initialValue: [] as string[] },
  );
  private readonly selectedStatus = toSignal(this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value ?? ['unread'])), {
    initialValue: ['unread'] as string[],
  });

  private readonly notificationsPage = toSignal(
    this.reload$.pipe(
      tap(() => {
        this.isLoading.set(true);
        this.error.set(null);
      }),
      switchMap(() =>
        forkJoin({
          inbox: this.userNotificationService.list(1, NotificationsComponent.ALL_PAGE_SIZE),
          applications: this.applicationService.list(1, NotificationsComponent.ALL_PAGE_SIZE).pipe(catchError(() => of({ data: [] }))),
        }).pipe(
          map(({ inbox, applications }) => ({
            inbox: inbox.data ?? [],
            applications: applications.data ?? [],
          })),
          catchError(() => {
            this.error.set(
              $localize`:@@notificationsLoadError:An error occurred while loading notifications. Try again later or contact your Portal administrator.`,
            );
            return of({ inbox: [] as PortalNotification[], applications: [] });
          }),
        ),
      ),
      tap(() => this.isLoading.set(false)),
    ),
    { initialValue: { inbox: [] as PortalNotification[], applications: [] } },
  );

  readonly allRows = computed(() => {
    this.archiveVersion();
    const inbox = this.notificationsPage().inbox;
    const inboxIds = new Set(inbox.map(notification => notification.id).filter((id): id is string => !!id));
    const unread = inbox.map(notification => enrichPortalNotification(notification, false));
    const read = this.notificationReadArchiveService
      .list()
      .filter(notification => !inboxIds.has(notification.id ?? ''))
      .map(notification => enrichPortalNotification(notification, true));

    return [...unread, ...read].sort((left, right) => {
      const leftTime = Date.parse(left.created_at ?? '') || 0;
      const rightTime = Date.parse(right.created_at ?? '') || 0;
      return rightTime - leftTime;
    });
  });

  readonly apiOptions = computed(() => {
    const names = new Set<string>();
    for (const row of this.allRows()) {
      if (row.apiName) {
        names.add(row.apiName);
      }
    }
    return [...names].sort((a, b) => a.localeCompare(b)).map(name => ({ value: name, label: name }));
  });

  readonly applicationOptions = computed(() => {
    const names = new Set<string>();
    for (const application of this.notificationsPage().applications) {
      if (application.name) {
        names.add(application.name);
      }
    }
    for (const row of this.allRows()) {
      if (row.applicationName) {
        names.add(row.applicationName);
      }
    }
    return [...names].sort((a, b) => a.localeCompare(b)).map(name => ({ value: name, label: name }));
  });

  readonly filteredRows = computed(() => {
    const apiNames = this.selectedApiNames() ?? [];
    const applicationNames = this.selectedApplicationNames() ?? [];
    const status = this.parseStatus((this.selectedStatus() ?? [])[0]);

    return this.allRows().filter(row => this.matchesFilters(row, { apiNames, applicationNames, status }));
  });
  readonly totalElements = computed(() => this.filteredRows().length);
  readonly rows = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredRows().slice(start, start + this.pageSize);
  });

  readonly hasAnyNotifications = computed(() => this.allRows().length > 0);

  constructor() {
    this.breadcrumbService.set([notificationListBreadcrumb()]);
    merge(this.apiFilter.valueChanges, this.applicationFilter.valueChanges, this.statusFilter.valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.currentPage.set(1));
  }

  setActiveView(view: NotificationsView | string | undefined): void {
    if (view === 'inbox' || view === 'preferences') {
      this.activeView.set(view);
    }
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  clearFilters(): void {
    this.apiFilter.setValue([]);
    this.applicationFilter.setValue([]);
    this.statusFilter.setValue(['unread']);
    this.currentPage.set(1);
  }

  sourceLabel(source: PortalNotificationSource): string {
    switch (source) {
      case 'API':
        return $localize`:@@notificationsSourceApi:API`;
      case 'APPLICATION':
        return $localize`:@@notificationsSourceApp:App`;
      default:
        return $localize`:@@notificationsSourcePortal:Portal`;
    }
  }

  onActionClick(event: { actionId: string; row: EnrichedPortalNotification }): void {
    if (event.actionId !== 'read' || !event.row.id || event.row.read) {
      return;
    }

    const original: PortalNotification = {
      id: event.row.id,
      title: event.row.title,
      message: event.row.message,
      created_at: event.row.created_at,
    };

    this.notificationReadArchiveService.archive(original);
    this.archiveVersion.update(version => version + 1);

    this.userNotificationService
      .markAsRead(event.row.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.userNotificationInboxService.refresh();
          this.reload$.next();
        },
        error: () => this.error.set($localize`:@@notificationsMarkAsReadError:An error occurred while marking the notification as read.`),
      });
  }

  private matchesFilters(
    row: EnrichedPortalNotification,
    filters: {
      apiNames: string[];
      applicationNames: string[];
      status: NotificationReadStatus;
    },
  ): boolean {
    if (filters.status === 'unread' && row.read) {
      return false;
    }
    if (filters.status === 'read' && !row.read) {
      return false;
    }
    if (
      filters.apiNames.length &&
      !this.matchesNameFilter(filters.apiNames, [row.apiName, row.source === 'API' ? row.resourceName : undefined])
    ) {
      return false;
    }
    if (
      filters.applicationNames.length &&
      !this.matchesNameFilter(filters.applicationNames, [row.applicationName, row.source === 'APPLICATION' ? row.resourceName : undefined])
    ) {
      return false;
    }
    return true;
  }

  private matchesNameFilter(selected: string[], candidates: Array<string | undefined>): boolean {
    return selected.some(name => candidates.includes(name));
  }

  private parseStatus(value: unknown): NotificationReadStatus {
    if (value === 'read' || value === 'all' || value === 'unread') {
      return value;
    }
    return 'unread';
  }
}
