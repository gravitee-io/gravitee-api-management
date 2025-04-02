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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, Input, OnChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  GIO_DIALOG_WIDTH,
  GioClipboardModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioIconsModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { filter, map, startWith, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSortModule } from '@angular/material/sort';
import { BehaviorSubject, Observable } from 'rxjs';
import { isEqual } from 'lodash';

import { gioTableFilterCollection } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApplicationSubscriptionService } from '../../../../../../services-ngx/application-subscription.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../../../../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperFilters } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { ApplicationService } from '../../../../../../services-ngx/application.service';

type ApiKeyVM = {
  id: string;
  key: string;
  createdAt: number;
  endDate: number;
  isValid: boolean;
};
@Component({
  selector: 'subscription-api-keys',
  templateUrl: './subscription-api-keys.component.html',
  styleUrls: ['./subscription-api-keys.component.scss'],
  imports: [
    CommonModule,
    GioIconsModule,
    GioLoaderModule,
    MatButtonModule,
    MatTableModule,
    MatTooltipModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSortModule,
    GioIconsModule,
    GioTableWrapperModule,
    GioClipboardModule,
    GioPermissionModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubscriptionApiKeysComponent implements OnChanges {
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly applicationSubscriptionService = inject(ApplicationSubscriptionService);
  private readonly applicationService = inject(ApplicationService);

  @Input({ required: true })
  applicationId!: string;

  @Input({ required: false })
  subscriptionId: string;

  @Input()
  public subtitleText?: string = undefined;

  @Input()
  public readonly = false;

  public displayedColumns = ['active-icon', 'key', 'createdAt', 'endDate', 'actions'];

  public defaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    sort: {
      active: 'isValid',
      direction: 'desc',
    },
    pagination: {
      index: 1,
      size: 25,
    },
  };

  public filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.defaultFilters);

  public pageVM$: Observable<{
    apiKeys: ApiKeyVM[];
    totalLength: number;
    filters: GioTableWrapperFilters;
    loading: boolean;
  }>;

  public onFiltersChanged(filters: GioTableWrapperFilters) {
    if (!isEqual(filters, this.filters$.value)) {
      this.filters$.next(filters);
    }
  }

  ngOnChanges() {
    const defaultColumns = ['active-icon', 'key', 'createdAt', 'endDate'];
    this.displayedColumns = this.readonly ? defaultColumns : [...defaultColumns, 'actions'];

    const getApiKeys$ = this.subscriptionId
      ? this.applicationSubscriptionService.getApiKeys(this.applicationId, this.subscriptionId)
      : this.applicationService.getApiKeys(this.applicationId);

    this.pageVM$ = this.filters$.pipe(
      switchMap((filters) => getApiKeys$.pipe(map((apiKeys) => ({ apiKeys, filters })))),
      map(({ apiKeys, filters }) => {
        const filtered = gioTableFilterCollection(
          apiKeys.map((apiKey) => ({
            id: apiKey.id,
            key: apiKey.key,
            createdAt: apiKey.created_at,
            endDate: apiKey.revoked ? apiKey.revoked_at : apiKey.expire_at,
            isValid: !apiKey.revoked && !apiKey.expired,
          })),
          filters,
        );
        return {
          apiKeys: filtered.filteredCollection,
          totalLength: filtered.unpaginatedLength,
          filters,
          loading: false,
        };
      }),
      startWith({
        apiKeys: [],
        totalLength: 0,
        filters: this.defaultFilters,
        loading: true,
      }),
    );
  }

  revokeApiKey(apiKeyVM: ApiKeyVM) {
    const revokeApiKey$ = this.subscriptionId
      ? this.applicationSubscriptionService.revokeApiKey(this.applicationId, this.subscriptionId, apiKeyVM.id)
      : this.applicationService.revokeApiKey(this.applicationId, apiKeyVM.id);

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: 'Revoke API Key',
          content: `Are you sure you want to revoke API Key <code>${apiKeyVM.key}</code>?`,
        },
        role: 'alertdialog',
        id: 'revokeApiKeysDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap(() => revokeApiKey$),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success(`API Key revoked`);
          this.filters$.next(this.filters$.value);
        },
        error: (err) => this.snackBarService.error(err.message),
      });
  }

  renewApiKey() {
    const renewApiKey$ = this.subscriptionId
      ? this.applicationSubscriptionService.renewApiKey(this.applicationId, this.subscriptionId)
      : this.applicationService.renewApiKey(this.applicationId);

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: 'Renew API Key',
          content: `Are you sure you want to renew API Key ?`,
        },
        role: 'alertdialog',
        id: 'renewApiKeysDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap(() => renewApiKey$),
        takeUntilDestroyed(this.destroyRef),
      )

      .subscribe({
        next: () => {
          this.snackBarService.success(`API Key renewed`);
          this.filters$.next(this.filters$.value);
        },
        error: (err) => this.snackBarService.error(err.message),
      });
  }
}
