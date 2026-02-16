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
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ResourceListItem } from '../../../../../../entities/resource/resourceListItem';
import { ServiceDiscoveryService } from '../../../../../../services-ngx/service-discovery.service';

@Component({
  selector: 'api-proxy-group-service-discovery',
  templateUrl: './api-proxy-group-service-discovery.component.html',
  styleUrls: ['./api-proxy-group-service-discovery.component.scss'],
  standalone: false,
})
export class ApiProxyGroupServiceDiscoveryComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() serviceDiscoveryForm: UntypedFormGroup;
  @Input() serviceDiscoveryItems: ResourceListItem[];

  public schema: unknown;

  constructor(private readonly serviceDiscoveryService: ServiceDiscoveryService) {}

  ngOnInit(): void {
    if (this.serviceDiscoveryForm.get('enabled').value) {
      this.getProviderSchema(this.serviceDiscoveryForm.get('provider').value);
    }

    this.serviceDiscoveryForm
      .get('provider')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe(provider => {
        this.serviceDiscoveryForm.get('configuration').reset({});
        this.getProviderSchema(provider);
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  private getProviderSchema(provider: string) {
    if (provider) {
      this.serviceDiscoveryService
        .getSchema(provider)
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe(schema => (this.schema = schema));
    }
  }
}
