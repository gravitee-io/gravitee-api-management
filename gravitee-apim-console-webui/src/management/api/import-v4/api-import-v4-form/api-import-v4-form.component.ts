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
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { Component, computed, DestroyRef, effect, inject, input, output, signal, Signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NgTemplateOutlet } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioBannerModule, GioFormSelectionInlineModule, GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { combineLatest, defer, EMPTY, merge, Observable, of, throwError } from 'rxjs';
import { catchError, distinctUntilChanged, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { ApiImportFilePickerComponent } from '../../component/api-import-file-picker/api-import-file-picker.component';
import { ApiV4, PolicyPlugin } from '../../../../entities/management-api-v2';
import { ImportSwaggerDescriptor } from '../../../../entities/management-api-v2/api/v4/importSwaggerDescriptor';
import { ImportWsdlDescriptor } from '../../../../entities/management-api-v2/api/v4/importWsdlDescriptor';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { PolicyV2Service } from '../../../../services-ngx/policy-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

function formValidSignal(form: AbstractControl, initialValue = form.valid): Signal<boolean> {
  return toSignal(
    merge(form.statusChanges, form.valueChanges).pipe(
      startWith(null),
      map(() => form.valid),
    ),
    { initialValue },
  );
}

@Component({
  selector: 'api-import-v4-form',
  standalone: true,
  host: {
    '[class.api-import-v4-form--embedded-dialog]': 'embeddedInDialog()',
  },
  imports: [
    ApiImportFilePickerComponent,
    NgTemplateOutlet,
    GioBannerModule,
    GioFormSelectionInlineModule,
    GioFormSlideToggleModule,
    GioIconsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggle,
    MatStepperModule,
    MatTooltipModule,
    ReactiveFormsModule,
    RouterModule,
  ],
  templateUrl: './api-import-v4-form.component.html',
  styleUrl: './api-import-v4-form.component.scss',
})
export class ApiImportV4FormComponent {
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly http = inject(HttpClient);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly policyV2Service = inject(PolicyV2Service);

  readonly updateTargetApiId = input<string | undefined>(undefined);
  readonly embeddedInDialog = input(false);
  readonly embeddedDialogApiName = input<string | undefined>(undefined);

  readonly importCompleted = output<string>();
  readonly dismissed = output<void>();

  protected readonly importStepper = viewChild(MatStepper);

  protected readonly primaryImportActionLabel = computed(() => (this.updateTargetApiId() ? 'Update API' : 'Import API'));

  protected readonly reviewStepTitle = computed(() => (this.updateTargetApiId() ? 'Review before updating' : 'Review your import'));

  protected readonly reviewStepDescription = computed(() =>
    this.updateTargetApiId()
      ? 'Verify all settings before updating this API from the selected definition.'
      : 'Verify all settings before importing your API definition.',
  );

  protected readonly formats = [
    { value: 'gravitee', label: 'Gravitee definition', icon: 'gio:gravitee' },
    { value: 'openapi', label: 'OpenAPI specification', icon: 'gio:open-api' },
    { value: 'wsdl', label: 'WSDL', icon: 'gio:language' },
  ];
  protected readonly sources = [
    { value: 'local', label: 'Local file', icon: 'gio:laptop', disabled: false },
    { value: 'remote', label: 'Remote source', icon: 'gio:language', disabled: false },
  ];

  private readonly importFileContent = signal<string | undefined>(undefined);

  private readonly importType = signal<string | undefined>(undefined);

  public readonly selectApiFormatForm = this.formBuilder.group({
    format: ['gravitee', Validators.required],
  });

  public readonly configureFileSourceForm = this.formBuilder.group(
    {
      source: ['local', Validators.required],
      remoteUrl: [''],
      /** When set, sent as the `Authorization` header on the GET to the user-entered remote URL only — not to the Gravitee Management API. */
      authorizationHeader: [''],
    },
    { validators: [this.createConfigureFileSourceValidator()] },
  );

  public readonly optionsForm = this.formBuilder.group({
    withRestToSoap: [{ value: false, disabled: false }],
    withDocumentation: [{ value: false, disabled: false }],
    withOASValidationPolicy: [{ value: false, disabled: false }],
  });

  protected readonly hasOasValidationPolicy = signal(false);
  protected readonly hasRestToSoapPolicy = signal(false);

  protected readonly importFormat = toSignal(
    this.selectApiFormatForm.controls.format.valueChanges.pipe(
      startWith(this.selectApiFormatForm.controls.format.value),
      distinctUntilChanged(),
    ),
    { initialValue: this.selectApiFormatForm.controls.format.value },
  );

  private readonly resetConfigureStepWhenApiFormatChanges = (() => {
    let previous = this.selectApiFormatForm.controls.format.value;
    return effect(() => {
      const current = this.importFormat();
      if (current === previous) return;
      previous = current;
      this.resetConfigureFileSourceAfterApiFormatChange();
    });
  })();

  private readonly importSourceMode = toSignal(
    this.configureFileSourceForm.controls.source.valueChanges.pipe(
      startWith(this.configureFileSourceForm.controls.source.value),
      distinctUntilChanged(),
    ),
    { initialValue: this.configureFileSourceForm.controls.source.value },
  );

  private readonly selectApiFormatFormValid = formValidSignal(this.selectApiFormatForm);

  private readonly configureFileSourceFormValid = formValidSignal(this.configureFileSourceForm);

  private readonly optionsFormValidSignal = formValidSignal(this.optionsForm);

  protected readonly isFormatStepNextDisabled = computed(() => !this.selectApiFormatFormValid());

  protected readonly isFileSourceStepNextDisabled = computed(() => !this.configureFileSourceFormValid());

  protected readonly isOptionsStepNextDisabled = computed(() => !this.optionsFormValidSignal());

  protected readonly allowedImportFileExtensions = computed((): string[] => {
    const format = this.importFormat();
    return format === 'wsdl' ? ['wsdl', 'xml'] : ['yml', 'yaml', 'json'];
  });

  protected readonly allowedImportFileExtensionsLabel = computed(() => this.allowedImportFileExtensions().join(', '));

  protected readonly selectedFormatLabel = computed(() => {
    const value = this.importFormat();
    return this.formats.find(f => f.value === value)?.label ?? '-';
  });

  protected readonly selectedSourceLabel = computed(() => {
    const value = this.importSourceMode();
    return this.sources.find(s => s.value === value)?.label ?? '-';
  });

  protected readonly showImportOptionsStep = computed(() => {
    const format = this.importFormat();
    return format === 'openapi' || format === 'wsdl';
  });

  protected readonly importInProgress = signal(false);

  protected readonly canSubmitImport = computed(() => {
    this.importFileContent();
    this.importType();

    const formatOk = this.selectApiFormatFormValid();
    const fileSourceOk = this.configureFileSourceFormValid();
    const optionsOk = this.showImportOptionsStep() ? this.optionsFormValidSignal() : true;

    return formatOk && fileSourceOk && optionsOk && this.importRequestContextOk();
  });

  protected readonly isReviewImportPrimaryDisabled = computed(() => !this.canSubmitImport() || this.importInProgress());

  private readonly formatAndPolicies = toSignal(
    combineLatest([
      this.selectApiFormatForm.valueChanges.pipe(
        map(() => this.selectApiFormatForm.controls.format.value),
        startWith(this.selectApiFormatForm.controls.format.value),
        distinctUntilChanged(),
      ),
      this.policyV2Service.list().pipe(
        catchError((err: unknown) => {
          this.snackBarService.error(this.readPolicyListErrorMessage(err));
          return of([] as PolicyPlugin[]);
        }),
      ),
    ]),
    {
      initialValue: [this.selectApiFormatForm.controls.format.value, [] as PolicyPlugin[]] as [string | null, PolicyPlugin[]],
    },
  );

  private readonly syncPolicyOptionsEffect = effect(() => {
    const [format, policies] = this.formatAndPolicies();
    const hasOas = policies.some(policy => policy.id === 'oas-validation');
    const hasRestToSoap = policies.some(policy => policy.id === 'rest-to-soap');
    this.hasOasValidationPolicy.set(hasOas);
    this.hasRestToSoapPolicy.set(hasRestToSoap);
    this.syncImportOptionsFromFormat(format, hasOas, hasRestToSoap);
    this.configureFileSourceForm.updateValueAndValidity({ emitEvent: true });
    this.optionsForm.updateValueAndValidity({ emitEvent: true });
  });

  private readonly restToSoapValue = toSignal(
    this.optionsForm.controls.withRestToSoap.valueChanges.pipe(startWith(this.optionsForm.controls.withRestToSoap.value)),
    { initialValue: this.optionsForm.controls.withRestToSoap.value },
  );

  private readonly syncRestToSoapDependentsEffect = effect(() => {
    const format = this.importFormat();
    if (format !== 'wsdl') return;
    const restToSoapOn = this.restToSoapValue();
    if (restToSoapOn) {
      this.optionsForm.controls.withDocumentation.setValue(true, { emitEvent: false });
      this.optionsForm.controls.withDocumentation.enable({ emitEvent: false });
      if (this.hasOasValidationPolicy()) {
        this.optionsForm.controls.withOASValidationPolicy.setValue(true, { emitEvent: false });
        this.optionsForm.controls.withOASValidationPolicy.enable({ emitEvent: false });
      }
    } else {
      this.optionsForm.controls.withDocumentation.setValue(false, { emitEvent: false });
      this.optionsForm.controls.withDocumentation.disable({ emitEvent: false });
      this.optionsForm.controls.withOASValidationPolicy.setValue(false, { emitEvent: false });
      this.optionsForm.controls.withOASValidationPolicy.disable({ emitEvent: false });
    }
  });

  private readonly applyFileSourceModeEffect = effect(() => {
    this.applyFileSourceMode(this.importSourceMode());
  });

  protected onImportFile(event: { importFileContent?: string; importType?: string }): void {
    this.importFileContent.set(event.importFileContent);
    this.importType.set(event.importType);
    this.configureFileSourceForm.updateValueAndValidity({ emitEvent: true });
  }

  protected importApi(): void {
    if (this.importInProgress()) {
      return;
    }
    const request$ = this.resolveImportRequest$();
    if (!request$) {
      this.snackBarService.error('Unsupported type for V4 API import');
      return;
    }

    this.importInProgress.set(true);
    request$
      .pipe(
        tap(api => {
          if (this.updateTargetApiId()) {
            this.snackBarService.success('API definition updated successfully');
            this.importCompleted.emit(api.id);
            return;
          }
          this.snackBarService.success('API imported successfully');
          this.router.navigate([`../../${api.id}`], { relativeTo: this.activatedRoute });
        }),
        catchError((err: unknown) => this.mapImportErrorToEmpty(err)),
        finalize(() => this.importInProgress.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected onDismiss(): void {
    this.dismissed.emit();
  }

  protected goStepperNext(): void {
    this.importStepper()?.next();
  }

  protected goStepperPrevious(): void {
    this.importStepper()?.previous();
  }

  protected onImportStepSelectionChange(event: StepperSelectionEvent): void {
    const selectedIndex = event.selectedIndex;
    const previousIndex = event.previouslySelectedIndex;
    if (selectedIndex < previousIndex) {
      this.importInProgress.set(false);
    }
  }

  /** Clears picked file and remote URL fields when the user selects a different API format on step 1 (stepper navigation alone does not reset). */
  private resetConfigureFileSourceAfterApiFormatChange(): void {
    this.importFileContent.set(undefined);
    this.importType.set(undefined);
    if (this.configureFileSourceForm.controls.source.value !== 'local') {
      this.configureFileSourceForm.controls.source.setValue('local', { emitEvent: true });
    }
    this.configureFileSourceForm.patchValue({ remoteUrl: '', authorizationHeader: '' }, { emitEvent: false });
    this.configureFileSourceForm.controls.remoteUrl.markAsUntouched();
    this.configureFileSourceForm.controls.authorizationHeader.markAsUntouched();
    this.configureFileSourceForm.updateValueAndValidity({ emitEvent: true });
  }

  private applyFileSourceMode(source: string | null): void {
    const urlCtrl = this.configureFileSourceForm.controls.remoteUrl;
    const authCtrl = this.configureFileSourceForm.controls.authorizationHeader;

    if (source === 'remote') {
      urlCtrl.setValidators([Validators.required, ApiImportV4FormComponent.remoteHttpUrlValidator]);
      this.importFileContent.set(undefined);
      this.importType.set(undefined);
    } else {
      urlCtrl.clearValidators();
      urlCtrl.setValue('', { emitEvent: false });
      authCtrl.setValue('', { emitEvent: false });
      urlCtrl.markAsUntouched();
      authCtrl.markAsUntouched();
    }
    urlCtrl.updateValueAndValidity({ emitEvent: true });
    authCtrl.updateValueAndValidity({ emitEvent: false });
    this.configureFileSourceForm.updateValueAndValidity({ emitEvent: true });
  }

  private syncImportOptionsFromFormat(
    format: string | null,
    hasOasValidationPolicyInstalled: boolean,
    hasRestToSoapPolicyInstalled: boolean,
  ): void {
    if (format === 'openapi') {
      this.optionsForm.controls.withRestToSoap.setValue(false, { emitEvent: false });
      this.optionsForm.controls.withRestToSoap.disable({ emitEvent: false });
      this.optionsForm.patchValue(
        { withDocumentation: true, withOASValidationPolicy: hasOasValidationPolicyInstalled },
        { emitEvent: false },
      );
      this.optionsForm.controls.withDocumentation.enable();
      if (hasOasValidationPolicyInstalled) {
        this.optionsForm.controls.withOASValidationPolicy.enable();
      } else {
        this.optionsForm.controls.withOASValidationPolicy.setValue(false, { emitEvent: false });
        this.optionsForm.controls.withOASValidationPolicy.disable({ emitEvent: false });
      }
      return;
    }
    if (format === 'wsdl') {
      if (hasRestToSoapPolicyInstalled) {
        // Intentionally emits event so `restToSoapValue` signal updates and `syncRestToSoapDependentsEffect` sets documentation/OAS controls.
        this.optionsForm.controls.withRestToSoap.setValue(true);
        this.optionsForm.controls.withRestToSoap.enable({ emitEvent: false });
      } else {
        this.optionsForm.controls.withRestToSoap.setValue(false, { emitEvent: false });
        this.optionsForm.controls.withRestToSoap.disable({ emitEvent: false });
        this.optionsForm.controls.withDocumentation.setValue(false, { emitEvent: false });
        this.optionsForm.controls.withDocumentation.disable({ emitEvent: false });
        this.optionsForm.controls.withOASValidationPolicy.setValue(false, { emitEvent: false });
        this.optionsForm.controls.withOASValidationPolicy.disable({ emitEvent: false });
      }
      return;
    }
    this.optionsForm.controls.withRestToSoap.setValue(false, { emitEvent: false });
    this.optionsForm.controls.withRestToSoap.disable({ emitEvent: false });
    this.optionsForm.patchValue({ withDocumentation: false, withOASValidationPolicy: false });
    this.optionsForm.controls.withDocumentation.enable();
    this.optionsForm.controls.withOASValidationPolicy.enable();
  }

  private static remoteHttpUrlValidator(control: AbstractControl): ValidationErrors | null {
    const raw = (control.value as string | null | undefined)?.trim() ?? '';
    if (!raw) {
      return null;
    }
    try {
      const u = new URL(raw);
      if (u.protocol !== 'http:' && u.protocol !== 'https:') {
        return { invalidHttpUrl: true };
      }
    } catch {
      return { invalidHttpUrl: true };
    }
    return null;
  }

  private createConfigureFileSourceValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const fg = group as FormGroup;
      const source = fg.get('source')?.value;
      if (source === 'remote') {
        const urlCtrl = fg.get('remoteUrl');
        if (urlCtrl?.invalid) {
          return { remoteSourceInvalid: true };
        }
        return null;
      }
      const importType = this.importType();
      const fileContent = this.importFileContent();
      if (!importType || fileContent == null || fileContent === '') {
        return { fileRequired: true };
      }
      const format = this.selectApiFormatForm.controls.format.value;
      if (
        (format === 'openapi' && importType !== 'SWAGGER') ||
        (format === 'gravitee' && importType !== 'MAPI_V2') ||
        (format === 'wsdl' && importType !== 'WSDL')
      ) {
        return { mismatchFileFormat: true };
      }
      return null;
    };
  }

  /**
   * Same readiness rules as {@link resolveImportRequest$} without allocating observables
   * (used by {@link canSubmitImport}).
   */
  private importRequestContextOk(): boolean {
    const source = this.configureFileSourceForm.controls.source.value;
    const format = this.selectApiFormatForm.controls.format.value;

    if (source === 'remote') {
      const url = this.configureFileSourceForm.controls.remoteUrl.value?.trim();
      if (!url || this.configureFileSourceForm.controls.remoteUrl.invalid) {
        return false;
      }
      return format === 'gravitee' || format === 'openapi' || format === 'wsdl';
    }

    const type = this.importType();
    const fileContent = this.importFileContent();
    if (!type || fileContent == null || fileContent === '') {
      return false;
    }

    return (
      (format === 'gravitee' && type === 'MAPI_V2') ||
      (format === 'openapi' && type === 'SWAGGER') ||
      (format === 'wsdl' && type === 'WSDL')
    );
  }

  private resolveImportRequest$(): Observable<ApiV4> | null {
    if (!this.importRequestContextOk()) {
      return null;
    }

    const source = this.configureFileSourceForm.controls.source.value;
    return source === 'remote' ? this.resolveRemoteImport$() : this.resolveFileImport$();
  }

  private resolveRemoteImport$(): Observable<ApiV4> | null {
    const updateId = this.updateTargetApiId();
    const format = this.selectApiFormatForm.controls.format.value;
    const url = this.configureFileSourceForm.controls.remoteUrl.value?.trim() as string;

    if (format === 'gravitee') {
      return this.importGraviteeDefinitionFromRemoteUrl$(url, updateId);
    }
    if (format === 'wsdl') {
      return updateId
        ? this.apiV2Service.updateApiFromWsdl(updateId, this.buildImportWsdlDescriptor(url, 'URL'))
        : this.apiV2Service.importWsdlApi(this.buildImportWsdlDescriptor(url, 'URL'));
    }
    return updateId
      ? this.apiV2Service.updateApiFromSwagger(updateId, this.buildImportSwaggerDescriptor(url))
      : this.apiV2Service.importSwaggerApi(this.buildImportSwaggerDescriptor(url));
  }

  private resolveFileImport$(): Observable<ApiV4> | null {
    const updateId = this.updateTargetApiId();
    const format = this.selectApiFormatForm.controls.format.value;
    const pickedType = this.importType();
    const fileContent = this.importFileContent() as string;

    if (format === 'gravitee' && pickedType === 'MAPI_V2') {
      return updateId
        ? defer(() => this.apiV2Service.updateApiFromDefinition(updateId, this.parseGraviteeDefinitionJson(fileContent)))
        : this.apiV2Service.import(fileContent);
    }
    if (format === 'openapi' && pickedType === 'SWAGGER') {
      return updateId
        ? this.apiV2Service.updateApiFromSwagger(updateId, this.buildImportSwaggerDescriptor(fileContent))
        : this.apiV2Service.importSwaggerApi(this.buildImportSwaggerDescriptor(fileContent));
    }
    if (format === 'wsdl' && pickedType === 'WSDL') {
      return updateId
        ? this.apiV2Service.updateApiFromWsdl(updateId, this.buildImportWsdlDescriptor(fileContent, 'INLINE'))
        : this.apiV2Service.importWsdlApi(this.buildImportWsdlDescriptor(fileContent, 'INLINE'));
    }
    return null;
  }

  /** Builds the OpenAPI/Swagger import descriptor sent to the Management API. */
  private buildImportSwaggerDescriptor(payload: string): ImportSwaggerDescriptor {
    const rawOptions = this.optionsForm.getRawValue();
    return {
      payload,
      withDocumentation: rawOptions.withDocumentation,
      withOASValidationPolicy: rawOptions.withOASValidationPolicy,
    };
  }

  /** Builds the WSDL import descriptor sent to the Management API. */
  private buildImportWsdlDescriptor(payload: string, type: 'INLINE' | 'URL'): ImportWsdlDescriptor {
    const rawOptions = this.optionsForm.getRawValue();
    return {
      payload,
      type,
      withDocumentation: rawOptions.withDocumentation,
      withOASValidationPolicy: rawOptions.withOASValidationPolicy,
      withPolicies: rawOptions.withRestToSoap ? ['rest-to-soap'] : [],
    };
  }

  private parseGraviteeDefinitionJson(content: string): unknown {
    const trimmed = content.trim();
    if (!trimmed) {
      throw new Error('The API definition content is empty');
    }
    try {
      return JSON.parse(trimmed);
    } catch {
      throw new Error('Invalid JSON in Gravitee API definition');
    }
  }

  /**
   * Fetches the Gravitee definition JSON from the URL the user typed (browser-side GET).
   * Optional `authorizationHeader` from `configureFileSourceForm` is forwarded only on that request, not on subsequent Management API calls.
   */
  private importGraviteeDefinitionFromRemoteUrl$(url: string, updateApiId: string | undefined): Observable<ApiV4> {
    const auth = this.configureFileSourceForm.controls.authorizationHeader.value?.trim();
    let headers = new HttpHeaders();
    if (auth) {
      headers = headers.set('Authorization', auth);
    }
    return this.http.get(url, { responseType: 'text', headers }).pipe(
      switchMap(body => {
        const trimmed = body.trim();
        if (!trimmed) {
          return throwError(() => new HttpErrorResponse({ status: 0, error: 'The URL returned an empty response' }));
        }
        let definition: unknown;
        try {
          definition = JSON.parse(trimmed);
        } catch {
          return throwError(() => new HttpErrorResponse({ status: 0, error: 'The URL must return a Gravitee API definition as JSON' }));
        }
        if (updateApiId) {
          return this.apiV2Service.updateApiFromDefinition(updateApiId, definition);
        }
        return this.apiV2Service.import(trimmed);
      }),
    );
  }

  private mapImportErrorToEmpty(err: unknown): Observable<never> {
    this.snackBarService.error(this.readImportErrorMessage(err));
    return EMPTY;
  }

  private readImportErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (typeof err.error === 'string') {
        return err.error;
      }
      if (err.status === 0) {
        return 'Could not fetch the remote URL. Check that the URL is reachable and allows CORS requests from this Console.';
      }
      const fromBody = (err.error as { message?: string } | null | undefined)?.message;
      return fromBody ?? err.message ?? 'An error occurred while importing the API';
    }
    if (err instanceof Error) {
      return err.message;
    }
    return 'An error occurred while importing the API';
  }

  private readPolicyListErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (typeof err.error === 'string') {
        return err.error;
      }
      const fromBody = (err.error as { message?: string } | null | undefined)?.message;
      return fromBody ?? err.message ?? 'Could not load policies.';
    }
    return 'Could not load policies.';
  }
}
