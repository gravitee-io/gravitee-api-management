/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, DestroyRef, effect, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, FormRecord, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Router } from '@angular/router';
import { startWith } from 'rxjs';

import { BreadcrumbNavigationComponent } from '../../../components/breadcrumb-navigation/breadcrumb-navigation.component';
import { FormKeyValuePairsComponent } from '../../../components/form-key-value-pairs/form-key-value-pairs.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { MobileClassDirective } from '../../../directives/mobile-class.directive';
import { ApplicationInput, ApplicationSettings, ApplicationType } from '../../../entities/application/application';
import { ApplicationTypeTranslatePipe } from '../../../pipe/application-type-translate.pipe';
import { ApplicationService } from '../../../services/application.service';
import { ObservabilityBreakpointService } from '../../../services/observability-breakpoint.service';

type BaseControls = {
  name: FormControl<string>;
  description: FormControl<string>;
  clientCertificate: FormControl<string>;
  appType: FormControl<string>;
  appClientId: FormControl<string>;
  metadata: FormControl<Record<string, string> | null>;
  dynamic: FormRecord<FormControl<unknown>>;
};

interface GrantTypeVM {
  type: string;
  name: string;
  isDisabled: boolean;
}

@Component({
  selector: 'app-create-application',
  imports: [
    ApplicationTypeTranslatePipe,
    BreadcrumbNavigationComponent,
    FormKeyValuePairsComponent,
    LoaderComponent,
    MatButtonModule,
    MatCard,
    MatCardContent,
    MatChipsModule,
    MatDividerModule,
    MatError,
    MatFormField,
    MatHint,
    MatIcon,
    MatInput,
    MatLabel,
    MatRadioButton,
    MatRadioGroup,
    MatSelectModule,
    MatSlideToggleModule,
    MobileClassDirective,
    ReactiveFormsModule,
  ],
  templateUrl: './create-application.component.html',
  styleUrl: './create-application.component.scss',
})
export class CreateApplicationComponent {
  readonly applicationService = inject(ApplicationService);
  readonly router = inject(Router);
  readonly destroyRef = inject(DestroyRef);
  readonly isMobile = inject(ObservabilityBreakpointService).isMobile;

  readonly typeIdControl = new FormControl<string | null>(null, { nonNullable: false });

  readonly form = new FormGroup<BaseControls>({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    clientCertificate: new FormControl('', { nonNullable: true }),
    appType: new FormControl('', { nonNullable: true }),
    appClientId: new FormControl('', { nonNullable: true }),
    metadata: new FormControl<Record<string, string> | null>(null, { nonNullable: false }),
    dynamic: new FormRecord<FormControl<unknown>>({}),
  });

  readonly enabledApplicationTypes = toSignal(this.applicationService.getEnabledApplicationTypes());

  readonly selectedTypeId = toSignal(this.typeIdControl.valueChanges.pipe(startWith(this.typeIdControl.value)), {
    initialValue: null,
  });

  readonly selectedType = computed<ApplicationType | null>(() => {
    const typeId = this.selectedTypeId();
    if (!typeId) return null;
    const types = this.enabledApplicationTypes();
    if (!types) return null;
    return types.find(type => type.id === typeId || type.name === typeId) ?? null;
  });

  readonly isSimpleType = computed(() => this.selectedType()?.id === 'simple');
  readonly requiresRedirectUris = computed(() => this.selectedType()?.requires_redirect_uris ?? false);

  readonly grantTypesList = computed<GrantTypeVM[]>(() => {
    const selectedType = this.selectedType();
    if (!selectedType) return [];

    const allowed = selectedType.allowed_grant_types ?? [];
    const mandatory = selectedType.mandatory_grant_types ?? [];

    return allowed.map(grantType => ({
      type: grantType.type ?? '',
      name: grantType.name ?? grantType.type ?? '',
      isDisabled: mandatory.some(m => m.type === grantType.type),
    }));
  });

  hasApplicationError: boolean = false;

  constructor() {
    this.setupDynamicFormFields();
    this.setupDefaultApplicationType();
  }

  get redirectUrisControl(): FormControl<string[]> | null {
    const control = this.form.controls.dynamic.get('redirectUris');
    return control instanceof FormControl ? control : null;
  }

  get grantTypesControl(): FormControl<string[]> | null {
    const control = this.form.controls.dynamic.get('grantTypes');
    return control instanceof FormControl ? control : null;
  }

