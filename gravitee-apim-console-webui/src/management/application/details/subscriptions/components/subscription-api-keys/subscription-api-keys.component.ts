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
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { filter, switchMap, tap } from 'rxjs/operators';
import { BehaviorSubject } from 'rxjs';

import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { ApiKeyMode } from '../../../../../../entities/application/Application';
import {
  ApiPortalSubscriptionRenewApiKeyDialogComponent,
  ApiPortalSubscriptionRenewApiKeyDialogData,
  ApiPortalSubscriptionRenewApiKeyDialogResult,
} from '../../../../../api/subscriptions/components/dialogs/renew-api-key/api-portal-subscription-renew-api-key-dialog.component';
import {
  ApiPortalSubscriptionExpireApiKeyDialogComponent,
  ApiPortalSubscriptionExpireApiKeyDialogData,
  ApiPortalSubscriptionExpireApiKeyDialogResult,
} from '../../../../../api/subscriptions/components/dialogs/expire-api-key/api-portal-subscription-expire-api-key-dialog.component';

export interface SubscriptionApiKeysApiKey {
  id: string;
  key: string;
  createdAt: Date;
  endDate?: Date;
  isRevoked: boolean;
  isExpired: boolean;
}

@Component({
  selector: 'subscription-api-keys',
  templateUrl: './subscription-api-keys.component.html',
  styleUrls: ['./subscription-api-keys.component.scss'],
})
export class SubscriptionApiKeysComponent implements OnInit, OnChanges {
  @Input() apiKeys: SubscriptionApiKeysApiKey[] = [];
  @Input() apiKeyMode: ApiKeyMode;
  @Input() canRenew: boolean = true;
  @Input() canRevoke: boolean = true;
  @Input() canExpire: boolean = true;
  @Input() customApiKeyAllowed: boolean = false;
  @Output() renewApiKey = new EventEmitter<{ customApiKey?: string }>();
  @Output() revokeApiKey = new EventEmitter<string>();
  @Output() expireApiKey = new EventEmitter<{ apiKeyId: string; expireAt: Date }>();

  displayedColumns = ['key', 'createdAt', 'endDate', 'actions'];
  apiKeysDataSource: SubscriptionApiKeysApiKey[] = [];
  private apiKeysSubject = new BehaviorSubject<SubscriptionApiKeysApiKey[]>([]);

  constructor(
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.apiKeysSubject.subscribe((apiKeys) => {
      this.apiKeysDataSource = apiKeys;
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiKeys) {
      this.apiKeysSubject.next(this.apiKeys);
    }
  }

  onRenewApiKey() {
    this.matDialog
      .open<
        ApiPortalSubscriptionRenewApiKeyDialogComponent,
        ApiPortalSubscriptionRenewApiKeyDialogData,
        ApiPortalSubscriptionRenewApiKeyDialogResult
      >(ApiPortalSubscriptionRenewApiKeyDialogComponent, {
        width: '500px',
        data: {
          customApiKeyAllowed: this.customApiKeyAllowed,
        },
        role: 'alertdialog',
        id: 'renewApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        tap((result) => {
          this.renewApiKey.emit({ customApiKey: result.customApiKey });
        }),
      )
      .subscribe();
  }

  onRevokeApiKey(apiKeyId: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Revoke API Key',
          content: `Are you sure you want to revoke this API Key?`,
          confirmButton: 'Revoke',
        },
        role: 'alertdialog',
        id: 'revokeApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === 'confirmed'),
        tap(() => {
          this.revokeApiKey.emit(apiKeyId);
        }),
      )
      .subscribe();
  }

  onExpireApiKey(apiKey: SubscriptionApiKeysApiKey) {
    this.matDialog
      .open<
        ApiPortalSubscriptionExpireApiKeyDialogComponent,
        ApiPortalSubscriptionExpireApiKeyDialogData,
        ApiPortalSubscriptionExpireApiKeyDialogResult
      >(ApiPortalSubscriptionExpireApiKeyDialogComponent, {
        width: '500px',
        data: {
          expirationDate: apiKey.endDate,
        },
        role: 'alertdialog',
        id: 'expireApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        tap((result) => {
          this.expireApiKey.emit({ apiKeyId: apiKey.id, expireAt: result.expirationDate });
        }),
      )
      .subscribe();
  }

  isSharedApiKeyMode() {
    return this.apiKeyMode === 'SHARED';
  }
}
