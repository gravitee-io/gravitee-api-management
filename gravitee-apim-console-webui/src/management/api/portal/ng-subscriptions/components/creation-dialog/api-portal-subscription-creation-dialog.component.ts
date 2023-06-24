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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, Subject } from 'rxjs';
import { FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map, share, switchMap, takeUntil } from 'rxjs/operators';

import { CreateSubscription, Plan } from '../../../../../../entities/management-api-v2';
import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApplicationService } from '../../../../../../services-ngx/application.service';
import { Application } from '../../../../../../entities/application/application';
import { PagedResult } from '../../../../../../entities/pagedResult';
import { Constants } from '../../../../../../entities/Constants';

export type ApiPortalSubscriptionCreationDialogData = {
  plans: Plan[];
};

export type ApiPortalSubscriptionCreationDialogResult = {
  subscriptionToCreate: CreateSubscription;
};

@Component({
  selector: 'api-portal-subscription-creation-dialog',
  template: require('./api-portal-subscription-creation-dialog.component.html'),
  styles: [require('./api-portal-subscription-creation-dialog.component.scss')],
})
export class ApiPortalSubscriptionCreationDialogComponent implements OnInit, OnDestroy {
  public plans: Plan[];
  public applications$: Observable<Application[]> = new Observable<Application[]>();
  public showGeneralConditionsMsg: boolean;
  public canUseCustomApikey: boolean;
  public form: FormGroup = new FormGroup({
    selectedPlan: new FormControl(undefined, [Validators.required]),
    selectedApplication: new FormControl(undefined, [applicationSelectionRequiredValidator]),
  });

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionCreationDialogComponent, ApiPortalSubscriptionCreationDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionCreationDialogData,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject('Constants') private readonly constants: Constants,
    private readonly applicationService: ApplicationService,
  ) {
    this.plans = dialogData.plans.filter((plan) => plan.security?.type !== 'KEY_LESS');
    this.canUseCustomApikey = this.constants.env?.settings?.plan?.security?.customApiKey?.enabled;
    if (this.canUseCustomApikey) {
      this.form.addControl('customApiKey', new FormControl('', []));
    }
  }

  ngOnInit(): void {
    this.showGeneralConditionsMsg = this.plans.some((plan) => plan.generalConditions);

    this.applications$ = this.form.get('selectedApplication').valueChanges.pipe(
      distinctUntilChanged(),
      debounceTime(100),
      switchMap((term) =>
        term.length > 0 ? this.applicationService.list('ACTIVE', term, 'name', 1, 20) : of(new PagedResult<Application>()),
      ),
      map((applicationsPage) => applicationsPage.data),
      share(),
      takeUntil(this.unsubscribe$),
    );
  }

  onCreate() {
    const dialogResult = {
      subscriptionToCreate: {
        planId: this.form.getRawValue().selectedPlan.id,
        applicationId: this.form.getRawValue().selectedApplication.id,
        ...(this.shouldDisplayCustomApiKey()  && this.form.getRawValue().customApiKey ? { customApiKey: this.form.getRawValue().customApiKey } : undefined),
      },
    };
    this.dialogRef.close(dialogResult);
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  displayApplication(application: Application): string {
    return application?.name;
  }

  shouldDisplayCustomApiKey(): boolean {
    return this.canUseCustomApikey && this.form.get('selectedPlan').value?.security?.type === 'API_KEY';
  }
}

const applicationSelectionRequiredValidator: ValidatorFn = (control): ValidationErrors | null => {
  const value = control?.value;
  if (value && typeof value !== 'string' && 'id' in value && 'name' in value) {
    return null;
  }
  return { selectionRequired: true };
};
