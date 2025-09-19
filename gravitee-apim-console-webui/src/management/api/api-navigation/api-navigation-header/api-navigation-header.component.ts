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
import { Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItemHeader } from '../MenuGroupItem';
import { timeFrames } from '../../../../shared/utils/timeFrameRanges';

@Component({
  selector: 'api-navigation-header',
  templateUrl: './api-navigation-header.component.html',
  styleUrls: ['./api-navigation-header.component.scss'],
  standalone: false,
})
export class ApiNavigationHeaderComponent {
  @Input()
  public menuItemHeader: MenuItemHeader;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
  ) {}

  public navigateToURL(url: string) {
    if (url.includes('runtime-logs')) {
      this.navigateToLogs(url);
    }
  }

  private navigateToLogs(url: string) {
    const queryParams = this.activatedRoute.snapshot.queryParams;

    const queryParamsForLogs = {
      from: this.getFromAndToTimestamps(queryParams).fromTimestamp,
      to: this.getFromAndToTimestamps(queryParams).toTimestamp,
      statuses: queryParams.httpStatuses,
      planIds: queryParams.plans,
      applicationIds: queryParams.applications,
    };

    this.router.navigate([url], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsForLogs,
    });
  }

  private getFromAndToTimestamps(queryParams) {
    let fromTimestamp: number;
    let toTimestamp: number;
    if (queryParams.period === 'custom' && queryParams.from && queryParams.to) {
      fromTimestamp = +queryParams.from;
      toTimestamp = +queryParams.to;
    } else {
      const timeFrame = timeFrames.find((tf) => tf.id === queryParams.period);
      const timeRangeParams = timeFrame.timeFrameRangesParams();
      fromTimestamp = timeRangeParams.from;
      toTimestamp = timeRangeParams.to;
    }
    return { fromTimestamp, toTimestamp };
  }
}
