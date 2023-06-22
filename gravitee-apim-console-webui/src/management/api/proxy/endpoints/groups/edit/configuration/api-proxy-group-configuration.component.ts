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
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-schema-form-group';

import { ConnectorService } from '../../../../../../../services-ngx/connector.service';

@Component({
  selector: 'api-proxy-group-configuration',
  template: require('./api-proxy-group-configuration.component.html'),
  styles: [require('./api-proxy-group-configuration.component.scss')],
})
export class ApiProxyGroupConfigurationComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() groupConfigurationControl: FormControl;

  public schemaForm: unknown;

  constructor(private readonly connectorService: ConnectorService) {}

  ngOnInit(): void {
    this.connectorService
      .list(true)
      .pipe(
        map((connectors) => {
          this.schemaForm = JSON.parse(connectors.find((connector) => connector.supportedTypes.includes('http'))?.schema);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  onConfigurationError(error: unknown) {
    // Set error at the end of js task. Otherwise it will be reset on value change
    setTimeout(() => {
      this.groupConfigurationControl.setErrors(error ? { error: true } : null);
    }, 0);
  }
}
