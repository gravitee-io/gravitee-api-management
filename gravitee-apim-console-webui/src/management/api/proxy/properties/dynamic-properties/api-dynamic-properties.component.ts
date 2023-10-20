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
import { combineLatest, Subject } from 'rxjs';
import { StateParams } from '@uirouter/angularjs';

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { FormControl, FormGroup } from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiPropertiesOldService } from '../../properties-ng/api-properties-old.service';

@Component({
  selector: 'api-dynamic-properties',
  template: require('./api-dynamic-properties.component.html'),
  styles: [require('./api-dynamic-properties.component.scss')],
})
export class ApiDynamicPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public transformationJOLTExample = `[
  {
    "key": 1,
      "value": "https://north-europe.company.com/"
  },
  {
    "key": 2,
    "value": "https://north-europe.company.com/"
  },
  {
    "key": 3,
    "value": "https://south-asia.company.com/"
  }
]`;
  public transformationJOLTExampleCollapse = true;

  public form: FormGroup;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiService: ApiV2Service,
    private readonly apiPropertiesService: ApiPropertiesOldService,
  ) {}

  ngOnInit(): void {
    combineLatest([this.apiService.get(this.ajsStateParams.apiId), this.apiPropertiesService.getProviders()])
      .pipe(
        tap(([api, providers]) => {
          if (api.definitionVersion === 'V1') {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }
          // this.providers = providers;
          const isReadonly = api.definitionContext.origin === 'KUBERNETES';

          this.form = new FormGroup({
            enabled: new FormControl({
              value: false,
              disabled: isReadonly,
            }),
          });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onSave() {}
}
