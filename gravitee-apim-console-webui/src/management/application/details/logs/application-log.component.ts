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
import { UpgradeComponent } from '@angular/upgrade/static';
import { Component, ElementRef, Injector, SimpleChange } from '@angular/core';
import { Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { takeUntil } from 'rxjs/operators';

import { ApplicationService } from '../../../../services-ngx/application.service';

@Component({
  template: '',
  selector: 'application-log',
  standalone: false,
  host: {
    class: 'bootstrap',
  },
})
export class ApplicationLogComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationService: ApplicationService,
  ) {
    super('applicationLog', elementRef, injector);
  }

  override ngOnInit() {
    this.applicationService
      .getLog(
        this.activatedRoute.snapshot.params.applicationId,
        this.activatedRoute.snapshot.params.logId,
        this.activatedRoute.snapshot.queryParams.timestamp,
      )
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: log => {
          this.ngOnChanges({
            log: new SimpleChange(null, log, true),
            activatedRoute: new SimpleChange(null, this.activatedRoute, true),
          });

          super.ngOnInit();
        },
      });
  }

  override ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();

    super.ngOnDestroy();
  }
}
