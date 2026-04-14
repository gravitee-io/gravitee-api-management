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
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatError, MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { GioBannerModule, GioFormSelectionInlineModule, GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { distinctUntilChanged, map, startWith } from 'rxjs/operators';

import { ApiImportV4Format, ApiImportV4FileSourceType, ApiImportV4WizardPayload } from './api-import-v4-wizard.model';

import { ApiImportFilePickerComponent } from '../component/api-import-file-picker/api-import-file-picker.component';

@Component({
  selector: 'api-import-v4-stepper',
  imports: [
    ApiImportFilePickerComponent,
    GioBannerModule,
    GioFormSelectionInlineModule,
    GioFormSlideToggleModule,
    GioIconsModule,
    MatButtonModule,
    MatError,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggle,
    MatStepperModule,
    ReactiveFormsModule,
  ],
  templateUrl: './api-import-v4-stepper.component.html',
  styleUrl: './api-import-v4-stepper.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiImportV4StepperComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly hasOasValidationPolicy = input<boolean>(false);

  readonly importRequested = output<ApiImportV4WizardPayload>();
  readonly cancelled = output<void>();

  private readonly stepper = viewChild.required(MatStepper);

  protected readonly filePickerKey = signal(0);
  private readonly detectedImportType = signal<string | undefined>(undefined);

  protected readonly step1Group = new FormGroup({
    apiFormat: new FormControl<ApiImportV4Format>('gravitee', { nonNullable: true, validators: [Validators.required] }),
  });

  protected readonly step2Group = new FormGroup(
    {
      fileSourceType: new FormControl<ApiImportV4FileSourceType>('local', { nonNullable: true, validators: [Validators.required] }),
      fileContent: new FormControl<string | null>(null),
      remoteUrl: new FormControl<string | null>(null),
      remoteAuthorizationHeader: new FormControl<string | null>(null),
    },
    { validators: [this.createStep2CrossValidator()] },
  );

  protected readonly step3Group = new FormGroup({
    createDocPage: new FormControl(false, { nonNullable: true }),
    addSpecValidation: new FormControl(false, { nonNullable: true }),
  });

  protected readonly step4Group = new FormGroup({});

  protected readonly apiFormat = toSignal(
    this.step1Group.controls.apiFormat.valueChanges.pipe(
      startWith(this.step1Group.controls.apiFormat.value),
      map(v => v as ApiImportV4Format),
    ),
    { initialValue: this.step1Group.controls.apiFormat.value },
  );

  protected readonly showOptionsStep = computed(() => this.apiFormat() !== 'gravitee');

  protected readonly apiFormatLabel = computed((): string => {
    switch (this.apiFormat()) {
      case 'gravitee':
        return 'Gravitee definition';
      case 'openapi':
        return 'OpenAPI specification';
      case 'wsdl':
        return 'WSDL';
      default:
        return '';
    }
  });

  protected readonly allowedFileExtensions = computed((): string[] => {
    switch (this.apiFormat()) {
      case 'wsdl':
        return ['wsdl', 'xml'];
      default:
        return ['yml', 'yaml', 'json'];
    }
  });

  protected readonly formats: ReadonlyArray<{ value: ApiImportV4Format; label: string; icon: string }> = [
    { value: 'gravitee', label: 'Gravitee definition', icon: 'gio:gravitee' },
    { value: 'openapi', label: 'OpenAPI specification', icon: 'gio:open-api' },
    { value: 'wsdl', label: 'WSDL', icon: 'gio:language' },
  ];

  protected readonly sources: ReadonlyArray<{ value: ApiImportV4FileSourceType; label: string; icon: string }> = [
    { value: 'local', label: 'Local file', icon: 'gio:laptop' },
    { value: 'remote', label: 'Remote source', icon: 'gio:cloud-server' },
  ];

  constructor() {
    this.step1Group.controls.apiFormat.valueChanges.pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.resetDownstreamAfterApiFormatChange();
    });

    this.step2Group.controls.fileSourceType.valueChanges.pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.applyFileSourceValidatorsAndResetBranch();
    });

    this.applyFileSourceValidatorsAndResetBranch();
    this.resetDownstreamAfterApiFormatChange();
  }

  protected onFilePicked(event: { importFileContent?: string; importType?: string }): void {
    this.detectedImportType.set(event.importType);
    this.step2Group.patchValue({ fileContent: event.importFileContent ?? null });
    this.step2Group.updateValueAndValidity();
  }

  protected onImportClick(): void {
    if (this.isImportActionDisabled()) {
      return;
    }
    this.importRequested.emit(this.buildPayload());
  }

  /** Mirrors the template disabled expression for the Import action. */
  protected isImportActionDisabled(): boolean {
    const step3Ok = !this.showOptionsStep() || this.step3Group.disabled || this.step3Group.valid;
    return this.step1Group.invalid || this.step2Group.invalid || !step3Ok || this.step4Group.invalid;
  }

  protected onCancelClick(): void {
    this.resetWizard();
    this.cancelled.emit();
  }

  private buildPayload(): ApiImportV4WizardPayload {
    const step1 = this.step1Group.getRawValue();
    const step2 = this.step2Group.getRawValue();
    const options = this.showOptionsStep()
      ? this.step3Group.getRawValue()
      : { createDocPage: false, addSpecValidation: false };

    return {
      apiFormat: step1.apiFormat,
      fileSourceType: step2.fileSourceType,
      fileContent: step2.fileContent,
      remoteUrl: step2.remoteUrl,
      remoteAuthorizationHeader: step2.remoteAuthorizationHeader,
      createDocPage: options.createDocPage,
      addSpecValidation: options.addSpecValidation,
      detectedImportType: this.detectedImportType(),
    };
  }

  private resetDownstreamAfterApiFormatChange(): void {
    this.detectedImportType.set(undefined);
    this.step2Group.patchValue({
      fileContent: null,
      remoteUrl: null,
      remoteAuthorizationHeader: null,
    });
    this.step2Group.controls.fileContent.clearValidators();
    this.step2Group.controls.remoteUrl.clearValidators();
    this.applyFileSourceValidatorsAndResetBranch();

    const format = this.step1Group.controls.apiFormat.value;
    if (format === 'gravitee') {
      this.step3Group.patchValue({ createDocPage: false, addSpecValidation: false });
      this.step3Group.disable({ emitEvent: false });
    } else {
      this.step3Group.enable({ emitEvent: false });
      this.step3Group.patchValue({
        createDocPage: format === 'openapi' || format === 'wsdl',
        addSpecValidation: format === 'openapi',
      });
    }
    this.step3Group.updateValueAndValidity();

    this.filePickerKey.update(k => k + 1);
    this.step2Group.updateValueAndValidity();
  }

  private applyFileSourceValidatorsAndResetBranch(): void {
    const sourceType = this.step2Group.controls.fileSourceType.value;
    const fileContent = this.step2Group.controls.fileContent;
    const remoteUrl = this.step2Group.controls.remoteUrl;

    if (sourceType === 'local') {
      remoteUrl.clearValidators();
      remoteUrl.setValue(null, { emitEvent: false });
      this.step2Group.controls.remoteAuthorizationHeader.setValue(null, { emitEvent: false });
      fileContent.updateValueAndValidity();
    } else {
      fileContent.clearValidators();
      fileContent.setValue(null, { emitEvent: false });
      this.detectedImportType.set(undefined);
      remoteUrl.setValidators([Validators.required, Validators.pattern(/^https?:\/\/.+/i)]);
      remoteUrl.updateValueAndValidity();
    }

    remoteUrl.updateValueAndValidity();
    fileContent.updateValueAndValidity();
    this.step2Group.updateValueAndValidity();
  }

  private resetWizard(): void {
    this.step1Group.reset({ apiFormat: 'gravitee' });
    this.step2Group.reset({
      fileSourceType: 'local',
      fileContent: null,
      remoteUrl: null,
      remoteAuthorizationHeader: null,
    });
    this.step3Group.reset({ createDocPage: false, addSpecValidation: false });
    this.detectedImportType.set(undefined);
    this.filePickerKey.update(k => k + 1);
    this.applyFileSourceValidatorsAndResetBranch();
    this.resetDownstreamAfterApiFormatChange();
    this.stepper().reset();
  }

  private createStep2CrossValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const group = control as FormGroup;
      const fileSourceType = group.controls['fileSourceType'].value as ApiImportV4FileSourceType;
      if (fileSourceType === 'remote') {
        return null;
      }
      const apiFormat = this.step1Group.controls.apiFormat.value;
      const fileContent = group.controls['fileContent'].value;
      if (!fileContent) {
        return { fileRequired: true };
      }
      const importType = this.detectedImportType();
      if (!importType) {
        return { fileRequired: true };
      }
      if (apiFormat === 'openapi' && importType !== 'SWAGGER') {
        return { mismatchFileFormat: true };
      }
      if (apiFormat === 'gravitee' && importType !== 'MAPI_V2') {
        return { mismatchFileFormat: true };
      }
      if (apiFormat === 'wsdl' && importType !== 'WSDL') {
        return { mismatchFileFormat: true };
      }
      return null;
    };
  }
}
