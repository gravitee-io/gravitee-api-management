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
import { Component, computed, effect, EventEmitter, Input, model, Output } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon, MatIconRegistry } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { DomSanitizer } from '@angular/platform-browser';
import { filter } from 'rxjs';
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
  isActive: boolean;
  key: string;
  createdAt?: string;
  revokedAt?: string;
}

const GIO_CHECK_CIRCLED_OUTLINE_ICON = `
  <svg fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path fill="currentColor" d="M15.663 3.773A9 9 0 1 0 21 12v-.919a1 1 0 0 1 2 0V12a11.002 11.002 0 0 1-21.066 4.432A11 11 0 0 1 16.477 1.947a.999.999 0 0 1-.037 1.863 1 1 0 0 1-.777-.037Z"/>
    <path fill="currentColor" d="M22.707 3.293a1 1 0 0 1 0 1.414l-10 10.01a1 1 0 0 1-1.414 0l-3-3a1 1 0 1 1 1.414-1.414L12 12.595l9.293-9.302a1 1 0 0 1 1.414 0Z"/>
  </svg>
`;

const GIO_X_CIRCLE_ICON = `
  <svg fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path fill="currentColor" fill-rule="evenodd" d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18ZM1 12C1 5.925 5.925 1 12 1s11 4.925 11 11-4.925 11-11 11S1 18.075 1 12Z" clip-rule="evenodd"/>
    <path fill="currentColor" fill-rule="evenodd" d="M15.707 8.293a1 1 0 0 1 0 1.414l-6 6a1 1 0 0 1-1.414-1.414l6-6a1 1 0 0 1 1.414 0Z" clip-rule="evenodd"/>
    <path fill="currentColor" fill-rule="evenodd" d="M8.293 8.293a1 1 0 0 1 1.414 0l6 6a1 1 0 0 1-1.414 1.414l-6-6a1 1 0 0 1 0-1.414Z" clip-rule="evenodd"/>
  </svg>
`;

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
    MatTooltip,
    CopyCodeIconComponent,
    PaginatedTableComponent,
    TableCellDirective,
  ],
  templateUrl: './api-access.component.html',
  styleUrl: './api-access.component.scss',
})
export class ApiAccessComponent {
  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  subscription?: Subscription;

  @Input()
  set apiKeys(value: SubscriptionDataKeys[] | undefined) {
    this._apiKeys = value ?? [];
    this.apiKeyRows = this._apiKeys.map(apiKey => {
      const isActive = this.isActiveApiKey(apiKey);
      return {
        statusIcon: isActive ? 'gio:check-circled-outline' : 'gio:x-circle',
        statusLabel: isActive
          ? $localize`:@@subscriptionDetailsApiAccessApiKeysStatusActive:Active API key`
          : $localize`:@@subscriptionDetailsApiAccessApiKeysStatusInactive:Inactive API key`,
        isActive,
        key: apiKey.key ?? '',
        createdAt: apiKey.created_at,
        revokedAt: apiKey.revoked_at ?? apiKey.expire_at,
      };
    });
    this.hasActiveApiKey = !!this.activeApiKey;
    this.totalApiKeyElements = this.apiKeyRows.length;
    this.apiKeysCurrentPage = 1;
    this.updateDisplayedApiKeyRows();
  }

  @Input()
  apiKeyConfigUsername?: string;

  @Input()
  apiType?: ApiType;

  @Input()
  entrypointUrls?: string[];

  @Input()
  clientId?: string;

  @Input()
  clientSecret?: string;

  @Input()
  canManageSubscription = false;

  @Output()
  revokeApiKey = new EventEmitter<void>();

