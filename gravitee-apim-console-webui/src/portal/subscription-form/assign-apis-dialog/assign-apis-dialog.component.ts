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
import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { MappedApi, SubscriptionFormApisComponent } from '../subscription-form-apis/subscription-form-apis.component';

export interface AssignApisDialogData {
  /** The APIs the edited form is currently mapped to, saved or not. */
  selectedApis: MappedApi[];
  /** Name of the form each API outside the edited one is mapped to, by API id. */
  mappedElsewhere: Record<string, string>;
  canUpdate: boolean;
}

export type AssignApisDialogResult = MappedApi[];

/**
 * Assigns the APIs a subscription form is shown for. The mapping is edited on a draft: it only reaches the form —
 * and its unsaved changes — once applied, and is dropped on cancel. Saving it stays the form's own Save button.
 */
@Component({
  selector: 'assign-apis-dialog',
  imports: [MatButtonModule, MatDialogModule, GioBannerModule, SubscriptionFormApisComponent],
  templateUrl: './assign-apis-dialog.component.html',
  styleUrl: './assign-apis-dialog.component.scss',
})
export class AssignApisDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AssignApisDialogComponent, AssignApisDialogResult>);
  protected readonly data = inject<AssignApisDialogData>(MAT_DIALOG_DATA);

  protected readonly draft = signal<MappedApi[]>(this.data.selectedApis);

  protected toggle(api: MappedApi): void {
    this.draft.update(apis =>
      apis.some(selected => selected.id === api.id) ? apis.filter(selected => selected.id !== api.id) : [...apis, api],
    );
  }

  protected cancel(): void {
    this.dialogRef.close();
  }

  protected apply(): void {
    this.dialogRef.close(this.draft());
  }
}
