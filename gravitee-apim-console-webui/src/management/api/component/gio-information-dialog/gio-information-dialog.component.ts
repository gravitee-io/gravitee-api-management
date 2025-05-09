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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { Information } from '../../../../entities/plugin/PluginMoreInformation';

export type GioConnectorDialogData = {
  name: string;
  information?: Information;
};

@Component({
  selector: 'gio-information-dialog',
  templateUrl: './gio-information-dialog.component.html',
  styleUrls: ['./gio-information-dialog.component.scss'],
  standalone: false,
})
export class GioInformationDialogComponent {
  public name: string;
  public information: Information | undefined;

  constructor(
    private readonly dialogRef: MatDialogRef<GioConnectorDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: GioConnectorDialogData,
  ) {
    this.name = dialogData.name;
    this.information = dialogData?.information;

    if (!this.information.description) {
      this.information.description = '🚧 More information coming soon 🚧';
    }
  }

  onClose() {
    this.dialogRef.close();
  }
}
