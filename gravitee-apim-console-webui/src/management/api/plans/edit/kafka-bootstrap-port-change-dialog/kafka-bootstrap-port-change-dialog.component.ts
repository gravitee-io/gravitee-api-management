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
import { Component, Inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

export interface KafkaBootstrapPortChangeDialogData {
  host?: string;
  oldPort?: number;
  newPort?: number;
}

@Component({
  selector: 'kafka-bootstrap-port-change-dialog',
  standalone: true,
  imports: [GioBannerModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton, MatCheckbox],
  templateUrl: './kafka-bootstrap-port-change-dialog.component.html',
})
export class KafkaBootstrapPortChangeDialogComponent {
  public isConfirmed = false;

  constructor(
    public readonly dialogRef: MatDialogRef<KafkaBootstrapPortChangeDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: KafkaBootstrapPortChangeDialogData,
  ) {}
}
