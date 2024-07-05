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
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApiResourcesService } from './api-resources.service';

import { ResourceListItem } from '../../../entities/resource/resourceListItem';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiV2, ApiV4 } from '../../../entities/management-api-v2';
import { stringFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { Constants } from '../../../entities/Constants';

@Component({
  selector: 'api-resources-old',
  templateUrl: './api-resources.component.html',
  styleUrls: ['./api-resources.component.scss'],
})
export class ApiResourcesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public api: ApiV2 | ApiV4;
  public initialApiDefinition: ApiV2 | ApiV4;

  public isReadonly = false;
  public isDirty = false;

  public resourceTypes: ResourceListItem[];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly apiResourcesService: ApiResourcesService,
    private readonly gioLicenseService: GioLicenseService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.activatedRoute.snapshot.params.apiId),
      this.apiResourcesService.listResources({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        tap(([api, resourceTypes]) => {
          if (api.definitionVersion !== 'V1' && api.definitionVersion !== 'FEDERATED') {
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
    const feature = stringFeature(this.constants.customization?.ctaConfiguration, featureName);
    this.gioLicenseService.openDialog({ feature, context: UTMTags.API_CONFLUENT });
  }

  onReset() {
    this.api = this.initialApiDefinition;
    this.isDirty = false;
  }

  onSubmit() {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api) => {
          const updateApi = { ...api, ...this.api };
          return this.apiService.update(this.activatedRoute.snapshot.params.apiId, updateApi);
        }),
        tap((api: ApiV2 | ApiV4) => {
          this.api = api;
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
