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
import { Component, computed, DestroyRef, inject, Input, model, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarConfig, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltip } from '@angular/material/tooltip';
import { filter, tap } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { NativeKafkaApiAccessComponent } from './native-kafka-api-access/native-kafka-api-access.component';
import { ApiType } from '../../entities/api/api';
import { PlanSecurityEnum } from '../../entities/plan/plan';
import { Subscription, SubscriptionDataKeys } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { SubscriptionKeysService } from '../../services/subscription-keys.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../confirm-dialog/confirm-dialog.component';
import { CopyCodeIconComponent } from '../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
import { CopyCodeComponent } from '../copy-code/copy-code.component';
import { PaginatedTableComponent, TableColumn } from '../paginated-table/paginated-table.component';
import { TableCellDirective } from '../paginated-table/table-cell.directive';

interface ApiKeyTableRow {
  statusIcon: 'gio:check-circled-outline' | 'gio:x-circle';
  statusLabel: string;
  revokeAriaLabel: string;
  isActive: boolean;
  key: string;
  createdAt?: string;
  closedAt?: string;
}

interface ApiKeyRenewFeedback {
  type: 'success' | 'error';
  message: string;
}

@Component({
  selector: 'app-api-access',
  imports: [
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatButton,
    MatIconButton,
    CopyCodeComponent,
    NativeKafkaApiAccessComponent,
    MatFormFieldModule,
    MatIcon,
    MatSelectModule,
    MatSnackBarModule,
    MatTooltip,
    CopyCodeIconComponent,
    PaginatedTableComponent,
    TableCellDirective,
  ],
  templateUrl: './api-access.component.html',
  styleUrl: './api-access.component.scss',
})
export class ApiAccessComponent {
  private readonly configService = inject(ConfigService);
  private readonly subscriptionKeysService = inject(SubscriptionKeysService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  @Input()
  set planSecurity(value: PlanSecurityEnum) {
    this.planSecurityInput.set(value);
  }

  get planSecurity(): PlanSecurityEnum {
    return this.planSecurityInput() as PlanSecurityEnum;
  }

  @Input()
  set subscription(value: Subscription | undefined) {
    this.subscriptionInput.set(value);
  }

  get subscription(): Subscription | undefined {
    return this.subscriptionInput();
  }

  @Input()
  set apiKeys(value: SubscriptionDataKeys[] | undefined) {
    this.apiKeysInput.set(value ?? []);
  }

  get apiKeys(): SubscriptionDataKeys[] {
    return this.apiKeysInput();
  }

  @Input()
  set apiKeyConfigUsername(value: string | undefined) {
    this.apiKeyConfigUsernameInput.set(value);
  }

  get apiKeyConfigUsername(): string | undefined {
    return this.apiKeyConfigUsernameInput();
  }

  @Input()
  set apiType(value: ApiType | undefined) {
    this.apiTypeInput.set(value);
  }

  get apiType(): ApiType | undefined {
    return this.apiTypeInput();
  }

  @Input()
  set entrypointUrls(value: string[] | undefined) {
    this.entrypointUrlsInput.set(value);
  }

  get entrypointUrls(): string[] | undefined {
    return this.entrypointUrlsInput();
  }

  @Input()
  set clientId(value: string | undefined) {
    this.clientIdInput.set(value);
  }

  get clientId(): string | undefined {
    return this.clientIdInput();
  }

  @Input()
  set clientSecret(value: string | undefined) {
    this.clientSecretInput.set(value);
  }

  get clientSecret(): string | undefined {
    return this.clientSecretInput();
  }

  @Input()
  set canManageApiKey(value: boolean) {
    this.canManageApiKeyInput.set(value);
  }

  get canManageApiKey(): boolean {
    return this.canManageApiKeyInput();
  }

  apiKeyRevoked = output<void>();
  apiKeyRenewed = output<void>();

  protected readonly apiKeysColumns: TableColumn[] = [
    { id: 'status', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnStatus:Status` },
    { id: 'key', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnKey:Key` },
    { id: 'createdAt', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnCreatedAt:Created At`, type: 'date-time' },
    { id: 'closedAt', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnRevokedAt:Revoked/Expired At`, type: 'date-time' },
    { id: 'revoke', label: '' },
  ];
  protected readonly apiKeysPageSizeOptions = [5, 10, 25];

  private readonly planSecurityInput = signal<PlanSecurityEnum | undefined>(undefined);
  private readonly subscriptionInput = signal<Subscription | undefined>(undefined);
  private readonly apiKeysInput = signal<SubscriptionDataKeys[]>([]);
  private readonly apiKeyConfigUsernameInput = signal<string | undefined>(undefined);
  private readonly apiTypeInput = signal<ApiType | undefined>(undefined);
  private readonly entrypointUrlsInput = signal<string[] | undefined>(undefined);
  private readonly clientIdInput = signal<string | undefined>(undefined);
  private readonly clientSecretInput = signal<string | undefined>(undefined);
  private readonly canManageApiKeyInput = signal(false);
  protected readonly apiKeyRenewFeedback = signal<ApiKeyRenewFeedback | undefined>(undefined);
  protected readonly isRenewingApiKey = signal(false);
  protected readonly isRenewApiKeyDialogOpen = signal(false);

  protected readonly entrypointUrlValues = computed(() => this.entrypointUrlsInput() ?? []);
  protected readonly apiKeyRows = computed<ApiKeyTableRow[]>(() =>
    this.apiKeysInput().map(apiKey => {
      const isActive = this.isActiveApiKey(apiKey);
      return {
        statusIcon: isActive ? 'gio:check-circled-outline' : 'gio:x-circle',
        statusLabel: isActive
          ? $localize`:@@subscriptionDetailsApiAccessApiKeysStatusActive:Active API key`
          : $localize`:@@subscriptionDetailsApiAccessApiKeysStatusInactive:Inactive API key`,
        revokeAriaLabel: $localize`:@@subscriptionDetailsApiAccessRevokeAriaLabel:Revoke API key ${apiKey.key ?? ''}:apiKey:`,
        isActive,
        key: apiKey.key ?? '',
        createdAt: apiKey.created_at,
        closedAt: apiKey.revoked_at ?? apiKey.expire_at,
      };
    }),
  );
  protected readonly activeApiKey = computed(() => this.apiKeysInput().find(apiKey => this.isActiveApiKey(apiKey)));
  protected readonly hasActiveApiKey = computed(() => !!this.activeApiKey());
  protected readonly totalApiKeyElements = computed(() => this.apiKeyRows().length);
  protected readonly apiKeysPageSize = signal(5);
  private readonly requestedApiKeysCurrentPage = signal(1);
  protected readonly lastValidApiKeysPage = computed(() => Math.max(1, Math.ceil(this.totalApiKeyElements() / this.apiKeysPageSize())));
  protected readonly apiKeysCurrentPage = computed(() => Math.min(this.requestedApiKeysCurrentPage(), this.lastValidApiKeysPage()));
  protected readonly displayedApiKeyRows = computed(() => {
    const startIndex = (this.apiKeysCurrentPage() - 1) * this.apiKeysPageSize();
    const endIndex = startIndex + this.apiKeysPageSize();
    return this.apiKeyRows().slice(startIndex, endIndex);
  });
  protected readonly primaryApiKey = computed(() => this.activeApiKey()?.key ?? '');
  protected readonly resolvedApiKeyConfigUsername = computed(() => this.apiKeyConfigUsernameInput() ?? this.activeApiKey()?.hash ?? '');
  protected readonly shouldShowApiAccessContent = computed(
    () => this.planSecurityInput() !== 'API_KEY' || this.subscriptionInput()?.status !== 'ACCEPTED' || this.hasActiveApiKey(),
  );
  protected isRevokingApiKey = false;
  protected isRevokeApiKeyDialogOpen = false;
  private readonly defaultSnackBarOptions: MatSnackBarConfig = {
    duration: 5000,
    horizontalPosition: 'end',
  };

  selectedEntrypointUrl = model<string>('');
  protected readonly selectedEntrypointUrlValue = computed(() => {
    const selectedEntrypointUrl = this.selectedEntrypointUrl();
    const entrypointUrls = this.entrypointUrlValues();
    return selectedEntrypointUrl && entrypointUrls.includes(selectedEntrypointUrl) ? selectedEntrypointUrl : (entrypointUrls[0] ?? '');
  });

  curlCmd = computed(() => this.formatCurlCommandLine(this.selectedEntrypointUrlValue(), this.planSecurityInput(), this.primaryApiKey()));

  protected onApiKeysPageChange(page: number): void {
    this.requestedApiKeysCurrentPage.set(page);
  }

  protected onApiKeysPageSizeChange(pageSize: number): void {
    this.apiKeysPageSize.set(pageSize);
    this.requestedApiKeysCurrentPage.set(1);
  }

  protected revokeApiKeyRow(apiKey: ApiKeyTableRow): void {
    if (!this.canManageApiKeyInput() || this.isRevokingApiKey || this.isRevokeApiKeyDialogOpen || !apiKey.isActive || !apiKey.key) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@subscriptionDetailsCloseApiKeyDialogTitle:Close this API key?`,
      content: $localize`:@@subscriptionDetailsCloseApiKeyDialogContent:Applications using it will no longer be able to access the API.`,
      confirmLabel: $localize`:@@subscriptionDetailsCloseApiKeyDialogConfirm:Yes, revoke`,
      cancelLabel: $localize`:@@subscriptionDetailsCloseApiKeyDialogCancel:Cancel`,
    };

    this.isRevokeApiKeyDialogOpen = true;
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        tap(() => (this.isRevokeApiKeyDialogOpen = false)),
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.executeApiKeyRevocation(apiKey.key));
  }

  protected renewApiKey(): void {
    if (!this.canManageApiKeyInput() || this.isRenewingApiKey() || this.isRenewApiKeyDialogOpen() || !this.subscription?.id) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@subscriptionDetailsRenewApiKeyDialogTitle:Renew API Key?`,
      content: $localize`:@@subscriptionDetailsRenewApiKeyDialogContent:API Key renewal will eventually deprecate the current key`,
      confirmLabel: $localize`:@@subscriptionDetailsRenewApiKeyDialogConfirm:Yes, renew`,
      cancelLabel: $localize`:@@subscriptionDetailsRenewApiKeyDialogCancel:Cancel`,
    };

    this.isRenewApiKeyDialogOpen.set(true);
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        tap(() => this.isRenewApiKeyDialogOpen.set(false)),
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.executeApiKeyRenewal());
  }

  private isActiveApiKey(apiKey: SubscriptionDataKeys): boolean {
    if (apiKey.revoked_at) {
      return false;
    }

    if (!apiKey.expire_at) {
      return true;
    }

    return new Date(apiKey.expire_at).getTime() > Date.now();
  }

  private executeApiKeyRevocation(apiKey: string): void {
    const subscriptionId = this.subscriptionInput()?.id;
    if (!subscriptionId || !apiKey) {
      return;
    }

    this.isRevokingApiKey = true;
    this.subscriptionKeysService
      .revoke(subscriptionId, apiKey)
      .pipe(
        finalize(() => (this.isRevokingApiKey = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.apiKeyRevoked.emit();
          this.snackBar.open(
            $localize`:@@subscriptionDetailsApiAccessRevokeSuccess:API key revoked successfully. Applications using it will no longer be able to access the API.`,
            undefined,
            {
              ...this.defaultSnackBarOptions,
              panelClass: 'gio-snack-bar-success',
            },
          );
        },
        error: () => {
          this.snackBar.open(
            $localize`:@@subscriptionDetailsApiAccessRevokeError:Failed to revoke API key. Please try again.`,
            $localize`:@@subscriptionDetailsApiAccessRevokeErrorClose:Close`,
            {
              ...this.defaultSnackBarOptions,
              panelClass: 'gio-snack-bar-error',
            },
          );
        },
      });
  }

  private executeApiKeyRenewal(): void {
    const subscriptionId = this.subscription?.id;
    if (!subscriptionId) {
      return;
    }

    this.isRenewingApiKey.set(true);
    this.apiKeyRenewFeedback.set(undefined);
    this.subscriptionKeysService
      .renew(subscriptionId)
      .pipe(
        finalize(() => this.isRenewingApiKey.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.apiKeyRenewed.emit();
          this.apiKeyRenewFeedback.set({
            type: 'success',
            message: $localize`:@@subscriptionDetailsApiAccessRenewSuccess:API key renewed successfully. You can now use it to access the API.`,
          });
        },
        error: () => {
          this.apiKeyRenewFeedback.set({
            type: 'error',
            message: $localize`:@@subscriptionDetailsApiAccessRenewError:Failed to renew API key. Please try again.`,
          });
        },
      });
  }

  private formatCurlCommandLine(entrypointUrl: string, planSecurity: PlanSecurityEnum | undefined, apiKey?: string): string {
    if (!entrypointUrl) {
      return '';
    }
    let curlHeader = '';
    switch (planSecurity) {
      case 'JWT':
      case 'OAUTH2':
        curlHeader = '--header "Authorization: Bearer {{ ACCESS_TOKEN }}" ';
        break;
      case 'API_KEY':
        if (this.configService.configuration.portal?.apikeyHeader) {
          curlHeader = `--header "${this.configService.configuration.portal.apikeyHeader}: ${apiKey || '{{ API_KEY }}'}" `;
        }
        break;
    }

    return `curl ${curlHeader}${entrypointUrl}`;
  }
}
