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
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { InstanceService } from '../../../services-ngx/instance.service';
import { Instance } from '../../../entities/instance/instance';

@Component({
  selector: 'instance-details',
  templateUrl: './instance-details.component.html',
  standalone: false,
})
export class InstanceDetailsComponent implements OnInit, OnDestroy {
  public instance: Instance;
  private unsubscribe$ = new Subject<void>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly instanceService: InstanceService,
  ) {}

  ngOnInit(): void {
    this.instanceService
      .get(this.activatedRoute.snapshot.params.instanceId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((instance) => {
        this.instance = instance;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
}
