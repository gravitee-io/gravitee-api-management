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
import { AsyncPipe } from '@angular/common';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { GioPolicyGroupStudioComponent, PolicyDocumentationFetcher, PolicySchemaFetcher } from '@gravitee/ui-policy-studio-angular';
import { map } from 'rxjs/operators';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

import { IconService } from '../../../../../../services-ngx/icon.service';
import { PolicyV2Service } from '../../../../../../services-ngx/policy-v2.service';
import { SharedPolicyGroup } from '../../../../../../entities/management-api-v2';

export interface HistoryStudioDialogData {
  sharedPolicyGroup: SharedPolicyGroup;
}

export type HistoryStudioDialogResult = boolean;

@Component({
  selector: 'history-studio-dialog',
  templateUrl: './history-studio-dialog.component.html',
  styleUrls: ['./history-studio-dialog.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    AsyncPipe,
    GioLoaderModule,
    GioPolicyGroupStudioComponent,
    FormsModule,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    ReactiveFormsModule,
  ],
})
export class HistoryStudioDialogComponent {
  private readonly policyV2Service = inject(PolicyV2Service);
  private readonly iconService = inject(IconService);

  protected sharedPolicyGroup = this.data.sharedPolicyGroup;

  protected policySchemaFetcher: PolicySchemaFetcher = (policy) => this.policyV2Service.getSchema(policy.id);
  protected policyDocumentationFetcher: PolicyDocumentationFetcher = (policy) => this.policyV2Service.getDocumentation(policy.id);
  protected policies$ = this.policyV2Service
    .list()
    .pipe(map((policies) => policies.map((policy) => ({ ...policy, icon: this.iconService.registerSvg(policy.id, policy.icon) }))));

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: HistoryStudioDialogData,
    public dialogRef: MatDialogRef<HistoryStudioDialogComponent, HistoryStudioDialogResult>,
  ) {}
}
