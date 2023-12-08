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
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, Subject, Subscription } from 'rxjs';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StateService } from '@uirouter/core';

import { UIRouterState, UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';
import { ConnectorService } from '../../../../../../../services-ngx/connector.service';
import { ApiService } from '../../../../../../../services-ngx/api.service';
import { Api } from '../../../../../../../entities/api';
import { ProxyConfiguration, ProxyGroupEndpoint } from '../../../../../../../entities/proxy';
import { TenantService } from '../../../../../../../services-ngx/tenant.service';
import { Tenant } from '../../../../../../../entities/tenant/tenant';
import { SnackBarService } from '../../../../../../../services-ngx/snack-bar.service';
import { toProxyGroupEndpoint } from '../api-proxy-group-endpoint.adapter';
import { isUniq } from '../../edit/api-proxy-group-edit.validator';
import { ConnectorListItem } from '../../../../../../../entities/connector/connector-list-item';
import { GioPermissionService } from '../../../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiProxyHealthCheckFormComponent } from '../../../../components/health-check-form/api-proxy-health-check-form.component';
import { HealthCheck } from '../../../../../../../entities/health-check';
import '@gravitee/ui-components/wc/gv-schema-form-group';
import { ApiV2Service } from '../../../../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-proxy-group-endpoint-edit',
  template: require('./api-proxy-group-endpoint-edit.component.html'),
  styles: [require('./api-proxy-group-endpoint-edit.component.scss')],
})
export class ApiProxyGroupEndpointEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private api: Api;
  private connectors: ConnectorListItem[];
  private mode: 'edit' | 'new';

  public apiId: string;
  public isReadOnly: boolean;
  public supportedTypes: string[];
  public endpointForm: FormGroup;
  public generalForm: FormGroup;
  public configurationForm: FormGroup;
  public healthCheckForm: FormGroup;
  public inheritHealthCheck: HealthCheck;
  public endpoint: ProxyGroupEndpoint;
  public initialEndpointFormValue: unknown;
  public tenants: Tenant[];
  public configurationSchema: unknown;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly apiService: ApiService,
    private readonly apiV2Service: ApiV2Service,
    private readonly connectorService: ConnectorService,
    private readonly tenantService: TenantService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  public ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
    this.mode = !this.ajsStateParams.groupName || !this.ajsStateParams.endpointName ? 'new' : 'edit';

    combineLatest([this.apiService.get(this.apiId), this.connectorService.list(true), this.tenantService.list()])
      .pipe(
        map(([api, connectors, tenants]) => {
          this.api = api;
          this.connectors = connectors;
          this.tenants = tenants;
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definition_context?.origin === 'kubernetes';
          this.initForms();
          this.supportedTypes = this.connectors.map((connector) => connector.supportedTypes).reduce((acc, val) => acc.concat(val), []);
          this.configurationSchema = JSON.parse(
            this.connectors.find((connector) => connector.supportedTypes.includes(this.endpoint?.type?.toLowerCase() ?? 'http'))?.schema,
          );
        }),
        // TODO : remove when this page only use apiV2Service
        switchMap(() => this.apiV2Service.get(this.apiId)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  public onSubmit(): Subscription {
    return this.apiService
      .get(this.apiId)
      .pipe(
        switchMap((api) => {
          const groupIndex = api.proxy.groups.findIndex((group) => group.name === this.ajsStateParams.groupName);

          let endpointIndex = -1;
          if (this.mode === 'edit') {
            endpointIndex = api.proxy.groups[groupIndex].endpoints.findIndex(
              (endpoint) => endpoint.name === this.ajsStateParams.endpointName,
            );
          } else {
            if (!api.proxy.groups[groupIndex].endpoints) {
              api.proxy.groups[groupIndex].endpoints = [];
            }
            endpointIndex = api.proxy.groups[groupIndex].endpoints.length;
          }
          // TODO: Remove the "as" when we migrate to mapiV2
          const healthCheck = ApiProxyHealthCheckFormComponent.HealthCheckFromFormGroup(this.healthCheckForm, true) as HealthCheck;

          const updatedEndpoint = toProxyGroupEndpoint(
            api.proxy.groups[groupIndex]?.endpoints[endpointIndex],
            this.generalForm.getRawValue(),
            this.configurationForm.getRawValue(),
            healthCheck,
          );

          endpointIndex !== -1
            ? api.proxy.groups[groupIndex].endpoints.splice(endpointIndex, 1, updatedEndpoint)
            : api.proxy.groups[groupIndex].endpoints.push(updatedEndpoint);

          return this.apiService.update(api);
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'Error while saving configuration.');
          return EMPTY;
        }),
        // TODO : remove when this page only use apiV2Service
        switchMap(() => this.apiV2Service.get(this.apiId)),
        tap(() =>
          // Redirect to same page with last endpoint name
          this.ajsState.go(
            'management.apis.ng.endpoint-v2',
            {
              apiId: this.apiId,
              groupName: this.ajsStateParams.groupName,
              endpointName: this.generalForm.getRawValue().name,
            },
            { reload: true },
          ),
        ),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForms(): void {
    const group = this.api.proxy.groups.find((group) => group.name === this.ajsStateParams.groupName);

    if (group && group.endpoints && group.endpoints.length > 0) {
      this.endpoint = {
        ...group.endpoints.find((endpoint) => endpoint.name === this.ajsStateParams.endpointName),
      };
    } else {
      this.endpoint = {};
    }

    this.generalForm = this.formBuilder.group({
      name: [
        {
          value: this.endpoint?.name ?? null,
          disabled: this.isReadOnly,
        },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isUniq(group.endpoints?.reduce((acc, endpoint) => [...acc, endpoint.name], []) ?? [], this.endpoint?.name),
        ],
      ],
      type: [{ value: this.endpoint?.type ?? 'http', disabled: this.isReadOnly }, [Validators.required]],
      target: [{ value: this.endpoint?.target ?? null, disabled: this.isReadOnly }, [Validators.required]],
      weight: [{ value: this.endpoint?.weight ?? null, disabled: this.isReadOnly }, [Validators.required]],
      tenants: [{ value: this.endpoint?.tenants ?? null, disabled: this.isReadOnly }],
      backup: [{ value: this.endpoint?.backup ?? false, disabled: this.isReadOnly }],
    });

    const proxyConfigurationValue: ProxyConfiguration = {
      headers: this.endpoint?.headers ?? [],
      http: this.endpoint?.http ?? {},
      proxy: this.endpoint?.proxy ?? { enabled: false },
      ssl: this.endpoint?.ssl ?? {},
    };

    this.configurationForm = this.formBuilder.group({
      inherit: [{ value: this.endpoint?.inherit ?? false, disabled: this.isReadOnly }],
      proxyConfiguration: [
        {
          value: proxyConfigurationValue,
          disabled: this.isReadOnly,
        },
      ],
    });

    this.healthCheckForm = ApiProxyHealthCheckFormComponent.NewHealthCheckFormGroup(
      this.endpoint?.healthcheck ?? { inherit: true },
      this.isReadOnly,
    );
    this.inheritHealthCheck = this.api?.services?.['health-check'] ?? { enabled: false };

    this.endpointForm = this.formBuilder.group({
      general: this.generalForm,
      configuration: this.configurationForm,
      healthCheckForm: this.healthCheckForm,
    });

    this.initialEndpointFormValue = this.endpointForm.getRawValue();

    this.generalForm
      .get('type')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((type) => {
        this.configurationSchema = JSON.parse(this.connectors.find((connector) => connector.supportedTypes.includes(type))?.schema);
      });
  }
}
