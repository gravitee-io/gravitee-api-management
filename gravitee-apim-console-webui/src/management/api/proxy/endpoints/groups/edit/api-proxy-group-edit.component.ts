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
import { EMPTY, Subject, Subscription } from 'rxjs';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StateService } from '@uirouter/core';

import { isUniq, serviceDiscoveryValidator } from './api-proxy-group-edit.validator';

import { UIRouterState, UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../../services-ngx/api.service';
import { Api } from '../../../../../../entities/api';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { ConnectorService } from '../../../../../../services-ngx/connector.service';
import { toProxyGroup } from '../api-proxy-groups.adapter';
import { ProxyConfiguration, ProxyGroup } from '../../../../../../entities/proxy';
import { ResourceListItem } from '../../../../../../entities/resource/resourceListItem';
import { ServiceDiscoveryService } from '../../../../../../services-ngx/service-discovery.service';
import { GioPermissionService } from '../../../../../../shared/components/gio-permission/gio-permission.service';
import { ConfigurationEvent } from '../api-proxy-groups.model';

@Component({
  selector: 'api-proxy-group-edit',
  template: require('./api-proxy-group-edit.component.html'),
  styles: [require('./api-proxy-group-edit.component.scss')],
})
export class ApiProxyGroupEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private updatedConfiguration: ProxyConfiguration;
  private mode: 'new' | 'edit';

  public apiId: string;
  public api: Api;
  public group: ProxyGroup;
  public isReadOnly: boolean;
  public generalForm: FormGroup;
  public groupForm: FormGroup;
  public serviceDiscoveryForm: FormGroup;
  public initialGroupFormValue: unknown;
  public schemaForm: unknown;
  public serviceDiscoveryItems: ResourceListItem[];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
    private readonly connectorService: ConnectorService,
    private readonly serviceDiscoveryService: ServiceDiscoveryService,
    private readonly permissionService: GioPermissionService,
  ) {}

  public ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
    this.mode = !this.ajsStateParams.groupName ? 'new' : 'edit';

    this.apiService
      .get(this.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          this.api = api;
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definition_context?.origin === 'kubernetes';
          return this.serviceDiscoveryService.list();
        }),
        map((serviceDiscoveryItems: ResourceListItem[]) => {
          this.serviceDiscoveryItems = serviceDiscoveryItems;
          this.initForms();
        }),
      )
      .subscribe();

    this.connectorService
      .list(true)
      .pipe(
        takeUntil(this.unsubscribe$),
        map((connectors) => {
          this.schemaForm = JSON.parse(connectors.find((connector) => connector.supportedTypes.includes('http'))?.schema);
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
          const groupIndex =
            this.mode === 'edit' ? api.proxy.groups.findIndex((group) => group.name === this.ajsStateParams.groupName) : -1;

          const updatedGroup = toProxyGroup(
            api.proxy.groups[groupIndex],
            this.generalForm.getRawValue(),
            this.updatedConfiguration,
            this.serviceDiscoveryForm.getRawValue(),
          );

          groupIndex !== -1 ? api.proxy.groups.splice(groupIndex, 1, updatedGroup) : api.proxy.groups.push(updatedGroup);
          return this.apiService.update(api);
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() =>
          // Redirect to same page with last group name
          this.ajsState.go(
            'management.apis.detail.proxy.group',
            { apiId: this.apiId, groupName: this.generalForm.get('name').value },
            { reload: true },
          ),
        ),
      )
      .subscribe();
  }

  public onConfigurationChange(event: ConfigurationEvent): void {
    this.groupForm.markAsDirty();
    this.groupForm.markAsTouched();
    if (this.groupForm.getError('invalidConfiguration') && event.isSchemaValid) {
      delete this.groupForm.errors['invalidConfiguration'];
      this.groupForm.updateValueAndValidity();
    } else if (!event.isSchemaValid) {
      this.groupForm.setErrors({ invalidConfiguration: true });
    }
    this.updatedConfiguration = event.configuration;
  }

  public reset(): void {
    // here we the force reset for the two components containing a gv-schema-form
    this.group = null;
    this.schemaForm = null;
    this.serviceDiscoveryItems = null;
    this.ngOnInit();
  }

  private initForms(): void {
    this.group = { ...this.api.proxy.groups.find((group) => group.name === this.ajsStateParams.groupName) };

    this.generalForm = this.formBuilder.group({
      name: [
        { value: this.group?.name ?? null, disabled: this.isReadOnly },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isUniq(
            this.api.proxy.groups.reduce((acc, group) => [...acc, group.name], []),
            this.group?.name,
          ),
        ],
      ],
      loadBalancerType: [{ value: this.group?.load_balancing?.type ?? null, disabled: this.isReadOnly }, [Validators.required]],
    });

    this.serviceDiscoveryForm = this.formBuilder.group(
      {
        enabled: [{ value: this.group?.services?.discovery.enabled ?? false, disabled: this.isReadOnly }],
        provider: [
          {
            value: this.group?.services?.discovery.provider ?? null,
            disabled: this.isReadOnly,
          },
        ],
        configuration: [
          {
            value: this.group?.services?.discovery.configuration ?? undefined,
            disabled: this.isReadOnly,
          },
        ],
      },
      { validators: [serviceDiscoveryValidator] },
    );

    this.groupForm = this.formBuilder.group({
      general: this.generalForm,
      serviceDiscovery: this.serviceDiscoveryForm,
    });

    this.initialGroupFormValue = this.groupForm.getRawValue();
  }
}
