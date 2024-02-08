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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

import { Api, ApiV4, EndpointGroupV4, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { isEndpointNameUniqueAndDoesNotMatchDefaultValue } from '../api-endpoint-v4-unique-name';
import { ApiHealthCheckV4FormComponent } from '../../component/health-check-v4-form/api-health-check-v4-form.component';
import { ApiServicePluginsV2Service } from '../../../../services-ngx/apiservice-plugins-v2.service';

export type EndpointGroupHealthCheckFormType = FormGroup<{
  enabled: FormControl<boolean>;
  configuration: FormControl<unknown>;
}>;

@Component({
  selector: 'api-endpoint-group',
  templateUrl: './api-endpoint-group.component.html',
  styleUrls: ['./api-endpoint-group.component.scss'],
})
export class ApiEndpointGroupComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private SUCCESSFUL_ENDPOINT_CONFIGURATION_SAVE_MESSAGE = 'Configuration successfully saved!';

  public api: ApiV4;
  public initialApi: ApiV4;
  public isReadOnly: boolean;
  public generalForm: UntypedFormGroup;
  public groupForm: UntypedFormGroup;
  public configurationForm: UntypedFormGroup;
  public healthCheckForm: EndpointGroupHealthCheckFormType;
  public healthCheckSchema: unknown;
  public isHttpProxyApi: boolean;

  public initialGroupFormValue: any;
  public endpointGroup: EndpointGroupV4;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly apiServicePluginsV2Service: ApiServicePluginsV2Service,
  ) {}

  public ngOnInit(): void {
    const apiId = this.activatedRoute.snapshot.params.apiId;

    this.apiService
      .get(apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: (api: ApiV4) => this.initializeComponent(api),
      });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public onSubmit(): void {
    this.updateApi()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: (api) => {
          this.initializeComponent(api as ApiV4);
          this.snackBarService.success(this.SUCCESSFUL_ENDPOINT_CONFIGURATION_SAVE_MESSAGE);
        },
        error: (error) => {
          this.snackBarService.error(error.message);
        },
      });
  }

  /**
   * Initialize the component
   *
   * @param api the API object
   * @private
   */
  private initializeComponent(api: ApiV4): void {
    this.api = api;

    this.isHttpProxyApi = api.type === 'PROXY' && !(api.listeners.find((listener) => listener.type === 'TCP') != null);

    this.initialApi = this.api;

    this.endpointGroup = this.api.endpointGroups[this.activatedRoute.snapshot.params.groupIndex];

    if (!this.endpointGroup) {
      this.snackBarService.error(`Endpoint group at index [ ${this.activatedRoute.snapshot.params.groupIndex} ] does not exist.`);
      this.router.navigate(['../'], { relativeTo: this.activatedRoute });
      return;
    }

    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-r']) || api.definitionContext?.origin === 'KUBERNETES';

    this.generalForm = new UntypedFormGroup({
      name: new UntypedFormControl(
        {
          value: this.endpointGroup.name ?? null,
          disabled: this.isReadOnly,
        },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isEndpointNameUniqueAndDoesNotMatchDefaultValue(this.api, this.endpointGroup.name),
        ],
      ),
      loadBalancerType: new UntypedFormControl({ value: this.endpointGroup.loadBalancer?.type ?? null, disabled: false }, [
        Validators.required,
      ]),
    });

    this.configurationForm = new UntypedFormGroup({
      groupConfiguration: new UntypedFormControl({
        value: this.endpointGroup.sharedConfiguration ?? {},
        disabled: this.isReadOnly,
      }),
    });

    this.healthCheckForm = new FormGroup({
      enabled: new FormControl({
        value: this.endpointGroup.services?.healthCheck?.enabled ?? false,
        disabled: this.isReadOnly,
      }),
      configuration: new FormControl(
        {
          value: this.endpointGroup.services?.healthCheck?.configuration ?? {},
          disabled: this.isReadOnly,
        } ?? {},
      ),
    });

    if (this.isHttpProxyApi) {
      this.apiServicePluginsV2Service
        .getApiServicePluginSchema(ApiHealthCheckV4FormComponent.HTTP_HEALTH_CHECK)
        .pipe(
          tap((schema) => {
            this.healthCheckSchema = schema;

            this.healthCheckForm.controls.enabled.valueChanges
              .pipe(startWith(this.healthCheckForm.controls.enabled.value), takeUntil(this.unsubscribe$))
              .subscribe((enabled) => {
                if (enabled) {
                  this.healthCheckForm.controls.configuration.enable({ emitEvent: false });
                } else {
                  this.healthCheckForm.controls.configuration.disable({ emitEvent: false });
                }
              });
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    }

    this.groupForm = new UntypedFormGroup({
      general: this.generalForm,
      configuration: this.configurationForm,
      healthCheck: this.healthCheckForm,
    });

    this.initialGroupFormValue = this.groupForm.getRawValue();
  }

  /**
   * Update the current API object with the values from the forms on all tabs
   *
   * @param api the API object
   * @private
   */
  private updateApiObjectWithFormData(api: ApiV4): UpdateApiV4 {
    const updatedEndpointGroups = [...api.endpointGroups];
    updatedEndpointGroups[this.activatedRoute.snapshot.params.groupIndex] = {
      ...this.endpointGroup,
      name: this.generalForm.getRawValue().name.trim(),
      loadBalancer: {
        type: this.generalForm.getRawValue().loadBalancerType,
      },
      sharedConfiguration: this.configurationForm.getRawValue().groupConfiguration,
    };
    if (this.isHttpProxyApi && this.healthCheckForm.controls.enabled.value) {
      updatedEndpointGroups[this.activatedRoute.snapshot.params.groupIndex].services = {
        ...this.endpointGroup.services,
        healthCheck: {
          enabled: this.healthCheckForm.getRawValue().enabled,
          type: ApiHealthCheckV4FormComponent.HTTP_HEALTH_CHECK,
          configuration: this.healthCheckForm.getRawValue().configuration,
          overrideConfiguration: false,
        },
      };
    }

    return {
      ...api,
      endpointGroups: updatedEndpointGroups,
    };
  }

  private updateApi(): Observable<Api> {
    return this.apiService
      .get(this.api.id)
      .pipe(switchMap((api: ApiV4) => this.apiService.update(this.api.id, this.updateApiObjectWithFormData(api))));
  }
}
