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
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { CockpitService, UtmCampaign } from '../../../services-ngx/cockpit.service';
import { InstallationService } from '../../../services-ngx/installation.service';
import { Installation } from '../../../entities/installation/installation';

@Component({
  selector: 'org-settings-cockpit',
  templateUrl: './org-settings-cockpit.component.html',
  styleUrls: ['./org-settings-cockpit.component.scss'],
  standalone: false,
})
export class OrgSettingsCockpitComponent implements OnInit, OnDestroy {
  isLoading = true;

  icon = '';
  title = '';
  message = '';

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly installationService: InstallationService,
    private readonly cockpitService: CockpitService,
  ) {}

  ngOnInit(): void {
    this.installationService
      .get()
      .pipe(
        tap(installation => {
          this.setupVM(installation);
          this.isLoading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private setupVM(installation: Installation): void {
    const cockpitInstallationStatus = installation.additionalInformation.COCKPIT_INSTALLATION_STATUS;
    const enhancedCockpitURL = this.cockpitService.addQueryParamsForAnalytics(
      installation.cockpitURL,
      UtmCampaign.DISCOVER_COCKPIT,
      cockpitInstallationStatus,
    );
    const cockpitLink = `<a href="${enhancedCockpitURL}" target="_blank">Gravitee Cloud</a>`;

    switch (cockpitInstallationStatus) {
      case 'PENDING':
        this.icon = 'schedule';
        this.title = 'Almost there!';
        this.message = `Your installation is connected but it still has to be accepted on ${cockpitLink}!`;
        return;

      case 'ACCEPTED':
        this.icon = 'check_circle';
        this.title = 'Congratulations!';
        this.message = `Your installation is now connected to ${cockpitLink}, you can now explore all the possibilities offered by Gravitee Cloud!`;
        return;

      case 'REJECTED':
        this.icon = 'warning';
        this.title = 'No luck!';
        this.message = `Seems that your installation is connected to ${cockpitLink}, but has been rejected...`;
        return;

      case 'DELETED':
        this.icon = 'gps_off';
        this.title = 'Installation unlinked!';
        this.message = `Seems that your installation is connected to ${cockpitLink}, but is not linked anymore...`;
        return;

      default:
        this.icon = 'explore';
        this.title = 'Meet Gravitee Cloud...';
        this.message = `Create an account on ${cockpitLink}, register your current installation and start creating new organizations and environments!`;
        return;
    }
  }
}
