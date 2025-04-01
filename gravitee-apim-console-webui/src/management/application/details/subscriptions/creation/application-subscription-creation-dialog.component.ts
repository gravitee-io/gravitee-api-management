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
import { Component, DestroyRef, inject, Inject } from '@angular/core';
import { FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, share, switchMap, tap } from 'rxjs/operators';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { toNewSubscriptionEntity } from './application-subscription-creation-dialog.adapter';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { Api, Plan, PlanV4 } from '../../../../../entities/management-api-v2';
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { ApiKeyMode, Application } from '../../../../../entities/application/Application';
import { SubscriptionPage } from '../../../../../entities/subscription/subscription';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import {
  ApplicationSubscriptionCreationDialogData,
  ApplicationSubscriptionCreationDialogResult,
} from '../list/application-subscription-list.component';

type SubscriptionCreationForm = FormGroup<{
  selectedApi: FormControl<string | Api>;
  selectedPlan: FormControl<Plan>;
  request?: FormControl<string>;
  apiKeyMode?: FormControl<ApiKeyMode>;
  selectedEntrypoint?: FormControl<string>;
  channel?: FormControl<string>;
  entrypointConfiguration?: FormControl<any>;
}>;

@Component({
  selector: 'application-subscription-creation-dialog',
  templateUrl: './application-subscription-creation-dialog.component.html',
  styleUrls: ['./application-subscription-creation-dialog.component.scss'],
  standalone: false,
})
export class ApplicationSubscriptionCreationDialogComponent {
  private destroyRef = inject(DestroyRef);
  private canUseSharedApiKeys = this.environmentSettingsService.getSnapshot().plan?.security?.sharedApiKey?.enabled;
  private isFederatedApi = false;
  private apiKeySubscriptions: SubscriptionPage[];
  private entrypointsMap = new Map<string, { icon: string; name: string }>();
  protected availableSubscriptionEntrypoints: { type: string; name?: string; icon?: string }[] = [];
  protected application: Application;
  protected selectedSchema: GioJsonSchema;
  protected form: SubscriptionCreationForm = new FormGroup({
    selectedApi: new FormControl(null),
    selectedPlan: new FormControl(null, [Validators.required]),
  });
  protected plans$: Observable<Plan[]>;
  protected apis$: Observable<Api[]> = this.form.controls.selectedApi.valueChanges.pipe(
    distinctUntilChanged(),
    debounceTime(100),
    filter((term) => typeof term === 'string'),
    switchMap((term: string) => (term.length > 0 ? this.apiService.search({ query: term }, null, 1, 9999, false) : of(null))),
    tap((_) => this.resetForm()),
    map((response) => response?.data),
    share(),
  );

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiPlanService: ApiPlanV2Service,
    private readonly applicationService: ApplicationService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
    private readonly dialogRef: MatDialogRef<ApplicationSubscriptionCreationDialogComponent, ApplicationSubscriptionCreationDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApplicationSubscriptionCreationDialogData,
  ) {
    this.applicationService
      .getLastApplicationFetch(dialogData.applicationId)
      .pipe(
        tap((application) => {
          this.application = application;
          const clientId = application.settings?.app?.client_id ?? application.settings?.oauth?.client_id;
          this.form.controls.selectedPlan.addValidators(clientIdRequiredValidator(clientId));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.applicationService
      .getSubscriptionsPage(dialogData.applicationId, { status: ['ACCEPTED', 'PAUSED', 'PENDING'], security_types: ['API_KEY'] })
      .pipe(
        tap((response) => (this.apiKeySubscriptions = response.data)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.connectorPluginsV2Service
      .listEntrypointPlugins()
      .pipe(
        tap((entrypointPlugins) =>
          entrypointPlugins
            .filter((plugin) => plugin.supportedListenerType === 'SUBSCRIPTION')
            .map((plugin) =>
              this.entrypointsMap.set(plugin.id, { icon: this.iconService.registerSvg(plugin.id, plugin.icon), name: plugin.name }),
            ),
        ),
      )
      .subscribe();

    this.onPlanSelectionChange();
    this.onPushPlanChange();
    this.onApiKeyPlanChange();
    this.onJwtOauth2PlanChange();
  }

  protected create() {
    this.dialogRef.close({
      planId: this.form.controls.selectedPlan.value.id,
      subscriptionToCreate: toNewSubscriptionEntity(this.form.getRawValue()),
    });
  }

  protected displayApi(api: Api) {
    return api?.name;
  }

  protected onApiSelection(selectedApi: Api) {
    this.resetForm();
    this.plans$ = this.apiPlanService.listSubscribablePlans(selectedApi.id, this.application.id).pipe(
      map((response) => response.data),
      share(),
      takeUntilDestroyed(this.destroyRef),
    );
    this.isFederatedApi = selectedApi.definitionVersion === 'FEDERATED';
    if (selectedApi.definitionVersion === 'V4') {
      this.availableSubscriptionEntrypoints = selectedApi.listeners
        .filter((listener) => listener.type === 'SUBSCRIPTION')
        .flatMap((listener) => listener.entrypoints)
        .map((listener) => {
          const entrypoint = this.entrypointsMap.get(listener.type);
          return entrypoint ? { type: listener.type, name: entrypoint.name, icon: entrypoint.icon } : null;
        });
    }
  }

  private onPlanSelectionChange() {
    this.form.controls.selectedPlan.valueChanges
      .pipe(
        filter((plan) => !!plan),
        tap((plan) => {
          if (plan.commentRequired) {
            this.form.addControl('request', new FormControl(null, Validators.required));
          } else {
            this.form.removeControl('request');
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private onPushPlanChange() {
    this.form.controls.selectedPlan.valueChanges
      .pipe(
        distinctUntilChanged(),
        filter((plan) => plan?.definitionVersion === 'V4' && (plan as PlanV4)?.mode === 'PUSH'),
        tap(() => {
          this.removeFormControls(['apiKeyMode']);
          this.form.addControl('selectedEntrypoint', new FormControl(null, Validators.required));
          this.form.addControl('channel', new FormControl(null));
        }),
        switchMap(() => this.form.controls.selectedEntrypoint.valueChanges),
        switchMap((entrypointId) => this.connectorPluginsV2Service.getEntrypointPluginSubscriptionSchema(entrypointId)),
        tap((subscriptionSchema) => {
          this.selectedSchema = subscriptionSchema;
          this.form.addControl('entrypointConfiguration', new FormControl({}, Validators.required));
        }),
      )
      .subscribe();
  }

  private onApiKeyPlanChange() {
    this.form.controls.selectedPlan.valueChanges
      .pipe(
        distinctUntilChanged(),
        filter((plan) => plan?.security?.type === 'API_KEY'),
        switchMap((plan) =>
          of(
            !this.isFederatedApi &&
              this.canUseSharedApiKeys &&
              this.application.api_key_mode === ApiKeyMode.UNSPECIFIED &&
              this.apiKeySubscriptions.some((subscription) => subscription?.api !== plan.apiId),
          ),
        ),
        tap((shouldDisplayKeyModeChoice) => {
          if (shouldDisplayKeyModeChoice) {
            this.form.addControl('apiKeyMode', new FormControl(null, [Validators.required]));
          }
          this.removeFormControls(['selectedEntrypoint', 'channel', 'entrypointConfiguration']);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private onJwtOauth2PlanChange() {
    this.form.controls.selectedPlan.valueChanges
      .pipe(
        distinctUntilChanged(),
        filter((plan) => ['JWT', 'OAUTH2'].includes(plan?.security?.type)),
        tap(() => this.removeFormControls(['apiKeyMode', 'selectedEntrypoint', 'channel', 'entrypointConfiguration'])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private resetForm() {
    this.form.controls.selectedPlan.reset();
    this.plans$ = undefined;
    this.removeFormControls(['apiKeyMode', 'selectedEntrypoint', 'channel', 'entrypointConfiguration']);
  }

  private removeFormControls(names: ('apiKeyMode' | 'selectedEntrypoint' | 'channel' | 'entrypointConfiguration')[]) {
    names.forEach((name) => this.form.removeControl(name));
  }
}

export const clientIdRequiredValidator = (clientId: string): ValidatorFn => {
  return (control): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    return !clientId && ['JWT', 'OAUTH2'].includes(control.value?.security?.type) ? { clientIdRequired: true } : null;
  };
};
