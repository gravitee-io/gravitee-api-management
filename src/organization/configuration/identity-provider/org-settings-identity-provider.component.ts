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
import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { isEmpty } from 'lodash';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface ProviderConfiguration {
  getFormGroups(): Record<string, FormGroup>;
}
@Component({
  selector: 'org-settings-identity-provider',
  styles: [require('./org-settings-identity-provider.component.scss')],
  template: require('./org-settings-identity-provider.component.html'),
})
export class OrgSettingsIdentityProviderComponent implements OnInit, AfterViewInit, OnDestroy {
  identityProviderSettings: FormGroup;

  @ViewChild('providerConfiguration', { static: false })
  set providerConfiguration(providerPart: ProviderConfiguration) {
    this.addProviderFormGroups(providerPart.getFormGroups());
  }

  identityProviderType = 'GRAVITEEIO_AM';

  private unsubscribe$ = new Subject<boolean>();

  private identityProviderFormControlKeys: string[] = [];

  ngOnInit() {
    this.identityProviderSettings = new FormGroup({
      type: new FormControl('GRAVITEEIO_AM'),
      enabled: new FormControl(),
      name: new FormControl(null, [Validators.required, Validators.maxLength(50), Validators.minLength(2)]),
      description: new FormControl(),
      tokenExchangeEndpoint: new FormControl(),
      emailRequired: new FormControl(),
      syncMappings: new FormControl(),
    });
  }

  ngAfterViewInit() {
    this.identityProviderSettings
      .get('type')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((type) => {
        this.identityProviderType = type;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addProviderFormGroups(formGroups: Record<string, FormGroup>) {
    // clean previous form group
    if (!isEmpty(this.identityProviderFormControlKeys)) {
      this.identityProviderFormControlKeys.forEach((key) => {
        this.identityProviderSettings.removeControl(key);
      });

      this.identityProviderFormControlKeys = [];
    }

    // add provider form group
    if (this.identityProviderSettings && !isEmpty(formGroups)) {
      Object.entries(formGroups).forEach(([key, formGroup]) => {
        this.identityProviderFormControlKeys.push(key);
        this.identityProviderSettings.addControl(key, formGroup);
      });
    }
  }
}
