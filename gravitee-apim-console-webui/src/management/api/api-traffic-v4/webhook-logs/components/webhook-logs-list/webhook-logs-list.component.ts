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

import { Component, computed, input, output } from '@angular/core';
import { GioAvatarModule } from '@gravitee/ui-particles-angular';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { GioTableWrapperModule } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { Pagination } from '../../../../../../entities/management-api-v2';
import {
  GioTableWrapperFilters,
  GioTableWrapperPagination,
} from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTooltipOnEllipsisModule } from '../../../../../../shared/components/gio-tooltip-on-ellipsis/gio-tooltip-on-ellipsis.module';
import { WebhookLog } from '../../models';

@Component({
  selector: 'webhook-logs-list',
  templateUrl: './webhook-logs-list.component.html',
  styleUrls: ['./webhook-logs-list.component.scss'],
  standalone: true,
  imports: [
    GioAvatarModule,
    GioTableWrapperModule,
    MatIcon,
    MatTableModule,
    MatSort,
    MatTooltipModule,
    RouterLink,
    MatButtonModule,
    DatePipe,
    GioTooltipOnEllipsisModule,
  ],
})
export class WebhookLogsListComponent {
  logs = input.required<WebhookLog[]>();
  pagination = input.required<Pagination>();

  logDetailsClicked = output<WebhookLog>();
  paginationUpdated = output<GioTableWrapperPagination>();

  readonly gioTableWrapperFilters = computed(() => {
    const pagination = this.pagination();
    return {
      searchTerm: '',
      pagination: {
        index: pagination.page ?? 1,
        size: pagination.perPage ?? 10,
      },
    };
  });

  readonly displayedColumns = computed(() => ['timestamp', 'status', 'callbackUrl', 'application', 'duration', 'actions']);

  readonly pageSizeOptions: number[] = [10, 25, 50, 100];

  onFiltersChanged(event: GioTableWrapperFilters) {
    const eventPagination = event.pagination;
    const currentPagination = this.pagination();
    if (
      (currentPagination.perPage >= 0 && currentPagination.perPage !== eventPagination.size) ||
      (currentPagination.page >= 0 && currentPagination.page !== eventPagination.index)
    ) {
      this.paginationUpdated.emit({ index: eventPagination.index, size: eventPagination.size });
    }
  }
}
