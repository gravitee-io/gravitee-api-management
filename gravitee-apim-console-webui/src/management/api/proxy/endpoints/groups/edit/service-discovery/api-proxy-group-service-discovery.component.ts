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
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { map, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ServiceDiscoveryEvent } from './api-proxy-group-service-discovery.model';

import { ResourceListItem } from '../../../../../../../entities/resource/resourceListItem';
import { ProxyGroup } from '../../../../../../../entities/proxy';
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
  @Input() group: ProxyGroup;
  @Input() isReadOnly: boolean;
  @Output() onServiceDiscoveryConfigurationChange = new EventEmitter<ServiceDiscoveryEvent>();

  public schema: unknown;
  public displaySchema: boolean;

  constructor(private readonly serviceDiscoveryService: ServiceDiscoveryService) {}

  ngOnInit(): void {
    if (this.group?.services?.discovery.enabled) {
      this.onFormValuesChange(this.group.services.discovery.provider);
    }

    this.serviceDiscoveryForm.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((values) => {
      const typeControl = this.serviceDiscoveryForm.get('type');
      if (values.enabled) {
        typeControl.enable({ emitEvent: false });
        this.onFormValuesChange(typeControl.value);
      } else {
        typeControl.disable({ emitEvent: false });
        this.displaySchema = false;
        this.schema = null;
      }
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  onSchemaFormChange(event: SchemaFormEvent) {
    this.onServiceDiscoveryConfigurationChange.emit({
      isSchemaValid: !event.detail?.validation?.errors?.length,
      serviceDiscoveryValues: event.detail?.values,
    });
  }

  private onFormValuesChange(type: string) {
    if (type) {
      this.serviceDiscoveryService
        .getSchema(type)
        .pipe(
          map((schema) => {
            this.schema = schema;
            this.displaySchema =
              this.serviceDiscoveryForm.get('enabled').value === true && !!this.serviceDiscoveryForm.get('type').value && !!this.schema;
          }),
        )
        .subscribe();
    }
  }
}
