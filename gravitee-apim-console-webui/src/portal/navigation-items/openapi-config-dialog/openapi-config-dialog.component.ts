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
import { Component, computed, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';

import {
  OpenApiDocExpansion,
  OpenApiViewer,
  OpenApiViewerConfiguration,
} from '../../../entities/management-api-v2/portalPageContent/openApiViewerConfiguration';

export interface OpenApiConfigDialogData {
  configuration: Partial<OpenApiViewerConfiguration>;
}

interface OpenApiFormControls {
  viewer: FormControl<OpenApiViewer>;
  tryItURL: FormControl<string>;
  tryIt: FormControl<boolean>;
  disableSyntaxHighlight: FormControl<boolean>;
  tryItAnonymous: FormControl<boolean>;
  showURL: FormControl<boolean>;
  entrypointsAsServers: FormControl<boolean>;
  contextPathAsServerPath: FormControl<boolean>;
  displayOperationId: FormControl<boolean>;
  usePkce: FormControl<boolean>;
  docExpansion: FormControl<OpenApiDocExpansion>;
  enableFiltering: FormControl<boolean>;
  showExtensions: FormControl<boolean>;
  showCommonExtensions: FormControl<boolean>;
  maxDisplayedTags: FormControl<number | null>;
}

function parseBoolean(value: string | boolean | undefined): boolean {
  return value === 'true' || value === true;
}

function parseNumber(value: string | number | undefined): number | null {
  if (value === undefined || value === '' || value === null) return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

@Component({
  selector: 'openapi-config-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    GioFormSlideToggleModule,
  ],
  templateUrl: './openapi-config-dialog.component.html',
  styleUrls: ['./openapi-config-dialog.component.scss'],
})
export class OpenApiConfigDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<OpenApiConfigDialogComponent, OpenApiViewerConfiguration>);
  private readonly data: OpenApiConfigDialogData = inject(MAT_DIALOG_DATA);

  private readonly configuration = this.data.configuration ?? {};

  readonly form = new FormGroup<OpenApiFormControls>({
    viewer: new FormControl(this.configuration.viewer ?? OpenApiViewer.Swagger, { nonNullable: true }),
    tryItURL: new FormControl(this.configuration.tryItURL ?? '', {
      nonNullable: true,
      validators: [Validators.pattern(/^$|^https?:\/\/\S+/)],
    }),
    tryIt: new FormControl(parseBoolean(this.configuration.tryIt), { nonNullable: true }),
    disableSyntaxHighlight: new FormControl(parseBoolean(this.configuration.disableSyntaxHighlight), { nonNullable: true }),
    tryItAnonymous: new FormControl(parseBoolean(this.configuration.tryItAnonymous), { nonNullable: true }),
    showURL: new FormControl(parseBoolean(this.configuration.showURL), { nonNullable: true }),
    entrypointsAsServers: new FormControl(parseBoolean(this.configuration.entrypointsAsServers), { nonNullable: true }),
    contextPathAsServerPath: new FormControl(parseBoolean(this.configuration.contextPathAsServerPath), { nonNullable: true }),
    displayOperationId: new FormControl(parseBoolean(this.configuration.displayOperationId), { nonNullable: true }),
    usePkce: new FormControl(parseBoolean(this.configuration.usePkce), { nonNullable: true }),
    docExpansion: new FormControl(this.configuration.docExpansion ?? OpenApiDocExpansion.None, { nonNullable: true }),
    enableFiltering: new FormControl(parseBoolean(this.configuration.enableFiltering), { nonNullable: true }),
    showExtensions: new FormControl(parseBoolean(this.configuration.showExtensions), { nonNullable: true }),
    showCommonExtensions: new FormControl(parseBoolean(this.configuration.showCommonExtensions), { nonNullable: true }),
    maxDisplayedTags: new FormControl<number | null>(parseNumber(this.configuration.maxDisplayedTags)),
  });
  readonly viewerEnum = OpenApiViewer;
  readonly docExpansionEnum = OpenApiDocExpansion;

  private readonly viewer = toSignal(this.form.controls.viewer.valueChanges, { initialValue: this.form.controls.viewer.value });
  private readonly entrypointsAsServers = toSignal(this.form.controls.entrypointsAsServers.valueChanges, {
    initialValue: this.form.controls.entrypointsAsServers.value,
  });

  readonly isSwaggerViewer = computed(() => this.viewer() === OpenApiViewer.Swagger);
  private readonly shouldUseEntrypointsAsServers = computed(() => this.isSwaggerViewer() && this.entrypointsAsServers());

  private readonly syncEntrypointsAsServersEffect = effect(() => {
    const tryItURLControl = this.form.controls.tryItURL;

    if (this.shouldUseEntrypointsAsServers()) {
      tryItURLControl.setValue('', { emitEvent: false });
      tryItURLControl.disable({ emitEvent: false });
    } else {
      tryItURLControl.enable({ emitEvent: false });
    }
  });

  onSave(): void {
    const formValue = this.form.getRawValue();
    this.dialogRef.close({
      viewer: formValue.viewer,
      tryItURL: formValue.tryItURL,
      tryIt: formValue.tryIt,
      disableSyntaxHighlight: formValue.disableSyntaxHighlight,
      tryItAnonymous: formValue.tryItAnonymous,
      showURL: formValue.showURL,
      entrypointsAsServers: formValue.entrypointsAsServers,
      contextPathAsServerPath: formValue.contextPathAsServerPath,
      displayOperationId: formValue.displayOperationId,
      usePkce: formValue.usePkce,
      docExpansion: formValue.docExpansion,
      enableFiltering: formValue.enableFiltering,
      showExtensions: formValue.showExtensions,
      showCommonExtensions: formValue.showCommonExtensions,
      maxDisplayedTags: formValue.maxDisplayedTags,
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
