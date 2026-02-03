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
import { Component, DestroyRef, Inject, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule, GioFormSelectionInlineModule, GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ApiImportFilePickerComponent } from '../component/api-import-file-picker/api-import-file-picker.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PolicyV2Service } from '../../../services-ngx/policy-v2.service';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, EMPTY } from 'rxjs';
import { ApiV4 } from '../../../entities/management-api-v2';

export interface ApiImportV4DialogData {
    apiId: string;
}

@Component({
    selector: 'api-import-v4-dialog',
    templateUrl: './api-import-v4-dialog.component.html',
    styleUrls: ['./api-import-v4-dialog.component.scss'],
    standalone: true,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatButtonModule,
        GioBannerModule,
        GioFormSelectionInlineModule,
        GioIconsModule,
        MatTooltipModule,
        MatSlideToggleModule,
        GioFormSlideToggleModule,
        ApiImportFilePickerComponent,
    ],
})
export class ApiImportV4DialogComponent implements OnInit {
    private apiV2Service = inject(ApiV2Service);
    private snackBarService = inject(SnackBarService);
    private policyV2Service = inject(PolicyV2Service);
    private destroyRef = inject(DestroyRef);
    private unsubscribe$: Subject<void> = new Subject<void>();

    protected apiId: string;
    protected importType: string;
    protected importFileContent: string;

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

    constructor(
        private readonly dialogRef: MatDialogRef<ApiImportV4DialogComponent>,
        @Inject(MAT_DIALOG_DATA) dialogData: ApiImportV4DialogData,
    ) {
        this.apiId = dialogData.apiId;
    }

    ngOnInit(): void {
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
            result = this.apiV2Service.importUpdate(this.apiId, this.importFileContent);
        } else if (
            this.form.controls.source.value === 'local' &&
            this.form.controls.format.value === 'openapi' &&
            this.importType === 'SWAGGER'
        ) {
            result = this.apiV2Service.importSwaggerApiUpdate(this.apiId, {
                payload: this.importFileContent,
                withDocumentation: this.form.value.withDocumentation,
                withOASValidationPolicy: this.form.value.withOASValidationPolicy,
            });
        } else {
            this.snackBarService.error('Unsupported type for V4 API import settings');
            return;
        }

        result
            .pipe(
                tap((updatedApi) => {
                    this.snackBarService.success('API updated successfully');
                    this.dialogRef.close(updatedApi);
                }),
                catchError(({ error }) => {
                    this.snackBarService.error(error.message ?? 'An error occurred while updating the API');
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
