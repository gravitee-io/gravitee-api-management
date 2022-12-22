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
import { ProxyGroupServiceDiscoveryConfiguration } from './service-discovery/api-proxy-group-service-discovery.model';

import { UIRouterState, UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../../services-ngx/api.service';
import { Api } from '../../../../../../entities/api';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { toProxyGroup } from '../api-proxy-groups.adapter';
import { ProxyConfiguration } from '../../../../../../entities/proxy';
import { ResourceListItem } from '../../../../../../entities/resource/resourceListItem';
import { ServiceDiscoveryService } from '../../../../../../services-ngx/service-discovery.service';
import { GioPermissionService } from '../../../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-proxy-group-edit',
  template: require('./api-proxy-group-edit.component.html'),
  styles: [require('./api-proxy-group-edit.component.scss')],
})
export class ApiProxyGroupEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private mode: 'new' | 'edit';

  public apiId: string;
  public api: Api;
  public isReadOnly: boolean;
  public generalForm: FormGroup;
  public groupForm: FormGroup;
  public serviceDiscoveryForm: FormGroup;
  public initialGroupFormValue: unknown;
  public serviceDiscoveryItems: ResourceListItem[];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
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
            this.getProxyConfiguration(),
            this.getServiceDiscoveryConfiguration(),
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

  public getProxyConfiguration(): ProxyConfiguration {
    return this.groupForm.get('groupConfiguration').value;
  }

  public getServiceDiscoveryConfiguration(): ProxyGroupServiceDiscoveryConfiguration {
    const enabled = this.serviceDiscoveryForm.get('enabled')?.value;

    if (!enabled) {
      return {
        discovery: {
          enabled,
        },
      };
    }

    return {
      discovery: {
        enabled,
        provider: this.serviceDiscoveryForm.get('type').value,
        configuration: this.serviceDiscoveryForm.get('configuration').value,
      },
    };
  }

  public reset(): void {
    // here we the force reset for the two components containing a gv-schema-form
    this.serviceDiscoveryItems = null;
    this.ngOnInit();
  }

  private initForms(): void {
    const group = { ...this.api.proxy.groups.find((group) => group.name === this.ajsStateParams.groupName) };

    this.generalForm = this.formBuilder.group({
      name: [
        { value: group?.name ?? null, disabled: this.isReadOnly },
        [
          Validators.required,
          Validators.pattern(/^[^:]*$/),
          isUniq(
            this.api.proxy.groups.reduce((acc, group) => [...acc, group.name], []),
            group?.name,
          ),
        ],
      ],
      loadBalancerType: [{ value: group?.load_balancing?.type ?? null, disabled: this.isReadOnly }, [Validators.required]],
    });

    this.serviceDiscoveryForm = this.formBuilder.group(
      {
        enabled: [{ value: group?.services?.discovery.enabled ?? false, disabled: this.isReadOnly }],
        type: [
          {
            value: group?.services?.discovery.provider ?? null,
            disabled: !group?.services?.discovery.enabled || this.isReadOnly,
          },
        ],
        configuration: [{ value: group?.services?.discovery?.configuration ?? {}, disabled: this.isReadOnly }],
      },
      { validators: [serviceDiscoveryValidator] },
    );

    this.groupForm = this.formBuilder.group({
      general: this.generalForm,
      groupConfiguration: [{ value: group ?? {}, disabled: this.isReadOnly }],
      serviceDiscovery: this.serviceDiscoveryForm,
    });

    this.initialGroupFormValue = this.groupForm.getRawValue();
  }
}
