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
import { Component, Inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';

import { PageComponent } from '../../../../../components/page/page.component';
import { Page } from '../../../../../entities/page/page';

export interface TermsAndConditionsDialogData {
  page: Page;
  apiId: string;
}

@Component({
  selector: 'app-terms-and-conditions-dialog',
  standalone: true,
  imports: [MatDialogTitle, MatDialogContent, MatDialogActions, MatButton, PageComponent],
  templateUrl: './terms-and-conditions-dialog.component.html',
})
export class TermsAndConditionsDialogComponent {
  page: Page;
  apiId: string;

  constructor(
    private readonly dialogRef: MatDialogRef<TermsAndConditionsDialogData, boolean>,
    @Inject(MAT_DIALOG_DATA) dialogData: TermsAndConditionsDialogData,
  ) {
    this.page = dialogData.page;
    this.apiId = dialogData.apiId;
  }

  onCancel() {
    this.dialogRef.close();
  }

  onAccept() {
    this.dialogRef.close(true);
  }
}
