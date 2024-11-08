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
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject, Subscription } from 'rxjs';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { isUniq, serviceDiscoveryValidator } from './api-proxy-group-edit.validator';
import { ProxyGroupServiceDiscoveryConfiguration } from './service-discovery/api-proxy-group-service-discovery.model';
import { toProxyGroup } from './api-proxy-group-edit.adapter';
import { ApiProxyGroupConfigurationComponent } from './configuration/api-proxy-group-configuration.component';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ResourceListItem } from '../../../../../entities/resource/resourceListItem';
import { ServiceDiscoveryService } from '../../../../../services-ngx/service-discovery.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../../../util/apiFilter.operator';
import { ApiV1, ApiV2 } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-proxy-group-edit',
  templateUrl: './api-proxy-group-edit.component.html',
  styleUrls: ['./api-proxy-group-edit.component.scss'],
})
export class ApiProxyGroupEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public mode: 'new' | 'edit';
  public apiId: string;
  public api: ApiV1 | ApiV2;
  public isReadOnly: boolean;
  public generalForm: UntypedFormGroup;
  public groupForm: UntypedFormGroup;
  public serviceDiscoveryForm: UntypedFormGroup;
  public initialGroupFormValue: unknown;
  public serviceDiscoveryItems: ResourceListItem[];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly serviceDiscoveryService: ServiceDiscoveryService,
    private readonly permissionService: GioPermissionService,
  ) {}

  public ngOnInit(): void {
    this.apiId = this.activatedRoute.snapshot.params.apiId;
    this.mode = !this.activatedRoute.snapshot.params.groupName ? 'new' : 'edit';

    this.apiService
      .get(this.apiId)
      .pipe(
        onlyApiV1V2Filter(this.snackBarService),
        switchMap((api) => {
          this.api = api;
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definitionContext?.origin === 'KUBERNETES';
          return this.serviceDiscoveryService.list();
        }),
        map((serviceDiscoveryItems: ResourceListItem[]) => {
          this.serviceDiscoveryItems = serviceDiscoveryItems;
          this.initForms();
        }),
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
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => {
          const groupIndex =
            this.mode === 'edit' ? api.proxy.groups.findIndex((group) => group.name === this.activatedRoute.snapshot.params.groupName) : -1;

          const updatedGroup = toProxyGroup(
            api.proxy.groups[groupIndex],
            this.generalForm.getRawValue(),
            ApiProxyGroupConfigurationComponent.getGroupConfigurationFormValue(
              this.groupForm.get('groupConfiguration') as UntypedFormGroup,
            ),
            this.getServiceDiscoveryConfiguration(),
          );

          groupIndex !== -1 ? api.proxy.groups.splice(groupIndex, 1, updatedGroup) : api.proxy.groups.push(updatedGroup);
          return this.apiService.update(api.id, api);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
          this.initialGroupFormValue = this.groupForm.getRawValue();
          this.groupForm.markAsPristine();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        switchMap(() =>
          // Redirect to same page with last group name
          this.router.navigate(['../', this.generalForm.get('name').value], { relativeTo: this.activatedRoute }),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public getServiceDiscoveryConfiguration(): ProxyGroupServiceDiscoveryConfiguration {
    const enabled: boolean = this.serviceDiscoveryForm.get('enabled')?.value;
    return {
      discovery: {
        enabled,
        ...(enabled
          ? {
              provider: this.serviceDiscoveryForm.get('provider').value,
              configuration: this.serviceDiscoveryForm.get('configuration').value,
            }
          : {}),
      },
    };
  }

  public reset(): void {
    // here we the force reset for the two components containing a gv-schema-form
    this.serviceDiscoveryItems = null;
    this.ngOnInit();
  }

  private initForms(): void {
    const group = { ...this.api.proxy.groups.find((group) => group.name === this.activatedRoute.snapshot.params.groupName) };

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
      loadBalancerType: [{ value: group?.loadBalancer?.type ?? null, disabled: this.isReadOnly }, [Validators.required]],
    });

    this.serviceDiscoveryForm = this.formBuilder.group(
      {
        enabled: [{ value: group?.services?.discovery?.enabled ?? false, disabled: this.isReadOnly }],
        provider: [
          {
            value: group?.services?.discovery?.provider ?? null,
            disabled: this.isReadOnly,
          },
        ],
        configuration: [{ value: group?.services?.discovery?.configuration ?? {}, disabled: this.isReadOnly }],
      },
      { validators: [serviceDiscoveryValidator] },
    );

    this.groupForm = this.formBuilder.group({
      general: this.generalForm,
      groupConfiguration: ApiProxyGroupConfigurationComponent.getGroupConfigurationFormGroup(group, this.isReadOnly),
      serviceDiscovery: this.serviceDiscoveryForm,
    });

    this.initialGroupFormValue = this.groupForm.getRawValue();
  }
}
