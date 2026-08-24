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
import { Component, computed, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { NavigationItemSourceEditorComponent } from '../navigation-item-source-editor/navigation-item-source-editor.component';
import { PortalNavigationItemSource } from '../../../entities/management-api-v2';

export interface ImportNavigationDialogResult {
  title: string;
  source: PortalNavigationItemSource;
}

@Component({
  selector: 'import-navigation-dialog',
  templateUrl: './import-navigation-dialog.component.html',
  styleUrls: ['./import-navigation-dialog.component.scss'],
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    GioBannerModule,
    NavigationItemSourceEditorComponent,
  ],
})
export class ImportNavigationDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ImportNavigationDialogComponent, ImportNavigationDialogResult>);

  readonly titleControl = new FormControl<string>('', { nonNullable: true, validators: Validators.required });

  private readonly sourceEditor = viewChild(NavigationItemSourceEditorComponent);
  private readonly titleStatus = toSignal(this.titleControl.statusChanges, { initialValue: this.titleControl.status });

  readonly importDisabled = computed(() => {
    const editor = this.sourceEditor();
    return this.titleStatus() !== 'VALID' || !editor || editor.saveDisabled();
  });

  onImport() {
    const editor = this.sourceEditor();
    const source = editor?.buildSource();
    if (this.titleControl.invalid || !editor || !source) {
      return;
    }
    // A blank path used to import an empty folder; for a tree import it can only mean the repository root
    const schemaProperties = Object.keys(editor.selectedSchema()?.properties ?? {});
    const configuration = { ...((source.configuration ?? {}) as Record<string, unknown>) };
    for (const key of schemaProperties.filter(property => property === 'filepath' || property === 'path')) {
      const value = configuration[key];
      if (value == null || String(value).trim() === '') {
        configuration[key] = '/';
      }
    }
    source.configuration = configuration;
    this.dialogRef.close({ title: this.titleControl.value.trim(), source });
  }

  onCancel() {
    this.dialogRef.close();
  }
}