  onCreate(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValue = this.form.getRawValue();
    const selectedType = this.selectedType();

    if (!selectedType) {
      return;
    }

    const applicationInput = this.buildApplicationInput(formValue, selectedType);

    this.applicationService
      .create(applicationInput)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: application => {
          this.router.navigate(['/applications', application.id]);
        },
        error: err => {
          this.hasApplicationError = true;
          console.error('Error creating application:', err);
        },
      });
  }

  onCancel(): void {
    this.router.navigate(['/applications']);
  }

  addRedirectUri(event: MatChipInputEvent): void {
    const newRedirectUri = (event.value || '').trim();
    if (!newRedirectUri) return;

    const redirectUrisControl = this.redirectUrisControl;
    if (!redirectUrisControl) return;

    const currentRedirectUris = redirectUrisControl.value ?? [];
    if (currentRedirectUris.includes(newRedirectUri)) {
      event.chipInput!.clear();
      return;
    }

    redirectUrisControl.setValue([...currentRedirectUris, newRedirectUri]);
    event.chipInput!.clear();
  }

  removeRedirectUri(redirectUriToRemove: string): void {
    const redirectUrisControl = this.redirectUrisControl;
    if (!redirectUrisControl) return;

    const currentRedirectUris = redirectUrisControl.value ?? [];
    redirectUrisControl.setValue(currentRedirectUris.filter((uri: string) => uri !== redirectUriToRemove));
  }

  toggleGrantType(grantType: string, checked: boolean): void {
    const grantTypesControl = this.grantTypesControl;
    if (!grantTypesControl) return;

    const currentGrantTypes = grantTypesControl.value ?? [];
    if (checked) {
      if (!currentGrantTypes.includes(grantType)) {
        grantTypesControl.setValue([...currentGrantTypes, grantType]);
      }
    } else {
      grantTypesControl.setValue(currentGrantTypes.filter((gt: string) => gt !== grantType));
    }
  }

  isGrantTypeSelected(grantType: string): boolean {
    const grantTypesControl = this.grantTypesControl;
    if (!grantTypesControl) return false;
    return (grantTypesControl.value ?? []).includes(grantType);
  }

  private setupDynamicFormFields(): void {
    effect(() => {
      const type = this.selectedType();
      const dynamic = this.form.controls.dynamic;

      this.clearDynamicControls(dynamic);

      if (!type) return;

      if (type.requires_redirect_uris) {
        this.addRedirectUrisControl(dynamic);
      }

      if (this.hasGrantTypes(type)) {
        this.addGrantTypesControl(dynamic, type);
      }
    });
  }

  private setupDefaultApplicationType(): void {
    effect(() => {
      const types = this.enabledApplicationTypes();
      if (types && types.length > 0 && this.typeIdControl.value === null) {
        const firstType = types[0];
        this.typeIdControl.setValue(firstType.id ?? firstType.name, { emitEvent: true });
      }
    });
  }

  private clearDynamicControls(dynamic: FormRecord<FormControl<unknown>>): void {
    Object.keys(dynamic.controls).forEach(key => dynamic.removeControl(key));
  }

  private addRedirectUrisControl(dynamic: FormRecord<FormControl<unknown>>): void {
    dynamic.addControl(
      'redirectUris',
      new FormControl<string[]>([], {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(1)],
      }),
    );
  }

  private addGrantTypesControl(dynamic: FormRecord<FormControl<unknown>>, type: ApplicationType): void {
    const defaults = this.extractGrantTypeIds(type.default_grant_types ?? []);
    const mandatory = this.extractGrantTypeIds(type.mandatory_grant_types ?? []);
    const initialGrantTypes = Array.from(new Set<string>([...defaults, ...mandatory]));

    dynamic.addControl(
      'grantTypes',
      new FormControl<string[]>(initialGrantTypes, {
        nonNullable: true,
        validators: [this.createMandatoryGrantTypesValidator(mandatory)],
      }),
    );
  }

  private hasGrantTypes(type: ApplicationType): boolean {
    return (type.allowed_grant_types ?? []).length > 0;
  }

  private extractGrantTypeIds(grantTypes: Array<{ type?: string }>): string[] {
    return grantTypes.map(g => g.type).filter((x): x is string => !!x);
  }

  private createMandatoryGrantTypesValidator(mandatory: string[]): ValidatorFn {
    return (control: AbstractControl) => {
      const val = (control.value as string[]) ?? [];
      const missing = mandatory.filter(m => !val.includes(m));
      return missing.length ? { mandatoryMissing: missing } : null;
    };
  }

  private buildApplicationInput(
    formValue: ReturnType<FormGroup<BaseControls>['getRawValue']>,
    selectedType: ApplicationType,
  ): ApplicationInput {
    const isSimple = selectedType.id === 'simple';

    const settings: ApplicationSettings = isSimple
      ? {
          app: {
            type: formValue.appType?.trim() || undefined,
            client_id: formValue.appClientId?.trim() || undefined,
          },
        }
      : (() => {
          const grantTypes = (formValue.dynamic?.['grantTypes'] as string[] | undefined) ?? [];
          const redirectUris = (formValue.dynamic?.['redirectUris'] as string[] | undefined) ?? [];
          const metadata = formValue.metadata ?? null;

          return {
            oauth: {
              application_type: selectedType.id || selectedType.name,
              grant_types: grantTypes,
              redirect_uris: redirectUris,
              additional_client_metadata: metadata && Object.keys(metadata).length > 0 ? metadata : undefined,
            },
          };
        })();

    if (formValue.clientCertificate?.trim()) {
      settings.tls = {
        client_certificate: formValue.clientCertificate.trim(),
      };
    }

    return {
      name: formValue.name.trim(),
      description: formValue.description?.trim() || undefined,
      settings,
    };
  }
}
