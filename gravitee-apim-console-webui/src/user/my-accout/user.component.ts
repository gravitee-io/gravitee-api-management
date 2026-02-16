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

import { CurrentUserService } from '../../services-ngx/current-user.service';
import { User } from '../../entities/user/user';
import { AuthService } from '../../auth/auth.service';
@Component({
  template: '',
  selector: 'user-my-account',
  standalone: false,
  host: {
    class: 'bootstrap',
  },
})
export class UserComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  @Output()
  onSaved!: EventEmitter<void>;

  @Output()
  onDeleteMyAccount!: EventEmitter<void>;

  user: User | null = null;

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly currentUserService: CurrentUserService,
    private readonly authService: AuthService,
  ) {
    super('user', elementRef, injector);
  }

  override ngOnInit() {
    this.currentUserService
      .current()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(user => {
        // Hack to Force the binding between Angular and AngularJS
        this.ngOnChanges({
          user: new SimpleChange(this.user, user, !this.user),
        });
      });
    super.ngOnInit();

    this.onSaved.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.currentUserService.updateCurrent();
    });

    this.onDeleteMyAccount.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this.authService.logout();
    });
  }

  override ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    super.ngOnDestroy();
  }
}
