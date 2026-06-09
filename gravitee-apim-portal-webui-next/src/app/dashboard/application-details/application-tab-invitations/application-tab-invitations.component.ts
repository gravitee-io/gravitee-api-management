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
import { PlatformLocation } from '@angular/common';
import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { EMPTY, filter, finalize, map, of, switchMap, tap } from 'rxjs';

import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginatedTableComponent, TableAction, TableColumn } from '../../../../components/paginated-table/paginated-table.component';
import { TableCellDirective } from '../../../../components/paginated-table/table-cell.directive';
import { SearchBarComponent } from '../../../../components/search-bar/search-bar.component';
import {
  ApplicationInvitation,
  ApplicationInvitationsResponse,
  ApplicationInvitationsSearchFilters,
} from '../../../../entities/application/application-invitation';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { ApplicationInvitationService } from '../../../../services/application-invitation.service';
import {
  ApplicationInvitationCreateDialogComponent,
  ApplicationInvitationCreateDialogData,
} from '../application-invitation-create-dialog/application-invitation-create-dialog.component';
import {
  ApplicationInvitationEditDialogComponent,
  ApplicationInvitationEditDialogData,
} from '../application-invitation-edit-dialog/application-invitation-edit-dialog.component';

interface InvitationTableRow {
  id: string;
  email: string;
  role: string;
}

interface InvitationsRequestParams {
  applicationId: string;
  page: number;
  size: number;
  filters: ApplicationInvitationsSearchFilters;
}

interface InvitationsResourceValue {
  params: InvitationsRequestParams;
  response: ApplicationInvitationsResponse;
}

