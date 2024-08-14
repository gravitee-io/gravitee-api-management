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
import { Component, EventEmitter, HostListener, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { ApplicationTypeOption } from '../application-creation.component';

export type AppFormType = FormGroup<{
  app: FormGroup<{ type: FormControl<string>; client_id: FormControl<string> }>;
  tls: FormGroup<{ client_certificate: FormControl<string> }>;
}> | null;
export type OAuthFormType = FormGroup<{
  oauth: FormGroup<{
    redirect_uris: FormArray;
    grant_types: FormArray;
    application_type: FormControl<string>;
    additionalClientMetadata: FormArray;
  }>;
  tls: FormGroup<{ client_certificate: FormControl<string> }>;
}>;

@Component({
  selector: 'app-application-creation-step2',
  templateUrl: './application-creation-step2.component.html',
  styleUrls: ['../application-creation.component.css'],
})
export class ApplicationCreationStep2Component implements OnInit, OnChanges {
  @Input() allowedTypes: Array<ApplicationTypeOption>;
  @Input() requireClientId: boolean;
  @Output() applicationTypeSelected = new EventEmitter<ApplicationTypeOption>();
  @Output() updated = new EventEmitter<AppFormType | OAuthFormType>();
  applicationType: ApplicationTypeOption;
  allGrantTypes: { name?: string; disabled: boolean; type?: string; value: boolean }[];
  oauthForm: OAuthFormType;
  appForm: AppFormType;
  private formSubscription: Subscription;

  constructor(private formBuilder: FormBuilder) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes.requireClientId && this.appForm && changes.requireClientId.previousValue !== changes.requireClientId.currentValue) {
      if (this.requireClientId) {
        this.appForm.get('app.client_id').setValidators([Validators.required]);
        this.appForm.get('app.client_id').updateValueAndValidity();
      } else {
        this.appForm.get('app.client_id').setValidators(null);
        this.appForm.get('app.client_id').updateValueAndValidity();
      }
    }
  }

  ngOnInit(): void {
    const firstApplicationType = this.allowedTypes[0];

    this.appForm = this.formBuilder.group({
      app: this.formBuilder.group({
        type: new FormControl('', null),
        client_id: new FormControl('', null),
      }),
      tls: this.formBuilder.group({
        client_certificate: new FormControl(''),
      }),
    });
    this.oauthForm = this.formBuilder.group({
      oauth: this.formBuilder.group({
        redirect_uris: new FormArray([], null),
        grant_types: new FormArray([], [Validators.required]),
        application_type: new FormControl(null, [Validators.required]),
        additionalClientMetadata: new FormArray([]),
      }),
      tls: this.formBuilder.group({
        client_certificate: new FormControl(''),
      }),
    });
    this.setApplicationType(firstApplicationType);
  }

  @HostListener(':gv-option:select', ['$event.detail'])
  setApplicationType(applicationType: ApplicationTypeOption) {
    if (this.formSubscription) {
      this.formSubscription.unsubscribe();
    }
    this.applicationType = applicationType;
    this.grantTypes.clear();
    this.allGrantTypes = this.applicationType.allowed_grant_types.map(allowedGrantType => {
      const value = this.applicationType.default_grant_types.find(grant => allowedGrantType.type === grant.type) != null;

      const disabled = this.applicationType.mandatory_grant_types.find(grant => allowedGrantType.type === grant.type) != null;

      if (value === true) {
        this.grantTypes.push(new FormControl(allowedGrantType.type));
      }
      return { ...allowedGrantType, disabled, value };
    });

    if (this.isSimpleApp) {
      this.formSubscription = this.appForm.valueChanges.subscribe(() => {
        this.updated.emit(this.appForm);
      });
      this.updated.emit(this.appForm);
    } else {
      this.oauthForm.get('oauth').get('application_type').setValue(this.applicationType.id);

      if (!this.requiresRedirectUris) {
        this.redirectURIs.clear();
        this.oauthForm.get('oauth').get('redirect_uris').setValidators([]);
        this.oauthForm.get('oauth').get('redirect_uris').updateValueAndValidity();
      } else {
        this.oauthForm.get('oauth').get('redirect_uris').setValidators([Validators.required]);
        this.oauthForm.get('oauth').get('redirect_uris').updateValueAndValidity();
      }

      this.formSubscription = this.oauthForm.valueChanges.subscribe(() => {
        this.updated.emit(this.oauthForm);
      });
      this.updated.emit(this.oauthForm);
    }

    this.applicationTypeSelected.emit(this.applicationType);
  }

  get redirectURIs() {
    return this.oauthForm.get('oauth.redirect_uris') as FormArray;
  }

  get grantTypes() {
    return this.oauthForm.get('oauth.grant_types') as FormArray;
  }

  get requiresRedirectUris() {
    return this.applicationType.requires_redirect_uris;
  }

  get isSimpleApp() {
    return this.applicationType.id.toLowerCase() === 'simple';
  }

  removeRedirectUri(index: number) {
    this.redirectURIs.removeAt(index);
  }

  get hasRedirectUris() {
    return this.redirectURIs.length > 0;
  }

  onSwitchGrant(event, grantType) {
    if (event.target.value) {
      this.grantTypes.push(new FormControl(grantType.type));
    } else {
      let index = -1;
      this.grantTypes.controls.forEach((control, i) => {
        if (control.value === grantType.type) {
          index = i;
          return;
        }
      });
      this.grantTypes.removeAt(index);
    }
  }

  addRedirectUri(event) {
    if (event.target.valid) {
      const value = event.target.value;
      if (value && value.trim() !== '') {
        if (!this.redirectURIs.controls.map(c => c.value).includes(value)) {
          this.redirectURIs.push(new FormControl(value, Validators.required));
        }
        event.target.value = '';
      }
    }
  }

  addMetadata() {
    this.oauthForm.controls.oauth.controls.additionalClientMetadata.push(
      this.formBuilder.group({
        key: new FormControl('', Validators.required),
        value: new FormControl('', Validators.required),
      }),
    );
  }

  removeMetadata(i: number) {
    this.oauthForm.controls.oauth.controls.additionalClientMetadata.removeAt(i);
  }

  get metadataControls() {
    return this.oauthForm.controls.oauth.controls.additionalClientMetadata.controls as FormGroup[];
  }
}
