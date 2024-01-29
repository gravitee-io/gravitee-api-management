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
import { combineLatest, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { takeUntil } from 'rxjs/operators';

import { ApplicationService } from '../../../../services-ngx/application.service';

@Component({
  template: '',
  selector: 'application-subscription',
  host: {
    class: 'bootstrap',
  },
})
export class ApplicationSubscriptionComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationService: ApplicationService,
  ) {
    super('applicationSubscription', elementRef, injector);
  }

  override ngOnInit() {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;
    combineLatest([
      this.applicationService.getLastApplicationFetch(applicationId),
      this.applicationService.getSubscription(applicationId, this.activatedRoute.snapshot.params.subscriptionId),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: ([application, subscription]) => {
          this.ngOnChanges({
            application: new SimpleChange(null, application, true),
            subscription: new SimpleChange(null, subscription, true),
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