@Component({
  selector: 'app-application-tab-invitations',
  standalone: true,
  imports: [LoaderComponent, MatButtonModule, MatDialogModule, MatIcon, PaginatedTableComponent, SearchBarComponent, TableCellDirective],
  templateUrl: './application-tab-invitations.component.html',
  styleUrl: './application-tab-invitations.component.scss',
})
export class ApplicationTabInvitationsComponent {
  private readonly applicationInvitationService = inject(ApplicationInvitationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly platformLocation = inject(PlatformLocation);

  readonly applicationId = input.required<string>();
  readonly userApplicationPermissions = input.required<UserApplicationPermissions>();

  readonly currentPage = signal(1);
  readonly pageSize = signal(10);
  readonly searchTerm = signal('');
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly resendingInvitationId = signal<string | null>(null);
  readonly displayedTotalElements = signal<number | null>(null);
  readonly sectionTitle = computed(() => {
    const totalElements = this.displayedTotalElements();

    return totalElements === null
      ? $localize`:@@applicationInvitationsTitle:Invitations`
      : $localize`:@@applicationInvitationsTitleWithCount:Invitations (${totalElements}:count:)`;
  });

  readonly tableColumns: TableColumn[] = [
    { id: 'email', label: $localize`:@@applicationInvitationsColumnEmail:Email` },
    { id: 'role', label: $localize`:@@applicationInvitationsColumnRole:Role` },
  ];

  readonly canRead = computed(() => this.userApplicationPermissions().MEMBER?.includes('R') ?? false);
  readonly canCreate = computed(() => this.userApplicationPermissions().MEMBER?.includes('C') ?? false);
  readonly canUpdate = computed(() => this.userApplicationPermissions().MEMBER?.includes('U') ?? false);
  readonly canDelete = computed(() => this.userApplicationPermissions().MEMBER?.includes('D') ?? false);
  readonly hasSearchTerm = computed(() => this.searchTerm().trim().length > 0);
  readonly searchFilters = computed<ApplicationInvitationsSearchFilters>(() => {
    const email = this.searchTerm().trim();
    return email ? { email } : {};
  });

  readonly actions = computed<TableAction<InvitationTableRow>[]>(() => {
    const updateActions: TableAction<InvitationTableRow>[] = this.canUpdate()
      ? [
          {
            id: 'resend',
            icon: 'send',
            ariaLabel: $localize`:@@resendApplicationInvitationAriaLabel:Resend invitation`,
            isDisabled: invitation => this.resendingInvitationId() === invitation.id,
          },
          {
            id: 'edit',
            icon: 'edit',
            ariaLabel: $localize`:@@editApplicationInvitationAriaLabel:Edit invitation role`,
          },
        ]
      : [];

    const deleteActions: TableAction<InvitationTableRow>[] = this.canDelete()
      ? [
          {
            id: 'delete',
            icon: 'delete_outline',
            ariaLabel: $localize`:@@deleteApplicationInvitationAriaLabel:Delete invitation`,
            color: 'warn',
          },
        ]
      : [];

    return [...updateActions, ...deleteActions];
  });

  readonly requestParams = computed<InvitationsRequestParams | null>(() =>
    this.canRead()
      ? {
          applicationId: this.applicationId(),
          page: this.currentPage(),
          size: this.pageSize(),
          filters: this.searchFilters(),
        }
      : null,
  );

  protected readonly invitationsResource = rxResource<InvitationsResourceValue | undefined, InvitationsRequestParams | null>({
    params: () => this.requestParams(),
    stream: ({ params }) =>
      params
        ? this.applicationInvitationService
            .searchApplicationInvitations(params.applicationId, params.page, params.size, params.filters)
            .pipe(
              tap(response => this.displayedTotalElements.set(this.getTotalElements(response))),
              map(response => ({ params, response })),
            )
        : of(undefined),
  });

  readonly currentResponse = computed<ApplicationInvitationsResponse | undefined>(() => {
    const value = this.invitationsResource.value();
    const params = this.requestParams();
    if (!value || !params || !this.isSameRequestParams(value.params, params)) {
      return undefined;
    }
    return value.response;
  });

  readonly rows = computed<InvitationTableRow[]>(() => {
    if (this.invitationsResource.error()) {
      return [];
    }
    return (this.currentResponse()?.data ?? []).map(invitation => this.toRow(invitation));
  });

  readonly totalElements = computed<number>(() => {
    if (this.invitationsResource.error()) {
      return 0;
    }
    const response = this.currentResponse();
    return response ? this.getTotalElements(response) : 0;
  });

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  onSearchTermChange(term: string): void {
    this.searchTerm.set(term);
    this.currentPage.set(1);
  }

  onActionClick({ actionId, row }: { actionId: string; row: InvitationTableRow }): void {
    switch (actionId) {
      case 'resend':
        this.openResendConfirmation(row);
        break;
      case 'edit':
        this.openEditInvitationDialog(row);
        break;
      case 'delete':
        this.openDeleteConfirmation(row);
        break;
    }
  }

  openCreateInvitationDialog(): void {
    this.clearActionFeedback();
    this.matDialog
      .open<ApplicationInvitationCreateDialogComponent, ApplicationInvitationCreateDialogData, boolean>(
        ApplicationInvitationCreateDialogComponent,
        {
          data: { applicationId: this.applicationId() },
          disableClose: true,
        },
      )
      .afterClosed()
      .pipe(
        filter((invitationsCreated): invitationsCreated is true => invitationsCreated === true),
        tap(() => this.invitationsResource.reload()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private openEditInvitationDialog(invitation: InvitationTableRow): void {
    this.clearActionFeedback();
    this.matDialog
      .open<ApplicationInvitationEditDialogComponent, ApplicationInvitationEditDialogData, boolean>(
        ApplicationInvitationEditDialogComponent,
        {
          data: {
            applicationId: this.applicationId(),
            invitationId: invitation.id,
            invitationEmail: invitation.email,
            currentRole: invitation.role,
          },
        },
      )
      .afterClosed()
      .pipe(
        filter(invitationUpdated => invitationUpdated === true),
        tap(() => this.invitationsResource.reload()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private openResendConfirmation(invitation: InvitationTableRow): void {
    this.clearActionFeedback();
    this.matDialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        data: {
          title: $localize`:@@resendApplicationInvitationDialogTitle:Resend invitation?`,
          content: $localize`:@@resendApplicationInvitationDialogContent:This will send a new invitation email to ${invitation.email}:invitationEmail: and refresh its validity period.`,
          confirmLabel: $localize`:@@resendApplicationInvitationDialogConfirm:Resend`,
          cancelLabel: $localize`:@@resendApplicationInvitationDialogCancel:Cancel`,
        },
      })
      .afterClosed()
      .pipe(
        switchMap(confirmed => {
          if (!confirmed) {
            return EMPTY;
          }

          this.resendingInvitationId.set(invitation.id);
          return this.applicationInvitationService
            .resendApplicationInvitation(this.applicationId(), invitation.id, {
              confirmation_page_url: this.buildRegistrationConfirmationPageUrl(),
            })
            .pipe(finalize(() => this.resendingInvitationId.set(null)));
        }),
        tap(() => {
          this.successMessage.set(
            $localize`:@@resendApplicationInvitationSuccess:Invitation email resent to ${invitation.email}:invitationEmail:.`,
          );
          this.invitationsResource.reload();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: () =>
          this.error.set(
            $localize`:@@resendApplicationInvitationError:An error occurred while resending the invitation. Please try again.`,
          ),
      });
  }

  private openDeleteConfirmation(invitation: InvitationTableRow): void {
    this.clearActionFeedback();
    this.matDialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        data: {
          title: $localize`:@@deleteApplicationInvitationDialogTitle:Delete invitation?`,
          content: $localize`:@@deleteApplicationInvitationDialogContent:Are you sure you want to delete the invitation for ${invitation.email}:invitationEmail:?`,
          confirmLabel: $localize`:@@deleteApplicationInvitationDialogConfirm:Delete`,
          cancelLabel: $localize`:@@deleteApplicationInvitationDialogCancel:Cancel`,
        },
      })
      .afterClosed()
      .pipe(
        switchMap(confirmed =>
          confirmed ? this.applicationInvitationService.deleteApplicationInvitation(this.applicationId(), invitation.id) : EMPTY,
        ),
        tap(() => this.reloadInvitationsAfterDelete()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: () =>
          this.error.set($localize`:@@deleteApplicationInvitationError:An error occurred while deleting the invitation. Please try again.`),
      });
  }

  private clearActionFeedback(): void {
    this.error.set(null);
    this.successMessage.set(null);
  }

  private buildRegistrationConfirmationPageUrl(): string {
    const baseHref = this.platformLocation.getBaseHrefFromDOM() || '/';
    const normalizedBaseHref = baseHref.endsWith('/') ? baseHref : `${baseHref}/`;

    return new URL(`${normalizedBaseHref}user/invitation/confirm`, globalThis.location.origin).toString();
  }

  private reloadInvitationsAfterDelete(): void {
    const totalElementsAfterDelete = Math.max(this.totalElements() - 1, 0);
    const lastPageAfterDelete = Math.max(1, Math.ceil(totalElementsAfterDelete / this.pageSize()));

    if (this.currentPage() > lastPageAfterDelete) {
      this.currentPage.set(lastPageAfterDelete);
      return;
    }

    this.invitationsResource.reload();
  }

  private toRow(invitation: ApplicationInvitation): InvitationTableRow {
    return {
      id: invitation.id,
      email: invitation.email,
      role: invitation.role,
    };
  }

  private isSameRequestParams(left: InvitationsRequestParams, right: InvitationsRequestParams): boolean {
    return (
      left.applicationId === right.applicationId &&
      left.page === right.page &&
      left.size === right.size &&
      left.filters?.email === right.filters?.email
    );
  }

  private getTotalElements(response: ApplicationInvitationsResponse): number {
    return response.metadata?.paginateMetaData?.totalElements ?? response.metadata?.pagination?.total ?? response.data?.length ?? 0;
  }
}
