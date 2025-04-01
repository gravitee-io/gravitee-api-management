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
import { Component, ElementRef, Injector, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { ActivatedRoute } from '@angular/router';

import { ApiService } from '../../../services-ngx/api.service';

@Component({
  template: '',
  selector: 'api-properties-v1',
  standalone: false,
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
export class ApiV1PropertiesComponent extends UpgradeComponent {
  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiService,
  ) {
    super('apiV1PropertiesComponentAjs', elementRef, injector);
  }

  override ngOnInit() {
    // Hack to Force the binding between Angular and AngularJS
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .toPromise()
      .then((api) => {
        this.ngOnChanges({
          resolvedApi: new SimpleChange(null, api, true),
        });
        super.ngOnInit();
      });
  }
}
