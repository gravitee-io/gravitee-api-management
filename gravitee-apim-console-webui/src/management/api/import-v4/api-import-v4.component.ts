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
import { ChangeDetectionStrategy, Component, DestroyRef, inject, Input, OnInit, Optional } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { GioBannerModule, GioFormSelectionInlineModule, GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { ApiImportFilePickerComponent } from '../component/api-import-file-picker/api-import-file-picker.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApiV4 } from '../../../entities/management-api-v2';
import { PolicyV2Service } from '../../../services-ngx/policy-v2.service';

export interface ApiImportV4DialogData {
  apiId?: string;
  isUpdateMode?: boolean;
}

@Component({
  selector: 'api-import-v4',
  imports: [
    FormsModule,
    GioBannerModule,
    GioFormSelectionInlineModule,
    GioIconsModule,
    MatButtonModule,
    MatCardModule,
    MatTooltipModule,
    ReactiveFormsModule,
    RouterModule,
    ApiImportFilePickerComponent,
    GioFormSlideToggleModule,
    MatSlideToggle,
    MatDialogModule,
  ],
  templateUrl: './api-import-v4.component.html',
  styleUrl: './api-import-v4.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiImportV4Component implements OnInit {
  private apiV2Service = inject(ApiV2Service);
  private snackBarService = inject(SnackBarService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private policyV2Service = inject(PolicyV2Service);
  private dialogRefOptional = inject(MatDialogRef<ApiImportV4Component>, { optional: true });
  private dialogData = inject<ApiImportV4DialogData>(MAT_DIALOG_DATA, { optional: true });
  private importFileContent: string;
  private unsubscribe$: Subject<void> = new Subject<void>();

  protected importType: string;
  protected isUpdateMode = false;
  protected apiId: string | undefined;
  protected dialogRef: MatDialogRef<ApiImportV4Component> | null = null;
  protected formats = [
    { value: 'gravitee', label: 'Gravitee definition', icon: 'gio:gravitee' },
    { value: 'openapi', label: 'OpenAPI specification', icon: 'gio:open-api' },
  ];
  protected sources = [
    { value: 'local', label: 'Local file', icon: 'gio:laptop', disabled: false },
    { value: 'remote', label: 'Remote source', icon: 'gio:language', disabled: true },
  ];
  protected form = new FormGroup(
    {
      format: new FormControl('gravitee', [Validators.required]),
      source: new FormControl('local', [Validators.required]),
      withDocumentation: new FormControl({ value: false, disabled: true }),
      withOASValidationPolicy: new FormControl({ value: false, disabled: true }),
    },
    [this.fileFormatValidator()],
  );

  protected hasOasValidationPolicy = toSignal(
    this.policyV2Service.list().pipe(map((policies) => policies.some((policy) => policy.id === 'oas-validation'))),
  );

  ngOnInit(): void {
    if (this.dialogData) {
      this.isUpdateMode = this.dialogData.isUpdateMode ?? false;
      this.apiId = this.dialogData.apiId;
    }
    if (this.dialogRefOptional) {
      this.dialogRef = this.dialogRefOptional;
    }
    this.form.controls['format'].valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
      if (value !== 'openapi') {
        this.form.patchValue({ withDocumentation: false, withOASValidationPolicy: false });
        this.form.get('withDocumentation').disable();
        this.form.get('withOASValidationPolicy').disable();
      } else {
        this.form.patchValue({ withDocumentation: true, withOASValidationPolicy: true });
        this.form.get('withDocumentation').enable();
        this.form.get('withOASValidationPolicy').enable();
      }
    });
  }

  protected onImportFile({ importFileContent, importType }: { importFileContent: string; importType: string }) {
    this.importType = importType;
    this.importFileContent = importFileContent;
    this.form.updateValueAndValidity();
  }

  protected import() {
    let result: Observable<ApiV4>;
    if (this.form.controls.source.value === 'local' && this.form.controls.format.value === 'gravitee' && this.importType === 'MAPI_V2') {
      if (this.isUpdateMode && this.apiId) {
        result = this.apiV2Service.updateApiWithDefinition(this.apiId, this.importFileContent);
      } else {
        result = this.apiV2Service.import(this.importFileContent);
      }
    } else if (
      this.form.controls.source.value === 'local' &&
      this.form.controls.format.value === 'openapi' &&
      this.importType === 'SWAGGER'
    ) {
      const descriptor = {
        payload: this.importFileContent,
        withDocumentation: this.form.value.withDocumentation,
        withOASValidationPolicy: this.form.value.withOASValidationPolicy,
      };
      if (this.isUpdateMode && this.apiId) {
        result = this.apiV2Service.updateApiWithSwagger(this.apiId, descriptor);
      } else {
        result = this.apiV2Service.importSwaggerApi(descriptor);
      }
    } else {
      this.snackBarService.error('Unsupported type for V4 API import');
      return;
    }

    result
      .pipe(
        tap((api) => {
          const successMessage = this.isUpdateMode ? 'API updated successfully' : 'API imported successfully';
          this.snackBarService.success(successMessage);
          if (this.dialogRef) {
            this.dialogRef.close(api);
          } else {
            this.router.navigate([`../../${api.id}`], { relativeTo: this.activatedRoute });
          }
        }),
        catchError(({ error }) => {
          const errorMessage = this.isUpdateMode
            ? error.message ?? 'An error occurred while updating the API'
            : error.message ?? 'An error occurred while importing the API';
          this.snackBarService.error(errorMessage);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  fileFormatValidator(): ValidatorFn {
    return (formGroup: FormGroup) => {
      const format = formGroup.get('format');
      if (
        this.importType != null &&
        ((format.value === 'openapi' && this.importType !== 'SWAGGER') || (format.value === 'gravitee' && this.importType !== 'MAPI_V2'))
      ) {
        return { mismatchFileFormat: true };
      }
      return null;
    };
  }
}
