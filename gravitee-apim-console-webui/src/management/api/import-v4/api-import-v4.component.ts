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
import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { GioFormSelectionInlineModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EMPTY } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiImportFilePickerComponent } from '../component/api-import-file-picker/api-import-file-picker.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-import-v4',
  standalone: true,
  imports: [
    FormsModule,
    GioFormSelectionInlineModule,
    GioIconsModule,
    MatButtonModule,
    MatCardModule,
    MatTooltipModule,
    ReactiveFormsModule,
    RouterModule,
    ApiImportFilePickerComponent,
  ],
  templateUrl: './api-import-v4.component.html',
  styleUrl: './api-import-v4.component.scss',
})
export class ApiImportV4Component implements AfterViewInit {
  private apiV2Service = inject(ApiV2Service);
  private snackBarService = inject(SnackBarService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private changeDetectorRef = inject(ChangeDetectorRef);
  private importFileContent: string;

  protected importType: string;
  protected formats = [
    { value: 'gravitee', label: 'Gravitee definition', icon: 'gio:gravitee', disabled: false },
    { value: 'openapi', label: 'OpenAPI specification', icon: 'gio:open-api', disabled: true },
  ];
  protected sources = [
    { value: 'local', label: 'Local file', icon: 'gio:laptop', disabled: false },
    { value: 'remote', label: 'Remote source', icon: 'gio:language', disabled: true },
  ];
  protected form = new FormGroup({
    format: new FormControl('gravitee', [Validators.required]),
    source: new FormControl('local', [Validators.required]),
  });

  ngAfterViewInit() {
    // FIXME: Check on gravitee-ui-particles why we have an ExpressionHasChangedAfterCheckedError.
    this.changeDetectorRef.detectChanges();
  }

  protected onImportFile({ importFileContent, importType }: { importFileContent: string; importType: string }) {
    this.importType = importType;
    this.importFileContent = importFileContent;
  }

  protected import() {
    if (this.form.controls.source.value === 'local' && this.form.controls.format.value === 'gravitee' && this.importType === 'MAPI_V2') {
      this.apiV2Service
        .import(this.importFileContent)
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
    } else {
      this.snackBarService.error('Unsupported type for V4 API import');
    }
  }
}
