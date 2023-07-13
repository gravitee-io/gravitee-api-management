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
import { EMPTY, Observable, of, Subject } from 'rxjs';
import { FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, filter, map, share, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { has } from 'lodash';

import { CreateSubscription, Entrypoint, Plan } from '../../../../../../../entities/management-api-v2';
import { ApplicationService } from '../../../../../../../services-ngx/application.service';
import { ApiKeyMode, Application } from '../../../../../../../entities/application/application';
import { PagedResult } from '../../../../../../../entities/pagedResult';
import { Constants } from '../../../../../../../entities/Constants';
import { ConnectorPluginsV2Service } from '../../../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../../../services-ngx/icon.service';
import { PlanSecurityType } from '../../../../../../../entities/plan';
import { SubscriptionService } from '../../../../../../../services-ngx/subscription.service';

export type ApiPortalSubscriptionCreationDialogData = {
  availableSubscriptionEntrypoints?: Entrypoint[];
  plans: Plan[];
};

export type ApiPortalSubscriptionCreationDialogResult = {
  application: Application;
  apiKeyMode?: ApiKeyMode;
  subscriptionToCreate: CreateSubscription;
};

@Component({
  selector: 'api-portal-subscription-creation-dialog',
  template: require('./api-portal-subscription-creation-dialog.component.html'),
  styles: [require('./api-portal-subscription-creation-dialog.component.scss')],
})
export class ApiPortalSubscriptionCreationDialogComponent implements OnInit, OnDestroy {
  public plans: Plan[];
  public availableSubscriptionEntrypoints: { type: string; name?: string; icon?: string }[];
  public applications$: Observable<Application[]> = new Observable<Application[]>();
  public selectedSchema: GioJsonSchema;
  public showGeneralConditionsMsg: boolean;
  public canUseCustomApiKey: boolean;
  public canUseSharedApiKeys: boolean;

  public form: FormGroup = new FormGroup({
    selectedPlan: new FormControl(undefined, [Validators.required]),
    selectedApplication: new FormControl(undefined, [applicationSelectionRequiredValidator]),
  });

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionCreationDialogComponent, ApiPortalSubscriptionCreationDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionCreationDialogData,
    @Inject('Constants') private readonly constants: Constants,
    private readonly applicationService: ApplicationService,
    private readonly subscriptionService: SubscriptionService,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
  ) {
    this.plans = dialogData.plans.filter((plan) => plan.security?.type !== 'KEY_LESS');
    this.availableSubscriptionEntrypoints = dialogData.availableSubscriptionEntrypoints.map((entrypoint) => ({ type: entrypoint.type }));
    this.canUseCustomApiKey = this.constants.env?.settings?.plan?.security?.customApiKey?.enabled;
    this.canUseSharedApiKeys = this.constants.env?.settings?.plan?.security?.sharedApiKey?.enabled;
  }

  ngOnInit(): void {
    this.showGeneralConditionsMsg = this.plans.some((plan) => plan.generalConditions);

    this.prepareSubscriptionEntrypoints();

    this.applications$ = this.onApplicationChange();

    this.onPushPlanChange();
    this.onApiKeyPlanChange();
    this.onApiKeyModeChange();
    this.onJwtOrOauth2PlanChange();
  }

  onCreate() {
    const dialogResult = {
      application: this.form.getRawValue().selectedApplication,
      ...(this.form.getRawValue().apiKeyMode && this.form.getRawValue().apiKeyMode !== ''
        ? {
            apiKeyMode: this.form.getRawValue().apiKeyMode,
          }
        : {}),
      subscriptionToCreate: {
        planId: this.form.getRawValue().selectedPlan.id,
        applicationId: this.form.getRawValue().selectedApplication.id,
        ...(this.form.getRawValue().customApiKey && this.form.getRawValue().customApiKey !== ''
          ? {
              customApiKey: this.form.getRawValue().customApiKey,
            }
          : {}),
        ...(this.form.getRawValue().selectedPlan.mode === 'PUSH'
          ? {
              consumerConfiguration: {
                channel: this.form.getRawValue().channel ?? undefined,
                entrypointId: this.form.getRawValue().selectedEntrypoint ?? undefined,
                entrypointConfiguration: this.form.getRawValue().entrypointConfiguration ?? undefined,
              },
            }
          : undefined),
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

  private prepareSubscriptionEntrypoints() {
    if (this.availableSubscriptionEntrypoints.length > 0) {
      this.connectorPluginsV2Service
        .listEntrypointPlugins()
        .pipe(
          tap((entrypointPlugins) =>
            this.availableSubscriptionEntrypoints.forEach((entrypoint) => {
              const connectorPlugin = entrypointPlugins.find((e) => e.id === entrypoint.type);
              if (connectorPlugin) {
                entrypoint.name = connectorPlugin.name;
                entrypoint.icon = this.iconService.registerSvg(connectorPlugin.id, connectorPlugin.icon);
              }
            }),
          ),
        )
        .subscribe();
    }
  }

  private onApplicationChange() {
    return this.form.get('selectedApplication').valueChanges.pipe(
      distinctUntilChanged(),
      debounceTime(100),
      switchMap((term) =>
        term.length > 0 ? this.applicationService.list('ACTIVE', term, 'name', 1, 20) : of(new PagedResult<Application>()),
      ),
      map((applicationsPage) => applicationsPage.data),
      tap((_) => {
        this.form.get('selectedPlan')?.reset();
        this.form.get('customApiKey')?.reset();
        this.form.removeControl('apiKeyMode');
        this.form.removeControl('customApiKey');
      }),
      share(),
      takeUntil(this.unsubscribe$),
    );
  }

  private onPushPlanChange() {
    this.form
      .get('selectedPlan')
      .valueChanges.pipe(
        distinctUntilChanged(),
        filter((plan) => plan?.mode === 'PUSH'),
        tap(() => {
          this.form.removeControl('apiKeyMode');
          this.form.removeControl('customApiKey');
          this.form.addControl('selectedEntrypoint', new FormControl({}, Validators.required));
          this.form.addControl('channel', new FormControl(undefined, []));
        }),
        switchMap(() => this.form.get('selectedEntrypoint').valueChanges),
        switchMap((entrypointId) => this.connectorPluginsV2Service.getEntrypointPluginSubscriptionSchema(entrypointId)),
        tap((subscriptionSchema) => {
          this.selectedSchema = subscriptionSchema;
          this.form.addControl('entrypointConfiguration', new FormControl({}, Validators.required));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private onApiKeyPlanChange() {
    this.form
      .get('selectedPlan')
      .valueChanges.pipe(
        distinctUntilChanged(),
        filter((plan) => plan?.security?.type === 'API_KEY'),
        switchMap((plan) => {
          if (this.canUseSharedApiKeys && this.form.get('selectedApplication').value.api_key_mode === ApiKeyMode.UNSPECIFIED) {
            return this.subscriptionService.getApplicationSubscriptions(this.form.get('selectedApplication').value.id).pipe(
              map((subscriptions) => {
                return (
                  subscriptions.data.filter(
                    (subscription) => subscription?.security === PlanSecurityType.API_KEY && subscription?.api?.id !== plan?.apiId,
                  ).length >= 1
                );
              }),
            );
          }
          return of(false);
        }),
        tap((shouldDisplayKeyModeChoice) => {
          if (shouldDisplayKeyModeChoice) {
            this.form.addControl('apiKeyMode', new FormControl('', [Validators.required]));
          }
          if (this.canUseCustomApiKey && !shouldDisplayKeyModeChoice) {
            this.form.addControl('customApiKey', new FormControl('', []));
          }
          this.form.removeControl('selectedEntrypoint');
          this.form.removeControl('channel');
          this.form.removeControl('entrypointConfiguration');
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private onApiKeyModeChange() {
    this.form.valueChanges
      .pipe(
        switchMap(() => {
          if (this.form.get('apiKeyMode')) {
            return this.form.get('apiKeyMode').valueChanges;
          }
          return EMPTY;
        }),
        distinctUntilChanged(),
        tap((value) => {
          if (this.canUseCustomApiKey && value === ApiKeyMode.EXCLUSIVE) {
            this.form.addControl('customApiKey', new FormControl('', []));
          } else {
            this.form.removeControl('customApiKey');
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private onJwtOrOauth2PlanChange() {
    this.form
      .get('selectedPlan')
      .valueChanges.pipe(
        distinctUntilChanged(),
        filter((plan) => plan?.security?.type === 'OAUTH2' || plan?.security?.type === 'JWT'),
        tap(() => {
          this.form.removeControl('apiKeyMode');
          this.form.removeControl('customApiKey');
          this.form.removeControl('selectedEntrypoint');
          this.form.removeControl('channel');
          this.form.removeControl('entrypointConfiguration');
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}

const applicationSelectionRequiredValidator: ValidatorFn = (control): ValidationErrors | null => {
  const value = control?.value;
  if (!value || typeof value === 'string' || !('id' in value) || !('name' in value)) {
    return { selectionRequired: true };
  }
  if (
    control.parent?.get('selectedPlan').value?.security?.type === 'JWT' ||
    control.parent?.get('selectedPlan').value?.security?.type === 'OAUTH2'
  ) {
    return has(value, 'settings.app.client_id') ? null : { clientIdRequired: true };
  }

  return null;
};
