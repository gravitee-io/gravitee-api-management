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
import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { isEmpty } from 'lodash';
import { of, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { IdentityProvider } from '../../../entities/identity-provider';
import { IdentityProviderService } from '../../../services-ngx/identity-provider.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export interface ProviderConfiguration {
  getFormGroups(): Record<string, FormGroup>;
}
@Component({
  selector: 'org-settings-identity-provider',
  styles: [require('./org-settings-identity-provider.component.scss')],
  template: require('./org-settings-identity-provider.component.html'),
})
export class OrgSettingsIdentityProviderComponent implements OnInit, OnDestroy {
  isLoading = true;

  identityProviderFormGroup: FormGroup;

  mode: 'new' | 'edit' = 'new';

  // Used for the edit mode
  initialIdentityProviderValue: IdentityProvider | null = null;

  @ViewChild('providerConfiguration', { static: false })
  set providerConfiguration(providerPart: ProviderConfiguration | undefined) {
    if (providerPart) {
      this.addProviderFormGroups(providerPart.getFormGroups());
    }
  }

  identityProviderType = 'GRAVITEEIO_AM';

  private unsubscribe$ = new Subject<boolean>();

  private identityProviderFormControlKeys: string[] = [];

  constructor(
    private readonly identityProviderService: IdentityProviderService,
    private readonly snackBarService: SnackBarService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  ngOnInit() {
    this.identityProviderFormGroup = new FormGroup({
      type: new FormControl('GRAVITEEIO_AM'),
      enabled: new FormControl(),
      name: new FormControl(null, [Validators.required, Validators.maxLength(50), Validators.minLength(2)]),
      description: new FormControl(),
      emailRequired: new FormControl(),
      syncMappings: new FormControl(),
    });

    this.identityProviderFormGroup
      .get('type')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((type) => {
        this.identityProviderType = type;
      });

    if (this.ajsStateParams.id) {
      this.mode = 'edit';

      this.identityProviderService
        .get(this.ajsStateParams.id)
        .pipe(
          takeUntil(this.unsubscribe$),
          tap((identityProvider) => {
            this.identityProviderType = identityProvider.type;
            this.initialIdentityProviderValue = identityProvider;
            this.isLoading = false;

            // Initializes the form value
            this.identityProviderFormGroup.patchValue(this.initialIdentityProviderValue, { emitEvent: false });
            this.identityProviderFormGroup.markAsPristine();
            this.identityProviderFormGroup.markAsUntouched();
            this.changeDetectorRef.detectChanges();
          }),
        )
        .subscribe();
    } else {
      this.mode = 'new';
      this.isLoading = false;
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addProviderFormGroups(formGroups: Record<string, FormGroup>) {
    if (this.isLoading) {
      return;
    }

    // clean previous form group
    if (!isEmpty(this.identityProviderFormControlKeys)) {
      this.identityProviderFormControlKeys.forEach((key) => {
        this.identityProviderFormGroup.removeControl(key);
      });

      this.identityProviderFormControlKeys = [];
    }

    // add provider form group
    if (this.identityProviderFormGroup && !isEmpty(formGroups)) {
      Object.entries(formGroups).forEach(([key, formGroup]) => {
        this.identityProviderFormControlKeys.push(key);
        this.identityProviderFormGroup.addControl(key, formGroup);
      });
    }

    // For the edit mode
    // Initializes the form value when the sub-form linked to the idP type is added
    if (this.mode === 'edit') {
      this.identityProviderFormGroup.patchValue(this.initialIdentityProviderValue, { emitEvent: false });
      this.identityProviderFormGroup.markAsPristine();
      this.identityProviderFormGroup.markAsUntouched();
      this.changeDetectorRef.detectChanges();
    }
  }

  onSubmit() {
    if (this.identityProviderFormGroup.invalid) {
      return;
    }

    const formSettingsValue = this.identityProviderFormGroup.getRawValue();

    const upsertIdentityProvider$ = this.mode === 'new' ? this.identityProviderService.create(formSettingsValue) : of();

    upsertIdentityProvider$
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('Identity provider successfully saved!')),
      )
      .subscribe();
  }
}
