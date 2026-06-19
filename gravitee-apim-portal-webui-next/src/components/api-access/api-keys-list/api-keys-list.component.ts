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
import { Component, computed, input, output, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import { isActiveApiKey, SubscriptionDataKeys } from '../../../entities/subscription/subscription';
import { PaginatedTableComponent, TableColumn } from '../../paginated-table/paginated-table.component';
import { TableCellDirective } from '../../paginated-table/table-cell.directive';

interface ApiKeyRow {
  statusIcon: 'gio:check-circled-outline' | 'gio:x-circle';
  statusLabel: string;
  revokeAriaLabel: string;
  isActive: boolean;
  key: string;
  createdAt?: string;
  closedAt?: string;
  originalKey: SubscriptionDataKeys;
}

export interface ApiKeyFeedback {
  type: 'success' | 'error';
  message: string;
}

@Component({
  selector: 'app-api-keys-list',
  imports: [MatIcon, MatIconButton, MatTooltip, PaginatedTableComponent, TableCellDirective],
  templateUrl: './api-keys-list.component.html',
  styleUrl: './api-keys-list.component.scss',
})
export class ApiKeysListComponent {
  apiKeys = input.required<SubscriptionDataKeys[]>();
  canManageApiKey = input(false);
  isRevokeDisabled = input(false);
  feedback = input<ApiKeyFeedback | undefined>(undefined);

  revokeApiKey = output<SubscriptionDataKeys>();

  protected readonly columns: TableColumn[] = [
    { id: 'status', label: $localize`:@@apiKeysListColumnStatus:Status` },
    { id: 'key', label: $localize`:@@apiKeysListColumnKey:Key` },
    { id: 'createdAt', label: $localize`:@@apiKeysListColumnCreatedAt:Created At`, type: 'date-time' },
    { id: 'closedAt', label: $localize`:@@apiKeysListColumnRevokedAt:Revoked/Expired At`, type: 'date-time' },
    { id: 'revoke', label: '' },
  ];
  protected readonly pageSizeOptions = [5, 10, 25];
  protected readonly pageSize = signal(5);
  private readonly requestedCurrentPage = signal(1);

  protected readonly rows = computed<ApiKeyRow[]>(() => this.apiKeys().map(apiKey => this.toRow(apiKey)));
  protected readonly totalElements = computed(() => this.rows().length);
  protected readonly lastValidPage = computed(() => Math.max(1, Math.ceil(this.totalElements() / this.pageSize())));
  protected readonly currentPage = computed(() => Math.min(this.requestedCurrentPage(), this.lastValidPage()));
  protected readonly displayedRows = computed(() => {
    const startIndex = (this.currentPage() - 1) * this.pageSize();
    const endIndex = startIndex + this.pageSize();
    return this.rows().slice(startIndex, endIndex);
  });

  protected onPageChange(page: number): void {
    this.requestedCurrentPage.set(page);
  }

  protected onPageSizeChange(pageSize: number): void {
    this.pageSize.set(pageSize);
    this.requestedCurrentPage.set(1);
  }

  private toRow(apiKey: SubscriptionDataKeys): ApiKeyRow {
    const isActive = isActiveApiKey(apiKey);
    return {
      statusIcon: isActive ? 'gio:check-circled-outline' : 'gio:x-circle',
      statusLabel: isActive
        ? $localize`:@@apiKeysListStatusActive:Active API key`
        : $localize`:@@apiKeysListStatusInactive:Inactive API key`,
      revokeAriaLabel: $localize`:@@apiKeysListRevokeAriaLabel:Revoke API key ${apiKey.key ?? ''}:apiKey:`,
      isActive,
      key: apiKey.key ?? '',
      createdAt: apiKey.created_at,
      closedAt: apiKey.revoked_at ?? apiKey.expire_at,
      originalKey: apiKey,
    };
  }
}
