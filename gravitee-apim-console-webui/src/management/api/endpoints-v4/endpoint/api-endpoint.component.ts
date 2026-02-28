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
import { catchError, filter, map, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, Observable, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioFormJsonSchemaComponent, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, ApiV4, ConnectorPlugin, EndpointGroupV4, EndpointV4, Entrypoint } from '../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { IconService } from '../../../../services-ngx/icon.service';
import { isEndpointNameUnique, isEndpointNameUniqueAndDoesNotMatchDefaultValue } from '../api-endpoint-v4-unique-name';
import { getMatchingDlqEntrypoints, updateDlqEntrypoint } from '../api-endpoint-v4-matching-dlq';
import { ApiServicePluginsV2Service } from '../../../../services-ngx/apiservice-plugins-v2.service';
import { ApiHealthCheckV4FormComponent } from '../../component/health-check-v4-form/api-health-check-v4-form.component';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Tenant } from '../../../../entities/tenant/tenant';
import { TenantService } from '../../../../services-ngx/tenant.service';
import { ENDPOINT_ADAPTER } from '../../../../shared/components/zee/adapters/endpoint-adapter';

export type EndpointHealthCheckFormType = FormGroup<{
  enabled: FormControl<boolean>;
  inherit: FormControl<boolean>;
  configuration: FormControl<unknown>;
}>;

