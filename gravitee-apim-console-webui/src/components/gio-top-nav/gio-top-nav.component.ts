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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { IRootScopeService } from 'angular';
import { TransitionService } from '@uirouter/angularjs';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Constants } from '../../entities/Constants';
import { AjsRootScope, CurrentUserService, UIRouterState } from '../../ajs-upgraded-providers';
import UserService from '../../services/user.service';
import { User } from '../../entities/user/user';
import { TaskService } from '../../services-ngx/task.service';

@Component({
  selector: 'gio-top-nav',
  template: require('./gio-top-nav.component.html'),
  styles: [require('./gio-top-nav.component.scss')],
})
export class GioTopNavComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject();
  public displayDocumentationButton = false;
  public hasAlert = false;
  public currentUser: User;
  public userTaskCount = 0;
  public supportEnabled: boolean;
  public newsletterProposed: boolean;
  public customLogo: string;
  public isOEM: boolean;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(AjsRootScope) private readonly ajsRootScope: IRootScopeService,
    @Inject('Constants') public readonly constants: Constants,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    public readonly taskService: TaskService,
    public readonly $transitions: TransitionService,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.currentUserService.currentUser;
    this.newsletterProposed =
      (this.currentUser && !this.currentUser.firstLogin) ||
      !!window.localStorage.getItem('newsletterProposed') ||
      !this.constants.org.settings.newsletter.enabled;
    this.supportEnabled = this.constants.org.settings.management.support.enabled;
    this.isOEM = this.constants.isOEM;
    if (this.constants.customization && this.constants.customization.logo) {
      this.customLogo = this.constants.customization.logo;
    }
    this.taskService
      .getTasksAutoFetch()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((taskPagedResult) => {
        this.userTaskCount = taskPagedResult.page.total_elements;
        this.hasAlert = this.userTaskCount > 0;
      });
    this.displayDocumentationButton = !!this.ajsState.current.data?.docs;
    this.ajsRootScope.$on('$locationChangeStart', () => {
      this.displayDocumentationButton = !!this.ajsState.current.data?.docs;
    });
  }

  navigateToHome(): void {
    this.ajsState.go('management');
  }

  openContextualDocumentation = () => {
    if (window.pendo && window.pendo.isReady()) {
      // Do nothing Pendo use this button to trigger the "Resource Center"
      return;
    }

    this.ajsRootScope.$broadcast('openContextualDocumentation');
  };

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
}
