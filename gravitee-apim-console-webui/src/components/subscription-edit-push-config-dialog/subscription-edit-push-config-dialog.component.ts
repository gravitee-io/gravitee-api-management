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
import { ChangeDetectionStrategy, Component, inject, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GioFormJsonSchemaModule, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { AsyncPipe, NgIf } from '@angular/common';
import { Observable } from 'rxjs';

import { SubscriptionConsumerConfiguration } from '../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../services-ngx/connector-plugins-v2.service';

export interface SubscriptionEditPushConfigDialogData {
  consumerConfiguration: SubscriptionConsumerConfiguration;
  readonly: boolean;
}

export type SubscriptionEditPushConfigDialogResult = {
  channel: string;
  entrypointConfiguration: unknown;
};

@Component({
  selector: 'subscription-edit-push-config-dialog',
  templateUrl: './subscription-edit-push-config-dialog.component.html',
  styleUrls: ['./subscription-edit-push-config-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    FormsModule,
    GioFormJsonSchemaModule,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    ReactiveFormsModule,
    AsyncPipe,
    NgIf,
  ],
})
export class SubscriptionEditPushConfigDialogComponent {
  private readonly connectorPluginsV2Service = inject(ConnectorPluginsV2Service);

  public formGroup: FormGroup<{
    channel: FormControl<string>;
    entrypointConfiguration: FormControl<unknown>;
  }>;
  public readonly: boolean;

  jsonSchema$: Observable<GioJsonSchema>;

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: SubscriptionEditPushConfigDialogData,
    public dialogRef: MatDialogRef<SubscriptionEditPushConfigDialogComponent, SubscriptionEditPushConfigDialogResult>,
  ) {
    this.jsonSchema$ = this.connectorPluginsV2Service.getEntrypointPluginSubscriptionSchema(data.consumerConfiguration.entrypointId);

    this.readonly = data.readonly;

    this.formGroup = new FormGroup({
      channel: new FormControl({
        value: data.consumerConfiguration.channel,
        disabled: data.readonly,
      }),
      entrypointConfiguration: new FormControl({
        value: data.consumerConfiguration.entrypointConfiguration,
        disabled: data.readonly,
      }),
    });
  }

  onSave() {
    this.dialogRef.close({
      channel: this.formGroup.value.channel,
      entrypointConfiguration: this.formGroup.value.entrypointConfiguration,
    });
  }
}
