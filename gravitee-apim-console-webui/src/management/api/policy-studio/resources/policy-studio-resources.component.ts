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
import { ResourceListItem } from '@gravitee/ui-policy-studio-angular';
import '@gravitee/ui-components/wc/gv-resources';

import { PolicyStudioResourcesService } from './policy-studio-resources.service';

import { ApiDefinition } from '../models/ApiDefinition';
import { PolicyStudioService } from '../policy-studio.service';

@Component({
  selector: 'policy-studio-resources',
  template: require('./policy-studio-resources.component.html'),
  styles: [require('./policy-studio-resources.component.scss')],
})
export class PolicyStudioResourcesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  apiDefinition: ApiDefinition;

  resourceTypes: ResourceListItem[];

  constructor(
    private readonly policyStudioService: PolicyStudioService,
    private readonly policyStudioResourcesService: PolicyStudioResourcesService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.policyStudioService.getApiDefinition$(),
      this.policyStudioResourcesService.listResources({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([definition, resourceTypes]) => {
          this.apiDefinition = definition;
          this.resourceTypes = resourceTypes;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChange($event: any) {
    this.apiDefinition.resources = $event.detail.resources;
    this.policyStudioService.emitApiDefinition(this.apiDefinition);
  }

  get isLoading() {
    return this.apiDefinition == null;
  }
}
