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
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-resources';
import { StateParams } from '@uirouter/angularjs';
import { action } from '@storybook/addon-actions';
import { MatDialog } from '@angular/material/dialog';

import { ApiResourcesService } from './api-resources.service';

import { ResourceListItem } from '../../../../entities/resource/resourceListItem';
import { AjsRootScope, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV2, ApiV4 } from '../../../../entities/management-api-v2';
import { GioLicenseService } from '../../../../shared/components/gio-license/gio-license.service';
import {
  GioEeUnlockDialogComponent,
  GioEeUnlockDialogData,
} from '../../../../components/gio-ee-unlock-dialog/gio-ee-unlock-dialog.component';
import { stringFeature } from '../../../../shared/components/gio-license/gio-license-features';
import { UTMMedium } from '../../../../shared/components/gio-license/gio-license-utm';

@Component({
  selector: 'api-resources',
  template: require('./api-resources.component.html'),
  styles: [require('./api-resources.component.scss')],
})
export class ApiResourcesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public api: ApiV2 | ApiV4;
  public initialApiDefinition: ApiV2 | ApiV4;

  public isReadonly = false;
  public isDirty = false;

  public resourceTypes: ResourceListItem[];

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiResourcesService: ApiResourcesService,
    private readonly permissionService: GioPermissionService,
    private readonly gioLicenseService: GioLicenseService,
    private readonly matDialog: MatDialog,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    @Inject(AjsRootScope) readonly ajsRootScope,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.ajsStateParams.apiId),
      this.apiResourcesService.listResources({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        tap(([api, resourceTypes]) => {
          if (api.definitionVersion !== 'V1') {
            this.api = api;
            this.initialApiDefinition = this.api;
            this.isReadonly = this.api.definitionContext.origin === 'KUBERNETES' ? true : null;
            this.resourceTypes = resourceTypes;
          } else {
            return EMPTY;
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChange($event: any) {
    this.api.resources = $event.detail.resources;
    this.isDirty = true;
  }

  onDisplayResourceCTA({ detail: event }) {
    const resourceId = event.detail.id;
    const featureName = this.resourceTypes.find((resourceType) => resourceType.id === resourceId).feature;
    const feature = stringFeature(featureName);
    const featureMoreInformation = this.gioLicenseService.getFeatureMoreInformation(feature);
    const trialURL = this.gioLicenseService.getTrialURL(UTMMedium.CONFLUENT_SCHEMA_REGISTRY);

    this.matDialog
      .open<GioEeUnlockDialogComponent, GioEeUnlockDialogData, boolean>(GioEeUnlockDialogComponent, {
        data: {
          featureMoreInformation,
          trialURL,
        },
        role: 'alertdialog',
        id: 'dialog',
      })
      .afterClosed()
      .pipe(
        tap((confirmed) => {
          action('confirmed?')(confirmed);
        }),
        takeUntil(this.unsubscribe$),
      )

      .subscribe();
  }

  onReset() {
    this.api = this.initialApiDefinition;
    this.isDirty = false;
  }

  onSubmit() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => {
          const updateApi = { ...api, ...this.api };
          return this.apiService.update(this.ajsStateParams.apiId, updateApi);
        }),
        tap((api: ApiV2 | ApiV4) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api });
          this.api = api;
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
