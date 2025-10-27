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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { STEPPER_GLOBAL_OPTIONS } from '@angular/cdk/stepper';
import { combineLatest, of, Subject } from 'rxjs';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { distinctUntilChanged, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiEndpointGroupSelectionComponent } from '../selection/api-endpoint-group-selection.component';
import { ApiEndpointGroupConfigurationComponent } from '../configuration/api-endpoint-group-configuration.component';
import { ApiEndpointGroupGeneralComponent } from '../general/api-endpoint-group-general.component';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiType, ApiV4, EndpointGroupV4, EndpointV4Default, Qos, UpdateApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { isEndpointNameUnique } from '../../api-endpoint-v4-unique-name';

@Component({
  selector: 'api-endpoints-group-create',
  templateUrl: './api-endpoint-group-create.component.html',
  styleUrls: ['./api-endpoint-group-create.component.scss'],
  standalone: false,
  providers: [
    {
      provide: STEPPER_GLOBAL_OPTIONS,
      useValue: { displayDefaultIndicatorType: false, showError: true },
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiEndpointGroupCreateComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @ViewChild(ApiEndpointGroupSelectionComponent)
  private apiEndpointGroupsSelectionComponent: ApiEndpointGroupSelectionComponent;

  @ViewChild(ApiEndpointGroupGeneralComponent)
  private apiEndpointGroupGeneralComponent: ApiEndpointGroupGeneralComponent;

  @ViewChild(ApiEndpointGroupConfigurationComponent)
  private apiEndpointGroupConfigurationComponent: ApiEndpointGroupConfigurationComponent;

  public createForm: UntypedFormGroup;
  public endpointGroupTypeForm: UntypedFormGroup;
  public generalForm: UntypedFormGroup;
  public configuration: UntypedFormControl;
  public sharedConfiguration: UntypedFormControl;
  public configurationSchema: GioJsonSchema;
  public sharedConfigurationSchema: GioJsonSchema;
  public apiType: ApiType;
  public dlqEntrypointType: string | null;
  public createButtonText: string;
  public requiredQos: Qos[];

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.initCreateForm();
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: (api) => {
          const apiV4 = api as ApiV4;
          this.generalForm.get('name').addValidators([isEndpointNameUnique(apiV4)]);
          this.apiType = apiV4.type;
          this.requiredQos = apiV4.listeners.flatMap((listener) => listener.entrypoints).flatMap((entrypoint) => entrypoint.qos);
          if (this.apiType === 'PROXY') {
            this.endpointGroupTypeForm.get('endpointGroupType').setValue('http-proxy');
          }
          if (this.apiType === 'LLM_PROXY') {
            this.endpointGroupTypeForm.get('endpointGroupType').setValue('llm-proxy');
          }
          this.changeDetectorRef.detectChanges();
        },
      });

    this.verifyEntrypointForDlq();

    this.endpointGroupTypeForm
      .get('endpointGroupType')
      .valueChanges.pipe(
        distinctUntilChanged(),
        switchMap((type) => {
          // Hide GioJsonSchemaForm to reset
          this.configurationSchema = undefined;
          this.sharedConfigurationSchema = undefined;

          this.createForm.setControl('configuration', new UntypedFormControl({}, Validators.required));
          this.createForm.setControl('sharedConfiguration', new UntypedFormControl({}, Validators.required));

          if (!type || type === 'mock') {
            this.createForm.removeControl('configuration');
            return of([undefined, undefined]);
          }

          return combineLatest([
            this.connectorPluginsV2Service.getEndpointPluginSchema(type),
            this.connectorPluginsV2Service.getEndpointPluginSharedConfigurationSchema(type),
          ]);
        }),
        tap(([schema, sharedSchema]) => {
          this.configurationSchema = schema;
          this.sharedConfigurationSchema = sharedSchema;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  createEndpointGroup() {
    const formValue = this.createForm.getRawValue();
    const configuration = formValue.configuration;
    const sharedConfiguration = formValue.sharedConfiguration;
    const cleanName = formValue.general.name.trim();

    const newEndpointGroup: EndpointGroupV4 = {
      name: cleanName,
      loadBalancer: { type: formValue.general.loadBalancerType },
      type: formValue.type.endpointGroupType,
      endpoints: [
        {
          ...EndpointV4Default.byTypeAndGroupName(formValue.type.endpointGroupType, cleanName),
          ...(configuration ? { configuration } : {}),
        },
      ],
      ...(sharedConfiguration ? { sharedConfiguration } : {}),
    };

    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api) => {
          const updatedApi: UpdateApiV4 = { ...(api as ApiV4) };
          this.updateDlq(updatedApi, newEndpointGroup);
          updatedApi.endpointGroups.push(newEndpointGroup);
          return this.apiService.update(api.id, updatedApi);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success(`Endpoint group ${newEndpointGroup.name} created!`);
          if (this.dlqEntrypointType) {
            this.goBackToDlqEntrypoint();
          } else {
            this.goBackToEndpointGroups();
          }
        },
        error: ({ err }) => this.snackBarService.error(err.message ?? 'An error occurred.'),
      });
  }

  private verifyEntrypointForDlq() {
    this.dlqEntrypointType = this.activatedRoute.snapshot?.queryParams?.dlq;
    const regularEndpointGroupButtonText = 'Create endpoint group';
    this.createButtonText = this.dlqEntrypointType ? 'Create DLQ endpoint group' : regularEndpointGroupButtonText;

    if (this.dlqEntrypointType) {
      this.connectorPluginsV2Service
        .listAsyncEntrypointPlugins()
        .pipe(
          tap((asyncEntrypoints) => {
            const entrypointForDlq = asyncEntrypoints.find(
              (entrypoint) =>
                entrypoint.id.toLowerCase() === this.dlqEntrypointType.toLowerCase() && entrypoint.availableFeatures?.includes('DLQ'),
            );
            if (!entrypointForDlq) {
              this.snackBarService.error(
                `Cannot create DLQ endpoint group for '${this.dlqEntrypointType}' entrypoint, creating regular endpoint group instead.`,
              );
              this.dlqEntrypointType = null;
              this.createButtonText = regularEndpointGroupButtonText;
            }
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    }
  }

  private updateDlq(updatedApi: UpdateApiV4, dlqEndpointGroup: EndpointGroupV4): void {
    if (this.dlqEntrypointType) {
      const dlqEntrypoint = updatedApi.listeners
        .flatMap((listener) => listener.entrypoints)
        .find((entrypoint) => entrypoint.type === this.dlqEntrypointType);
      if (dlqEntrypoint) {
        dlqEntrypoint.dlq = { endpoint: dlqEndpointGroup.name };
      }
    }
  }

  goBackToDlqEntrypoint() {
    this.router.navigate(['../../entrypoints/', this.dlqEntrypointType], { relativeTo: this.activatedRoute });
  }

  goBackToEndpointGroups() {
    this.router.navigate(['../'], { relativeTo: this.activatedRoute });
  }

  private initCreateForm(): void {
    this.endpointGroupTypeForm = new UntypedFormGroup({
      endpointGroupType: new UntypedFormControl('', Validators.required),
    });

    this.generalForm = new UntypedFormGroup({
      name: new UntypedFormControl('', [Validators.required, Validators.pattern(/^[^:]*$/)]),
      loadBalancerType: new UntypedFormControl('', [Validators.required]),
    });

    this.configuration = new UntypedFormControl({}, Validators.required);
    this.sharedConfiguration = new UntypedFormControl({}, Validators.required);

    this.createForm = new UntypedFormGroup({
      type: this.endpointGroupTypeForm,
      general: this.generalForm,
      configuration: this.configuration,
      sharedConfiguration: this.sharedConfiguration,
    });
  }
}
