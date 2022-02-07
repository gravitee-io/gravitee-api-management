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
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-properties';

import { PolicyStudioPropertiesService } from './policy-studio-properties.service';
import { ChangePropertiesEvent } from './models/ChangePropertiesEvent';
import { SaveProviderEvent } from './models/SaveProviderEvent';

import { ApiDefinition } from '../models/ApiDefinition';
import { PolicyStudioService } from '../policy-studio.service';

@Component({
  selector: 'policy-studio-properties',
  template: require('./policy-studio-properties.component.html'),
  styles: [require('./policy-studio-properties.component.scss')],
})
export class PolicyStudioPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public provider: any;

  apiDefinition: ApiDefinition;

  providers: any;

  dynamicPropertySchema: any;

  constructor(
    private readonly policyStudioService: PolicyStudioService,
    private readonly policyStudioPropertiesService: PolicyStudioPropertiesService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.policyStudioService.getApiDefinition$(),
      this.policyStudioPropertiesService.getProviders(),
      this.policyStudioPropertiesService.getDynamicPropertySchema(),
    ])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([apiDefinition, providers, dynamicPropertySchema]) => {
          this.apiDefinition = apiDefinition;
          this.provider = apiDefinition.services['dynamic-property'];
          this.providers = providers;
          this.dynamicPropertySchema = dynamicPropertySchema;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChange($event: ChangePropertiesEvent) {
    this.apiDefinition.properties = $event.detail.properties;
    this.policyStudioService.emitApiDefinition(this.apiDefinition);
  }

  onSaveProvider($event: SaveProviderEvent) {
    this.apiDefinition.services['dynamic-property'] = $event.detail.provider;
    this.policyStudioService.emitApiDefinition(this.apiDefinition);
  }

  get isLoading() {
    return this.apiDefinition == null;
  }
}
