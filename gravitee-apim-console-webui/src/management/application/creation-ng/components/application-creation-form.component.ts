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
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, DestroyRef, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { GioBannerModule, GioFormSelectionInlineModule, GioFormTagsInputModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatRadioModule } from '@angular/material/radio';
import { filter, map, share, startWith, tap } from 'rxjs/operators';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';

import { ApplicationType } from '../../../../entities/application-type/ApplicationType';

export type ApplicationForm = {
  name: FormControl<string>;
  description: FormControl<string>;
  domain: FormControl<string>;
  type: FormControl<string>;

  appType: FormControl<string>;
  appClientId: FormControl<string>;
  appClientCertificate: FormControl<string>;

  oauthGrantTypes: FormControl<string[]>;
  oauthRedirectUris: FormControl<string[]>;
};

export type ApplicationCreationFormApplicationType = ApplicationType & {
  title: string;
  subtitle: string;
  icon: string;
};

@Component({
  selector: 'application-creation-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonToggleModule,
    MatRadioModule,
    MatSelectModule,
    GioFormSelectionInlineModule,
    GioIconsModule,
    GioFormTagsInputModule,
    GioBannerModule,
  ],
  standalone: true,
  styleUrls: ['./application-creation-form.component.scss'],
  templateUrl: './application-creation-form.component.html',
})
export class ApplicationCreationFormComponent implements OnInit {
  private destroyRef = inject(DestroyRef);

  @Input({ required: true })
  public applicationTypes: ApplicationCreationFormApplicationType[];

  @Input({ required: true })
  public applicationFormGroup: FormGroup<ApplicationForm>;

  public applicationType$?: Observable<
    ApplicationType & {
      isOauth: boolean;
      requiresRedirectUris: boolean;
      allowedGrantTypesVM: { value: string; label: string; disabled: boolean }[];
    }
  >;

  ngOnInit() {
    this.applicationType$ = this.applicationFormGroup.get('type').valueChanges.pipe(
      startWith(this.applicationFormGroup.get('type').value),
      filter((typeSelected) => !!typeSelected),
      map((typeSelected) => {
        const applicationType = this.applicationTypes.find(
          (applicationType) => applicationType.id.toUpperCase() === typeSelected.toUpperCase(),
        );
        return {
          ...applicationType,
          isOauth: applicationType.id.toUpperCase() !== 'SIMPLE',
          requiresRedirectUris: applicationType.requires_redirect_uris,
          allowedGrantTypesVM: applicationType.allowed_grant_types.map((grantType) => ({
            value: grantType.type,
            label: grantType.name,
            disabled: applicationType.mandatory_grant_types.some((mandatoryGrantType) => mandatoryGrantType.type === grantType.type),
          })),
        };
      }),
      tap((applicationType) => {
        if (applicationType.isOauth && applicationType.allowedGrantTypesVM.length > 0) {
          this.applicationFormGroup.get('oauthGrantTypes')?.setValidators(Validators.required);

          const defaultGrantType = applicationType?.default_grant_types?.map((grantType) => grantType.type) ?? [];
          this.applicationFormGroup.get('oauthGrantTypes')?.setValue(defaultGrantType);
        } else {
          this.applicationFormGroup.get('oauthGrantTypes')?.clearValidators();
          this.applicationFormGroup.get('oauthGrantTypes')?.reset();
        }

        if (applicationType.requiresRedirectUris) {
          this.applicationFormGroup.get('oauthRedirectUris')?.setValidators(Validators.required);
        } else {
          this.applicationFormGroup.get('oauthRedirectUris')?.clearValidators();
          this.applicationFormGroup.get('oauthRedirectUris')?.reset();
        }
      }),
      share(),
    );
  }
}
