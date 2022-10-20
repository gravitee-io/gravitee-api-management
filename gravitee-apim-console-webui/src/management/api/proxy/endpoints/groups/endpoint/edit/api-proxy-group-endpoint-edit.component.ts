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
import { Component, Inject, OnInit } from '@angular/core';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, Subject, Subscription } from 'rxjs';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StateService } from '@uirouter/core';

import { UIRouterState, UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';
import { ConnectorService } from '../../../../../../../services-ngx/connector.service';
import { ApiService } from '../../../../../../../services-ngx/api.service';
import { Api } from '../../../../../../../entities/api';
import { ProxyGroupEndpoint } from '../../../../../../../entities/proxy';
import { TenantService } from '../../../../../../../services-ngx/tenant.service';
import { Tenant } from '../../../../../../../entities/tenant/tenant';
import { SnackBarService } from '../../../../../../../services-ngx/snack-bar.service';
import { toProxyGroupEndpoint } from '../api-proxy-group-endpoint.adapter';
import { isUniq } from '../../edit/api-proxy-group-edit.validator';

@Component({
  selector: 'api-proxy-group-endpoint-edit',
  template: require('./api-proxy-group-endpoint-edit.component.html'),
  styles: [require('./api-proxy-group-endpoint-edit.component.scss')],
})
export class ApiProxyGroupEndpointEditComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private api: Api;

  public apiId: string;
  public isReadOnly: boolean;
  public supportedTypes: string[];
  public endpointForm: FormGroup;
  public generalForm: FormGroup;
  public endpoint: ProxyGroupEndpoint;
  public initialEndpointFormValue: unknown;
  public tenants: Tenant[];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly apiService: ApiService,
    private readonly connectorService: ConnectorService,
    private readonly tenantService: TenantService,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;

    combineLatest([this.apiService.get(this.apiId), this.connectorService.list(true), this.tenantService.list()])
      .pipe(
        takeUntil(this.unsubscribe$),
        map(([api, connectors, tenants]) => {
          this.api = api;
          this.supportedTypes = connectors.map((connector) => connector.supportedTypes).reduce((acc, val) => acc.concat(val), []);
          this.tenants = tenants;
          this.initForms();
        }),
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
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          const groupIndex = api.proxy.groups.findIndex((group) => group.name === this.ajsStateParams.groupName);
          const endpointIndex = api.proxy.groups[groupIndex].endpoints.findIndex(
            (endpoint) => endpoint.name === this.ajsStateParams.endpointName,
          );
          const updatedEndpoint = toProxyGroupEndpoint(
            api.proxy.groups[groupIndex]?.endpoints[endpointIndex],
            this.generalForm.getRawValue(),
          );

          endpointIndex !== -1
            ? api.proxy.groups[groupIndex].endpoints.splice(groupIndex, 1, updatedEndpoint)
            : api.proxy.groups[groupIndex].endpoints.push(updatedEndpoint);

          return this.apiService.update(api);
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ajsState.go('management.apis.detail.proxy.ng-endpoints', { apiId: this.apiId })),
      )
      .subscribe();
  }

  private initForms(): void {
    const group = this.api.proxy.groups.find((group) => group.name === this.ajsStateParams.groupName);

    if (group && group.endpoints && group.endpoints.length > 0) {
      this.endpoint = {
        ...group.endpoints.find((endpoint) => endpoint.name === this.ajsStateParams.endpointName),
      };
    }

    this.generalForm = this.formBuilder.group({
      name: [
        {
          value: this.endpoint?.name ?? null,
          disabled: false,
        },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isUniq(
            group.endpoints.reduce((acc, endpoint) => [...acc, endpoint.name], []),
            this.endpoint?.name,
          ),
        ],
      ],
      type: [{ value: this.endpoint?.type ?? null, disabled: false }, [Validators.required]],
      target: [{ value: this.endpoint?.target ?? null, disabled: false }, [Validators.required]],
      weight: [{ value: this.endpoint?.weight ?? null, disabled: false }, [Validators.required]],
      tenants: [{ value: this.endpoint?.tenants ?? null, disabled: false }],
      backup: [{ value: this.endpoint?.backup ?? false, disabled: false }],
    });

    this.endpointForm = this.formBuilder.group({
      general: this.generalForm,
    });

    this.initialEndpointFormValue = this.endpointForm.getRawValue();
  }
}
