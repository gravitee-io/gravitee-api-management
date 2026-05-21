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
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import { of } from 'rxjs';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../../components/paginated-table/paginated-table.component';
import { TableCellDirective } from '../../../../components/paginated-table/table-cell.directive';
import { ApplicationInvitation, ApplicationInvitationsResponse } from '../../../../entities/application/application-invitation';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { ApplicationInvitationService } from '../../../../services/application-invitation.service';

interface InvitationTableRow {
  id: string;
  email: string;
  role: string;
}

interface InvitationsRequestParams {
  applicationId: string;
  page: number;
  size: number;
}

@Component({
  selector: 'app-application-tab-invitations',
  standalone: true,
  imports: [LoaderComponent, MatIcon, PaginatedTableComponent, TableCellDirective],
  templateUrl: './application-tab-invitations.component.html',
  styleUrl: './application-tab-invitations.component.scss',
})
export class ApplicationTabInvitationsComponent {
  private readonly applicationInvitationService = inject(ApplicationInvitationService);

  readonly applicationId = input.required<string>();
  readonly userApplicationPermissions = input.required<UserApplicationPermissions>();

  readonly currentPage = signal(1);
  readonly pageSize = signal(10);

  readonly tableColumns: TableColumn[] = [
    { id: 'email', label: $localize`:@@applicationInvitationsColumnEmail:Email` },
    { id: 'role', label: $localize`:@@applicationInvitationsColumnRole:Role` },
    { id: 'actions', label: $localize`:@@applicationInvitationsColumnActions:Actions` },
  ];

  readonly canRead = computed(() => this.userApplicationPermissions().MEMBER?.includes('R') ?? false);

  protected readonly invitationsResource = rxResource<ApplicationInvitationsResponse | undefined, InvitationsRequestParams | null>({
    params: () =>
      this.canRead()
        ? {
            applicationId: this.applicationId(),
            page: this.currentPage(),
            size: this.pageSize(),
          }
        : null,
    stream: ({ params }) =>
      params
        ? this.applicationInvitationService.searchApplicationInvitations(params.applicationId, params.page, params.size)
        : of(undefined),
  });

  readonly rows = computed<InvitationTableRow[]>(() => {
    if (this.invitationsResource.error()) {
      return [];
    }
    return (this.invitationsResource.value()?.data ?? []).map(invitation => this.toRow(invitation));
  });

  readonly totalElements = computed(() => {
    if (this.invitationsResource.error()) {
      return 0;
    }
    const response = this.invitationsResource.value();
    return response?.metadata?.paginateMetaData?.totalElements ?? response?.metadata?.pagination?.total ?? response?.data?.length ?? 0;
  });

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  private toRow(invitation: ApplicationInvitation): InvitationTableRow {
    return {
      id: invitation.id,
      email: invitation.email,
      role: invitation.role,
    };
  }
}
