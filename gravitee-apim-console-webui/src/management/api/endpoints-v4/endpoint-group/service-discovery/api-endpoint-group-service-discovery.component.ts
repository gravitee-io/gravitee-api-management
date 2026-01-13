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
import { UntypedFormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ResourceListItem } from '../../../../../entities/resource/resourceListItem';
import { ApiServicePluginsV2Service } from '../../../../../services-ngx/apiservice-plugins-v2.service';

@Component({
  selector: 'api-endpoint-group-service-discovery',
  templateUrl: './api-endpoint-group-service-discovery.component.html',
  styleUrls: ['./api-endpoint-group-service-discovery.component.scss'],
  standalone: false,
})
export class ApiEndpointGroupServiceDiscoveryComponent implements OnInit, OnDestroy {
  private readonly unsubscribe$ = new Subject<void>();

  @Input() serviceDiscoveryForm: UntypedFormGroup;
  @Input() serviceDiscoveryItems: ResourceListItem[];

  public schema: unknown;

  constructor(private readonly apiServicePluginsV2Service: ApiServicePluginsV2Service) {}

  ngOnInit(): void {
    const enabledControl = this.serviceDiscoveryForm.get('enabled');
    const typeControl = this.serviceDiscoveryForm.get('type');
    const configurationControl = this.serviceDiscoveryForm.get('configuration');
    if (enabledControl?.value) {
      this.getTypeSchema(typeControl?.value);
    }

    typeControl
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((type) => {
        configurationControl?.reset({});
        this.getTypeSchema(type);
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public get isServiceDiscoveryEnabled(): boolean {
    return this.serviceDiscoveryForm?.get('enabled')?.value === true;
  }

  private getTypeSchema(type: string): void {
    if (!type) {
      this.schema = undefined;
      return;
    }

    this.apiServicePluginsV2Service
      .getApiServicePluginSchema(type)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((schema) => (this.schema = schema));
  }
}
