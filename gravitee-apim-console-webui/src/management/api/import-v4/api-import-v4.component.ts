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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { GioBannerModule, GioFormSelectionInlineModule, GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatSlideToggle } from '@angular/material/slide-toggle';

import { ApiImportFilePickerComponent } from '../component/api-import-file-picker/api-import-file-picker.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApiV4 } from '../../../entities/management-api-v2';

@Component({
  selector: 'api-import-v4',
  standalone: true,
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
    MatCheckbox,
    GioFormSlideToggleModule,
    MatSlideToggle,
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
  private changeDetectorRef = inject(ChangeDetectorRef);
  private importFileContent: string;
  private unsubscribe$: Subject<void> = new Subject<void>();

  protected importType: string;
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
    },
    [this.fileFormatValidator()],
  );

  ngOnInit(): void {
    this.form.controls['format'].valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
      if (value !== 'openapi') {
        this.form.patchValue({ withDocumentation: false });
        this.form.controls['withDocumentation'].disable();
      } else {
        this.form.patchValue({ withDocumentation: true });
        this.form.controls['withDocumentation'].enable();
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
      result = this.apiV2Service.import(this.importFileContent);
    } else if (
      this.form.controls.source.value === 'local' &&
      this.form.controls.format.value === 'openapi' &&
      this.importType === 'SWAGGER'
    ) {
      result = this.apiV2Service.importSwaggerApi({
        payload: this.importFileContent,
        withDocumentation: this.form.value.withDocumentation,
      });
    } else {
      this.snackBarService.error('Unsupported type for V4 API import');
    }

    result
      .pipe(
        tap((createdApi) => {
          this.snackBarService.success('API imported successfully');
          this.router.navigate([`../../${createdApi.id}`], { relativeTo: this.activatedRoute });
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message ?? 'An error occurred while importing the API');
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
