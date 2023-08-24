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
import { map, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ResourceListItem } from '../../../../../../../entities/resource/resourceListItem';
import { ServiceDiscoveryService } from '../../../../../../../services-ngx/service-discovery.service';
import { SchemaFormEvent } from '../../api-proxy-groups.model';

@Component({
  selector: 'api-proxy-group-service-discovery',
  template: require('./api-proxy-group-service-discovery.component.html'),
  styles: [require('./api-proxy-group-service-discovery.component.scss')],
})
export class ApiProxyGroupServiceDiscoveryComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() serviceDiscoveryForm: FormGroup;
  @Input() serviceDiscoveryItems: ResourceListItem[];
  @Input() isReadOnly: boolean;

  public schema: unknown;

  constructor(private readonly serviceDiscoveryService: ServiceDiscoveryService) {}

  ngOnInit(): void {
    if (this.serviceDiscoveryForm.get('enabled').value) {
      this.onFormValuesChange(this.serviceDiscoveryForm.get('provider').value);
    }

    this.serviceDiscoveryForm
      .get('provider')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.onFormValuesChange(value);
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  onSchemaFormChange(event: SchemaFormEvent) {
    if (event.detail?.validation?.errors?.length > 0) {
      this.serviceDiscoveryForm.setErrors({ invalidServiceDiscovery: true });
    } else {
      if (this.serviceDiscoveryForm.getError('invalidServiceDiscovery')) {
        delete this.serviceDiscoveryForm.errors['invalidServiceDiscovery'];
        this.serviceDiscoveryForm.updateValueAndValidity();
      }
    }
  }

  private onFormValuesChange(provider: string) {
    if (provider) {
      // reset schema to force form component to reload
      this.schema = null;
      this.serviceDiscoveryService
        .getSchema(provider)
        .pipe(
          map((schema) => {
            this.serviceDiscoveryForm.get('configuration').reset({});
            this.schema = schema;
          }),
        )
        .subscribe();
    }
  }
}