@Component({
  selector: 'api-endpoint',
  templateUrl: './api-endpoint.component.html',
  styleUrls: ['./api-endpoint.component.scss'],
  standalone: false,
})
export class ApiEndpointComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private groupIndex: number;
  private endpointIndex: number;
  public endpointGroup: EndpointGroupV4;
  public isReadOnly = false;
  public formGroup: UntypedFormGroup;
  public endpointSchema: { config: GioJsonSchema; sharedConfig: GioJsonSchema };
  public connectorPlugin: ConnectorPlugin;
  public isLoading = false;
  private api: ApiV4;
  private endpoint: EndpointV4;
  private mode: 'edit' | 'create';
  public healthCheckSchema: unknown;
  public isHttpProxyApi: boolean;
  public isNativeKafkaApi: boolean;
  public healthCheckForm: EndpointHealthCheckFormType;
  public tenants: Tenant[];
  public endpointAdapter = ENDPOINT_ADAPTER;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly iconService: IconService,
    private readonly matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
    private readonly apiServicePluginsV2Service: ApiServicePluginsV2Service,
    private readonly tenantService: TenantService,
  ) {}

  public ngOnInit(): void {
    this.isLoading = true;
    const apiId = this.activatedRoute.snapshot.params.apiId;
    this.groupIndex = +this.activatedRoute.snapshot.params.groupIndex;
    this.mode = this.activatedRoute.snapshot.params.endpointIndex !== undefined ? 'edit' : 'create';

    this.apiService
      .get(apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          this.api = api;

          this.isHttpProxyApi = api.type === 'PROXY' && !(api.listeners.find((listener) => listener.type === 'TCP') != null);
          this.isNativeKafkaApi = api.type === 'NATIVE' && api.listeners.some((listener) => listener.type === 'KAFKA');

          const isKubernetesOrigin = api.definitionContext?.origin === 'KUBERNETES';
          const canUpdate = this.permissionService.hasAnyMatching(['api-definition-u']);
          this.isReadOnly = isKubernetesOrigin || !canUpdate;
          this.endpointGroup = api.endpointGroups[this.groupIndex];

          if (!this.endpointGroup) {
            this.snackBarService.error(`Endpoint group at index [ ${this.activatedRoute.snapshot.params.groupIndex} ] does not exist.`);
            this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
            return EMPTY;
          }

          return combineLatest([
            this.connectorPluginsV2Service.getEndpointPluginSchema(this.endpointGroup.type),
            this.connectorPluginsV2Service.getEndpointPluginSharedConfigurationSchema(this.endpointGroup.type),
            this.connectorPluginsV2Service.getEndpointPlugin(this.endpointGroup.type),
            this.apiServicePluginsV2Service.getApiServicePluginSchema(ApiHealthCheckV4FormComponent.HTTP_HEALTH_CHECK),
            this.tenantService.list(),
          ]);
        }),
        tap(([config, sharedConfig, connectorPlugin, healthCheckSchema, tenants]) => {
          this.endpointSchema = {
            config: GioFormJsonSchemaComponent.isDisplayable(config) ? config : null,
            sharedConfig: GioFormJsonSchemaComponent.isDisplayable(sharedConfig) ? sharedConfig : null,
          };
          this.connectorPlugin = { ...connectorPlugin, icon: this.iconService.registerSvg(connectorPlugin.id, connectorPlugin.icon) };
          this.healthCheckSchema = healthCheckSchema;
          this.tenants = tenants;
          this.initForm();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => (this.isLoading = false));
  }

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public onSave() {
    const inheritConfiguration = this.formGroup.get('inheritConfiguration').value;

    const updatedEndpoint: EndpointV4 = {
      type: this.endpointGroup.type,
      name: this.formGroup.get('name').value.trim(),
      weight: this.formGroup.get('weight').value,
      tenants: this.formGroup.get('tenants').value,
      configuration: this.formGroup.get('configuration').value,
      sharedConfigurationOverride: inheritConfiguration ? {} : this.formGroup.get('sharedConfigurationOverride').value,
      inheritConfiguration,
    };

    if (this.isHttpProxyApi) {
      const isHealthCheckEnabled = this.healthCheckForm.controls.enabled.value;
      const inheritHealthCheck = this.healthCheckForm.controls.inherit.value;
      updatedEndpoint.services = {
        healthCheck: !inheritHealthCheck
          ? {
              enabled: isHealthCheckEnabled,
              type: ApiHealthCheckV4FormComponent.HTTP_HEALTH_CHECK,
              configuration: this.healthCheckForm.getRawValue().configuration,
              overrideConfiguration: true,
            }
          : undefined,
      };
    }

    let apiUpdate$: Observable<Api>;
    if (this.mode === 'edit') {
      const matchingDlqEntrypoint = getMatchingDlqEntrypoints(this.api, this.endpoint.name);
      if (updatedEndpoint.name !== this.endpoint.name && matchingDlqEntrypoint.length > 0) {
        apiUpdate$ = this.updateApiWithDlq(matchingDlqEntrypoint, updatedEndpoint);
      } else {
        apiUpdate$ = this.apiService
          .get(this.activatedRoute.snapshot.params.apiId)
          .pipe(switchMap((api: ApiV4) => this.updateApi(api, updatedEndpoint)));
      }
    } else {
      apiUpdate$ = this.apiService
        .get(this.activatedRoute.snapshot.params.apiId)
        .pipe(switchMap((api: ApiV4) => this.updateApi(api, updatedEndpoint)));
    }

    apiUpdate$
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(() => {
          this.snackBarService.success(`Endpoint successfully created!`);
          this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private updateApiWithDlq(matchingDlqEntrypoint: Entrypoint[], updatedEndpoint: EndpointV4): Observable<Api> {
    return this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Rename Endpoint',
          content: `Some entrypoints use this endpoint as Dead letter queue. They will be modified to reference the new name.`,
          confirmButton: 'Update',
        },
        role: 'alertdialog',
        id: 'updateEndpointNameDlqConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.api.id)),
        map((api: ApiV4) => {
          updateDlqEntrypoint(api, matchingDlqEntrypoint, updatedEndpoint.name);
          return api;
        }),
        switchMap((api: ApiV4) => this.updateApi(api, updatedEndpoint)),
      );
  }

  private updateApi(api: ApiV4, updatedEndpoint: EndpointV4): Observable<Api> {
    const endpointGroups = api.endpointGroups.map((group, i) => {
      if (i === this.groupIndex) {
        return {
          ...group,
          endpoints:
            this.endpointIndex !== undefined
              ? group.endpoints.map((endpoint, j) => (j === this.endpointIndex ? updatedEndpoint : endpoint))
              : [...group.endpoints, updatedEndpoint],
        };
      }
      return group;
    });
    return this.apiService.update(api.id, { ...api, endpointGroups });
  }

  public onInheritConfigurationChange() {
    if (this.formGroup.get('inheritConfiguration').value) {
      this.formGroup.get('sharedConfigurationOverride').disable();
    } else {
      this.formGroup.get('sharedConfigurationOverride').enable();
    }
  }

  private initForm() {
    let name = null;
    let isHealthCheckEnabled = false;
    let isHealthCheckInherited = true;
    let healthCheckConfiguration = this.endpointGroup.services?.healthCheck?.configuration;
    let configuration = null;
    let sharedConfigurationOverride = this.endpointGroup.sharedConfiguration;
    // Inherit configuration only if there is a sharedConfiguration (that is not the case of mock endppoint)
    let inheritConfiguration = !!sharedConfigurationOverride;
    let weight = null;
    let tenants = null;

    if (this.mode === 'edit') {
      this.endpointIndex = +this.activatedRoute.snapshot.params.endpointIndex;
      this.endpoint = this.endpointGroup.endpoints[this.endpointIndex];

      if (!this.endpoint) {
        this.snackBarService.error(`Endpoint at index [ ${this.activatedRoute.snapshot.params.groupIndex} ] does not exist.`);
        this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
        return;
      }

      name = this.endpoint.name;
      weight = this.endpoint.weight;
      tenants = this.endpoint.tenants;
      inheritConfiguration = this.endpoint.inheritConfiguration;
      configuration = this.endpoint.configuration;
      if (!inheritConfiguration) {
        sharedConfigurationOverride = this.endpoint.sharedConfigurationOverride;
      }
      isHealthCheckInherited = !this.endpoint.services?.healthCheck?.overrideConfiguration;
      isHealthCheckEnabled = this.endpointGroup.services?.healthCheck?.enabled;
      if (!isHealthCheckInherited) {
        isHealthCheckEnabled = this.endpoint.services?.healthCheck?.enabled;
        healthCheckConfiguration = this.endpoint.services?.healthCheck?.configuration;
      }
    }
    this.healthCheckForm = new FormGroup({
      enabled: new FormControl({
        value: isHealthCheckEnabled ?? false,
        disabled: this.isReadOnly,
      }),
      inherit: new FormControl({
        value: isHealthCheckInherited ?? false,
        disabled: this.isReadOnly,
      }),
      configuration: new FormControl({
        value: healthCheckConfiguration ?? {},
        disabled: this.isReadOnly,
      }),
    });
    if (this.isHttpProxyApi) {
      this.healthCheckForm.controls.enabled.valueChanges
        .pipe(startWith(this.healthCheckForm.controls.enabled.value), takeUntil(this.unsubscribe$))
        .subscribe((enabled) => {
          if (enabled) {
            this.healthCheckForm.controls.configuration.enable({ emitEvent: false });
          } else {
            this.healthCheckForm.controls.configuration.disable({ emitEvent: false });
          }
        });
      this.healthCheckForm.controls.inherit.valueChanges
        .pipe(startWith(this.healthCheckForm.controls.inherit.value), takeUntil(this.unsubscribe$))
        .subscribe((inherit) => {
          if (inherit) {
            this.resetHealthCheckToGroup();
            this.healthCheckForm.controls.configuration.disable({ emitEvent: false });
            this.healthCheckForm.controls.enabled.disable({ emitEvent: false });
          } else {
            this.healthCheckForm.controls.enabled.enable({ emitEvent: false });
            if (this.healthCheckForm.controls.enabled.value) {
              this.healthCheckForm.controls.configuration.enable({ emitEvent: false });
            }
          }
        });
    }

    this.formGroup = new UntypedFormGroup({
      name: new UntypedFormControl({ value: name, disabled: this.isReadOnly }, [
        Validators.required,
        this.mode === 'edit'
          ? isEndpointNameUniqueAndDoesNotMatchDefaultValue(this.api, this.endpoint.name)
          : isEndpointNameUnique(this.api),
      ]),
      weight: new UntypedFormControl({ value: weight, disabled: this.isReadOnly }),
      tenants: new UntypedFormControl({ value: tenants, disabled: this.isReadOnly }),
      inheritConfiguration: new UntypedFormControl({ value: inheritConfiguration, disabled: this.isReadOnly }),
      configuration: new UntypedFormControl({ value: configuration, disabled: this.isReadOnly }),
      sharedConfigurationOverride: new UntypedFormControl({
        value: sharedConfigurationOverride,
        disabled: inheritConfiguration || this.isReadOnly,
      }),
      healthCheck: this.healthCheckForm,
    });

    if (!this.isNativeKafkaApi) {
      this.formGroup.get('weight').addValidators([Validators.required, Validators.min(1)]);
    }
  }

  private resetHealthCheckToGroup() {
    this.healthCheckForm.controls.enabled.patchValue(this.endpointGroup.services?.healthCheck?.enabled ?? false);
    this.healthCheckForm.controls.configuration.patchValue(this.endpointGroup.services?.healthCheck?.configuration ?? {});
  }

  onEndpointGenerated(generatedData: unknown) {
    const data = generatedData as Record<string, unknown>;
    const apiId = this.activatedRoute.snapshot.params.apiId;

    const newEndpoint: EndpointV4 = {
      type: this.endpointGroup?.type ?? 'http-proxy',
      name: (data.name as string) ?? '',
      weight: (data.weight as number) ?? 1,
      inheritConfiguration: (data.inheritConfiguration as boolean) ?? true,
      configuration: (data.configuration as Record<string, unknown>) ?? {},
      sharedConfigurationOverride: (data.sharedConfigurationOverride as Record<string, unknown>) ?? {},
    };

    this.apiService
      .get(apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const endpointGroups = api.endpointGroups.map((group, i) =>
            i === this.groupIndex ? { ...group, endpoints: [...group.endpoints, newEndpoint] } : group,
          );
          return this.apiService.update(api.id, { ...api, endpointGroups });
        }),
        tap(() => this.snackBarService.success('Endpoint created by Zee!')),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Failed to create endpoint');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
      });
  }
}
