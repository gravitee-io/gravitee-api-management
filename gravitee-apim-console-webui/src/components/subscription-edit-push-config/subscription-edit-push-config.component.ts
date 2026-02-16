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
import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { GIO_DIALOG_WIDTH, GioClipboardModule } from '@gravitee/ui-particles-angular';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  SubscriptionEditPushConfigDialogComponent,
  SubscriptionEditPushConfigDialogData,
  SubscriptionEditPushConfigDialogResult,
} from '../subscription-edit-push-config-dialog/subscription-edit-push-config-dialog.component';
import { SubscriptionConsumerConfiguration } from '../../entities/management-api-v2';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

type PushConfigVM = {
  channel: string;
  callbackUrl: string;
  authType: string;
};

@Component({
  selector: 'subscription-edit-push-config',
  templateUrl: './subscription-edit-push-config.component.html',
  styleUrls: ['./subscription-edit-push-config.component.scss'],
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIcon, GioClipboardModule, MatDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubscriptionEditPushConfigComponent implements OnInit {
  private readonly matDialog = inject(MatDialog);
  private destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(GioPermissionService);

  @Input({ required: true })
  updatePermission: string;

  @Input()
  consumerConfiguration!: SubscriptionConsumerConfiguration;

  @Input()
  readonly: boolean;

  @Output()
  consumerConfigurationChange = new EventEmitter<Partial<SubscriptionConsumerConfiguration>>();

  canEdit = false;

  ngOnInit() {
    this.canEdit = !this.readonly && this.permissionService.hasAnyMatching([this.updatePermission]);
  }

  get pushConfig(): PushConfigVM {
    return {
      channel: this.consumerConfiguration?.channel || '',
      callbackUrl: this.consumerConfiguration?.entrypointConfiguration?.callbackUrl ?? 'unknown',
      authType: this.consumerConfiguration?.entrypointConfiguration?.auth?.type ?? 'none',
    };
  }

  openConsumerConfigurationDialog() {
    this.matDialog
      .open<SubscriptionEditPushConfigDialogComponent, SubscriptionEditPushConfigDialogData, SubscriptionEditPushConfigDialogResult>(
        SubscriptionEditPushConfigDialogComponent,
        {
          data: {
            readonly: !this.canEdit,
            consumerConfiguration: this.consumerConfiguration,
          },
          width: GIO_DIALOG_WIDTH.MEDIUM,
          role: 'dialog',
          id: 'subscription-edit-push-config-dialog',
        },
      )
      .afterClosed()
      .pipe(
        tap(result => {
          if (result) {
            this.consumerConfigurationChange.emit({
              channel: result.channel,
              entrypointConfiguration: result.entrypointConfiguration,
            });
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
