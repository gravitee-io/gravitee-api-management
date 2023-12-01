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

import { Component, ElementRef, EventEmitter, Injector, Output, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  template: '',
  selector: 'support-ticket',
  host: {
    class: 'bootstrap',
  },
})
export class TicketComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  @Output()
  navigateToTicketsList!: EventEmitter<void>;

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {
    super('supportTicketComponentAjs', elementRef, injector);
  }

  ngOnInit() {
    const apiId = this.activatedRoute.snapshot.params.apiId;

    // Hack to Force the binding between Angular and AngularJS
    this.ngOnChanges({
      apiId: new SimpleChange(null, apiId, true),
    });

    this.navigateToTicketsList.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.router.navigate(['..', 'list'], { relativeTo: this.activatedRoute });
    });

    super.ngOnInit();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    super.ngOnDestroy();
  }
}
