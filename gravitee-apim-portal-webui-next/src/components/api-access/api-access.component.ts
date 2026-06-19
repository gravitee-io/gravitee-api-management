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
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { filter, tap } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { ApiKeyFeedback, ApiKeysListComponent } from './api-keys-list/api-keys-list.component';
import { NativeKafkaApiAccessComponent } from './native-kafka-api-access/native-kafka-api-access.component';
import { ApiType } from '../../entities/api/api';
import { PlanSecurityEnum } from '../../entities/plan/plan';
import { isActiveApiKey, Subscription, SubscriptionDataKeys } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { SubscriptionKeysService } from '../../services/subscription-keys.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../confirm-dialog/confirm-dialog.component';
import { CopyCodeIconComponent } from '../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
import { CopyCodeComponent } from '../copy-code/copy-code.component';

@Component({
  selector: 'app-api-access',
  imports: [
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatButton,
    ApiKeysListComponent,
    CopyCodeComponent,
    NativeKafkaApiAccessComponent,
    MatFormFieldModule,
    MatSelectModule,
    MatTooltip,
    CopyCodeIconComponent,
  ],
  templateUrl: './api-access.component.html',
  styleUrl: './api-access.component.scss',
})
export class ApiAccessComponent {
  private readonly configService = inject(ConfigService);
  private readonly subscriptionKeysService = inject(SubscriptionKeysService);
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

  private readonly planSecurityInput = signal<PlanSecurityEnum | undefined>(undefined);
  private readonly subscriptionInput = signal<Subscription | undefined>(undefined);
  private readonly apiKeysInput = signal<SubscriptionDataKeys[]>([]);
  private readonly apiTypeInput = signal<ApiType | undefined>(undefined);
  private readonly entrypointUrlsInput = signal<string[] | undefined>(undefined);
  private readonly clientIdInput = signal<string | undefined>(undefined);
  private readonly clientSecretInput = signal<string | undefined>(undefined);
  private readonly canManageApiKeyInput = signal(false);
  protected readonly apiKeyFeedback = signal<ApiKeyFeedback | undefined>(undefined);
  protected readonly isRenewingApiKey = signal(false);
  protected readonly isRenewApiKeyDialogOpen = signal(false);

  protected readonly entrypointUrlValues = computed(() => this.entrypointUrlsInput() ?? []);
  protected readonly activeApiKey = computed(() => this.apiKeysInput().find(apiKey => isActiveApiKey(apiKey)));
  protected readonly hasActiveApiKey = computed(() => !!this.activeApiKey());
  protected readonly primaryApiKey = computed(() => this.activeApiKey()?.key ?? '');
  protected readonly resolvedApiKeyConfigUsername = computed(() => this.activeApiKey()?.hash ?? '');
  protected readonly hasUsableApiKeyAccess = computed(
    () => this.planSecurityInput() !== 'API_KEY' || this.subscriptionInput()?.status !== 'ACCEPTED' || this.hasActiveApiKey(),
  );
  protected readonly shouldShowApiAccessContent = computed(() => this.entrypointUrlValues().length > 0 && this.hasUsableApiKeyAccess());
  protected readonly shouldShowNativeAccessContent = computed(() => this.hasUsableApiKeyAccess());
  protected readonly hasApiAccessContent = computed(() => {
    if (this.apiTypeInput() === 'NATIVE') {
      return true;
    }
    const security = this.planSecurityInput();
    if (security === 'OAUTH2' || security === 'JWT' || security === 'API_KEY') {
      return true;
    }
    return this.shouldShowApiAccessContent();
  });
  protected readonly shouldRenderApiAccessCard = computed(() => {
    const isAccessVisible = this.subscriptionInput()?.status === 'ACCEPTED' || this.planSecurityInput() === 'KEY_LESS';
    return isAccessVisible ? this.hasApiAccessContent() : true;
  });
  protected readonly isRevokingApiKey = signal(false);
  protected readonly isRevokeApiKeyDialogOpen = signal(false);

  selectedEntrypointUrl = model<string>('');
  protected readonly selectedEntrypointUrlValue = computed(() => {
    const selectedEntrypointUrl = this.selectedEntrypointUrl();
    const entrypointUrls = this.entrypointUrlValues();
    return selectedEntrypointUrl && entrypointUrls.includes(selectedEntrypointUrl) ? selectedEntrypointUrl : (entrypointUrls[0] ?? '');
  });

  curlCmd = computed(() => this.formatCurlCommandLine(this.selectedEntrypointUrlValue(), this.planSecurityInput(), this.primaryApiKey()));

  protected revokeApiKeyRow(apiKey: SubscriptionDataKeys): void {
    const key = apiKey.key;
    if (!this.canManageApiKeyInput() || this.isRevokingApiKey() || this.isRevokeApiKeyDialogOpen() || !isActiveApiKey(apiKey) || !key) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@apiKeyRevokeDialogTitle:Close this API key?`,
      content: $localize`:@@apiKeyRevokeDialogContent:Applications using it will no longer be able to access the API.`,
      confirmLabel: $localize`:@@apiKeyRevokeDialogConfirm:Yes, revoke`,
      cancelLabel: $localize`:@@apiKeyRevokeDialogCancel:Cancel`,
    };

    this.isRevokeApiKeyDialogOpen.set(true);
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        tap(() => this.isRevokeApiKeyDialogOpen.set(false)),
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.executeApiKeyRevocation(key));
  }

  protected renewApiKey(): void {
    if (!this.canManageApiKeyInput() || this.isRenewingApiKey() || this.isRenewApiKeyDialogOpen() || !this.subscription?.id) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@apiKeyRenewDialogTitle:Renew API Key?`,
      content: $localize`:@@apiKeyRenewDialogContent:API Key renewal will eventually deprecate the current key`,
      confirmLabel: $localize`:@@apiKeyRenewDialogConfirm:Yes, renew`,
      cancelLabel: $localize`:@@apiKeyRenewDialogCancel:Cancel`,
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

  private executeApiKeyRevocation(apiKey: string): void {
    const subscriptionId = this.subscriptionInput()?.id;
    if (!subscriptionId || !apiKey) {
      return;
    }

    this.isRevokingApiKey.set(true);
    this.apiKeyFeedback.set(undefined);
    this.subscriptionKeysService
      .revoke(subscriptionId, apiKey)
      .pipe(
        finalize(() => this.isRevokingApiKey.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.apiKeyRevoked.emit();
          this.apiKeyFeedback.set({
            type: 'success',
            message: $localize`:@@apiKeyRevokeSuccess:API key revoked successfully. Applications using it will no longer be able to access the API.`,
          });
        },
        error: () => {
          this.apiKeyFeedback.set({
            type: 'error',
            message: $localize`:@@apiKeyRevokeError:Failed to revoke API key. Please try again.`,
          });
        },
      });
  }

  private executeApiKeyRenewal(): void {
    const subscriptionId = this.subscription?.id;
    if (!subscriptionId) {
      return;
    }

    this.isRenewingApiKey.set(true);
    this.apiKeyFeedback.set(undefined);
    this.subscriptionKeysService
      .renew(subscriptionId)
      .pipe(
        finalize(() => this.isRenewingApiKey.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.apiKeyRenewed.emit();
          this.apiKeyFeedback.set({
            type: 'success',
            message: $localize`:@@apiKeyRenewSuccess:API key renewed successfully. You can now use it to access the API.`,
          });
        },
        error: () => {
          this.apiKeyFeedback.set({
            type: 'error',
            message: $localize`:@@apiKeyRenewError:Failed to renew API key. Please try again.`,
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
