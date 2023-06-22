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
import { FormGroup } from '@angular/forms';
import { startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, of, Subject } from 'rxjs';
import '@gravitee/ui-components/wc/gv-schema-form-group';

import { ResourceListItem } from '../../../../../../../entities/resource/resourceListItem';
import { ServiceDiscoveryService } from '../../../../../../../services-ngx/service-discovery.service';

@Component({
  selector: 'api-proxy-group-service-discovery',
  template: require('./api-proxy-group-service-discovery.component.html'),
  styles: [require('./api-proxy-group-service-discovery.component.scss')],
})
export class ApiProxyGroupServiceDiscoveryComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() serviceDiscoveryForm: FormGroup;
  @Input() serviceDiscoveryItems: ResourceListItem[];

  public schema: unknown;
  public displaySchema: boolean;

  constructor(private readonly serviceDiscoveryService: ServiceDiscoveryService) {}

  ngOnInit(): void {
    combineLatest([
      this.serviceDiscoveryForm
        .get('enabled')
        .valueChanges.pipe(startWith(this.serviceDiscoveryForm.get('enabled').value), takeUntil(this.unsubscribe$)),
      this.serviceDiscoveryForm
        .get('type')
        .valueChanges.pipe(startWith(this.serviceDiscoveryForm.get('type').value), takeUntil(this.unsubscribe$)),
    ])
      .pipe(
        switchMap(([enabled, type]) => {
          const typeControl = this.serviceDiscoveryForm.get('type');
          enabled ? typeControl.enable({ emitEvent: false }) : typeControl.disable({ emitEvent: false });

          if (enabled && type) {
            return this.serviceDiscoveryService.getSchema(type);
          } else {
            return of(null);
          }
        }),
        tap((schema) => {
          this.serviceDiscoveryForm.get('configuration').reset({});
          this.schema = schema;
          this.displaySchema = !!schema;
        }),
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
      this.serviceDiscoveryForm.get('configuration').setErrors(error ? { error: true } : null);
    }, 0);
  }
}