  protected readonly apiKeysColumns: TableColumn[] = [
    { id: 'status', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnStatus:Status` },
    { id: 'key', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnKey:Key` },
    { id: 'createdAt', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnCreatedAt:Created At`, type: 'date-time' },
    { id: 'revokedAt', label: $localize`:@@subscriptionDetailsApiAccessApiKeysColumnRevokedAt:Revoked/Expired At`, type: 'date-time' },
    { id: 'revoke', label: '' },
  ];
  protected readonly apiKeysPageSizeOptions = [5, 10, 25];

  protected apiKeyRows: ApiKeyTableRow[] = [];
  protected displayedApiKeyRows: ApiKeyTableRow[] = [];
  protected totalApiKeyElements = 0;
  protected apiKeysCurrentPage = 1;
  protected apiKeysPageSize = 5;
  protected isRevokingApiKey = false;
  protected hasActiveApiKey = false;

  private _apiKeys: SubscriptionDataKeys[] = [];

  selectedEntrypointUrl = model<string>('');

  curlCmd = computed(() => this.formatCurlCommandLine(this.selectedEntrypointUrl(), this.planSecurity, this.primaryApiKey));

  constructor(
    private readonly configService: ConfigService,
    private readonly subscriptionKeysService: SubscriptionKeysService,
    private readonly dialog: MatDialog,
    private readonly matIconRegistry: MatIconRegistry,
    private readonly sanitizer: DomSanitizer,
  ) {
    this.registerApiKeyStatusIcons();

    effect(() => {
      this.selectedEntrypointUrl.set(this.entrypointUrls?.length ? this.entrypointUrls[0] : this.selectedEntrypointUrl());
    });
  }

  protected onApiKeysPageChange(page: number): void {
    this.apiKeysCurrentPage = page;
    this.updateDisplayedApiKeyRows();
  }

  protected onApiKeysPageSizeChange(pageSize: number): void {
    this.apiKeysPageSize = pageSize;
    this.apiKeysCurrentPage = 1;
    this.updateDisplayedApiKeyRows();
  }

  protected get primaryApiKey(): string {
    return this.activeApiKey?.key ?? this._apiKeys[0]?.key ?? '';
  }

  protected get resolvedApiKeyConfigUsername(): string {
    return this.apiKeyConfigUsername ?? this.activeApiKey?.hash ?? this._apiKeys[0]?.hash ?? '';
  }

  protected get shouldShowApiAccessContent(): boolean {
    return this.planSecurity !== 'API_KEY' || this.subscription?.status !== 'ACCEPTED' || this.hasActiveApiKey;
  }

  protected revokeApiKeyRow(apiKey: ApiKeyTableRow): void {
    if (!this.canManageSubscription || this.isRevokingApiKey || !apiKey.isActive || !apiKey.key) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@subscriptionDetailsCloseApiKeyDialogTitle:Close this API key?`,
      content: $localize`:@@subscriptionDetailsCloseApiKeyDialogContent:Applications using it will no longer be able to access the API.`,
      confirmLabel: $localize`:@@subscriptionDetailsCloseApiKeyDialogConfirm:Yes, revoke`,
      cancelLabel: $localize`:@@subscriptionDetailsCloseApiKeyDialogCancel:Cancel`,
    };

    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(filter(confirmed => !!confirmed))
      .subscribe(() => this.executeApiKeyRevocation(apiKey.key));
  }

  private get activeApiKey(): SubscriptionDataKeys | undefined {
    return this._apiKeys.find(apiKey => this.isActiveApiKey(apiKey));
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

  private registerApiKeyStatusIcons(): void {
    this.matIconRegistry.addSvgIconLiteralInNamespace(
      'gio',
      'check-circled-outline',
      this.sanitizer.bypassSecurityTrustHtml(GIO_CHECK_CIRCLED_OUTLINE_ICON), //NOSONAR
    );
    this.matIconRegistry.addSvgIconLiteralInNamespace('gio', 'x-circle', this.sanitizer.bypassSecurityTrustHtml(GIO_X_CIRCLE_ICON)); //NOSONAR
  }

  private executeApiKeyRevocation(apiKey: string): void {
    const subscriptionId = this.subscription?.id;
    if (!subscriptionId || !apiKey) {
      return;
    }

    this.isRevokingApiKey = true;
    this.subscriptionKeysService
      .revoke(subscriptionId, apiKey)
      .pipe(finalize(() => (this.isRevokingApiKey = false)))
      .subscribe({
        next: () => this.revokeApiKey.emit(),
        error: () => {
          // no-op: keep UI stable and release loading state in finalize
        },
      });
  }

  private updateDisplayedApiKeyRows(): void {
    const startIndex = (this.apiKeysCurrentPage - 1) * this.apiKeysPageSize;
    const endIndex = startIndex + this.apiKeysPageSize;
    this.displayedApiKeyRows = this.apiKeyRows.slice(startIndex, endIndex);
  }

  private formatCurlCommandLine(entrypointUrl: string, planSecurity: PlanSecurityEnum, apiKey?: string): string {
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
