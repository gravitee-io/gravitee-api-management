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
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatTabsModule } from '@angular/material/tabs';
import { of } from 'rxjs';
import { catchError, map, startWith, tap } from 'rxjs/operators';

import { pathMappingValidators } from './path-mapping.validators';

import { Page } from '../../../entities/management-api-v2';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';

export interface ApiV4PathMappingsAddDialogData {
  apiId: string;
  existing: string[];
}

export type ApiV4PathMappingsAddDialogResult = { action: 'path'; path: string } | { action: 'import'; pageId: string; overwrite: boolean };

@Component({
  selector: 'api-v4-path-mappings-add-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatTabsModule,
  ],
  templateUrl: './api-v4-path-mappings-add-dialog.component.html',
  styleUrl: './api-v4-path-mappings-add-dialog.component.scss',
})
export class ApiV4PathMappingsAddDialogComponent {
  private readonly dialogData = inject<ApiV4PathMappingsAddDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ApiV4PathMappingsAddDialogComponent, ApiV4PathMappingsAddDialogResult>);
  private readonly apiDocumentationV2Service = inject(ApiDocumentationV2Service);

  protected readonly tabLabels = {
    path: 'Path',
    swagger: 'Swagger Document',
  };

  protected readonly selectedTab = signal<'path' | 'swagger'>('path');
  protected readonly errorStateMatcher: ErrorStateMatcher = {
    isErrorState: control => !!control && control.invalid && (control.dirty || control.touched || !!control.value),
  };

  protected readonly pathForm = new FormGroup({
    path: new FormControl('', {
      nonNullable: true,
      validators: pathMappingValidators(this.dialogData.existing),
    }),
  });

  protected readonly swaggerForm = new FormGroup({
    pageId: new FormControl('', { nonNullable: true }),
    overwrite: new FormControl(false, { nonNullable: true }),
  });

  protected readonly swaggerPagesStatus = signal<'loading' | 'ready' | 'failed'>('loading');

  protected readonly swaggerPages = toSignal(
    this.apiDocumentationV2Service.getApiPages(this.dialogData.apiId).pipe(
      map(result => result.pages.filter(page => page.type === 'SWAGGER')),
      tap(() => this.swaggerPagesStatus.set('ready')),
      catchError(() => {
        this.swaggerPagesStatus.set('failed');
        return of([] as Page[]);
      }),
    ),
    { initialValue: [] as Page[] },
  );

  private readonly pathStatus = toSignal(this.pathForm.statusChanges.pipe(startWith(this.pathForm.status)), {
    initialValue: this.pathForm.status,
  });

  private readonly swaggerPageId = toSignal(
    this.swaggerForm.controls.pageId.valueChanges.pipe(startWith(this.swaggerForm.controls.pageId.value)),
    { initialValue: this.swaggerForm.controls.pageId.value },
  );

  protected readonly isConfirmDisabled = computed(() => {
    if (this.selectedTab() === 'swagger') {
      return this.swaggerPageId().length === 0;
    }
    return this.pathStatus() !== 'VALID';
  });

  protected confirm(): void {
    if (this.selectedTab() === 'swagger') {
      const pageId = this.swaggerForm.controls.pageId.value;
      if (!pageId) {
        return;
      }
      this.dialogRef.close({
        action: 'import',
        pageId,
        overwrite: this.swaggerForm.controls.overwrite.value,
      });
      return;
    }
    if (this.pathForm.invalid) {
      this.pathForm.markAllAsTouched();
      return;
    }
    this.dialogRef.close({ action: 'path', path: this.pathForm.controls.path.value.trim() });
  }

  protected onSelectedTab(label: string): void {
    this.selectedTab.set(label === this.tabLabels.swagger ? 'swagger' : 'path');
  }
}
