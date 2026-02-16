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
import { Component, EventEmitter, Inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { isEmpty } from 'lodash';

import { Constants } from '../../entities/Constants';
import { User } from '../../entities/user/user';
import { TaskService } from '../../services-ngx/task.service';
import { UiCustomizationService } from '../../services-ngx/ui-customization.service';
import { EnvironmentSettingsService } from '../../services-ngx/environment-settings.service';

@Component({
  selector: 'gio-top-nav',
  templateUrl: './gio-top-nav.component.html',
  styleUrls: ['./gio-top-nav.component.scss'],
  standalone: false,
})
export class GioTopNavComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  @Input()
  displayContextualDocumentationButton = false;

  @Output()
  public openContextualDocumentationPage = new EventEmitter<void>();

  public hasAlert = false;
  public currentUser: User;
  public userTaskCount = 0;
  public supportEnabled: boolean;
  public newsletterProposed: boolean;
  public customLogo: string;
  public isOEM: boolean;
  public portalUrl?: string;

  constructor(
    @Inject(Constants) public readonly constants: Constants,
    public readonly taskService: TaskService,
    private readonly uiCustomizationService: UiCustomizationService,
    private readonly licenseService: GioLicenseService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
  ) {}

  ngOnInit(): void {
    this.supportEnabled = this.constants.org.settings.management.support.enabled;
    this.isOEM = this.constants.isOEM;
    if (this.constants.customization && this.constants.customization.logo) {
      this.customLogo = this.constants.customization.logo;
    }
    this.taskService
      .getTasksAutoFetch()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(taskPagedResult => {
        this.userTaskCount = taskPagedResult.page.total_elements;
        this.hasAlert = this.userTaskCount > 0;
      });

    this.licenseService
      .isOEM$()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(isOEM => {
        this.isOEM = isOEM;
      });

    this.uiCustomizationService
      .getConsoleCustomization()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(customization => {
        if (customization && customization.logo) {
          this.customLogo = customization.logo;
        }
      });

    this.environmentSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(settings => {
        this.portalUrl = isEmpty(settings?.portal?.url)
          ? undefined
          : this.constants.env.baseURL.replace('{:envId}', this.constants.org.currentEnv.id) + '/portal/redirect';
      });
  }

  openContextualDocumentationClick = () => {
    if (window.pendo && window.pendo.isReady()) {
      // Do nothing Pendo use this button to trigger the "Resource Center"
      return;
    }

    this.openContextualDocumentationPage.emit();
  };

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }
}
