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

import { Component, Input, OnChanges } from '@angular/core';

export type TimelineStep =
  | {
      mode: 'CLIENT_APP';
    }
  | {
      mode: 'BACKEND_TARGET';
    }
  | {
      mode: 'REQUEST_INPUT';
    }
  | {
      mode: 'REQUEST_OUTPUT';
    };

interface TimelineCardVM {
  icon?: string;
  title?: string;
  color?: 'green' | 'blue' | 'default';
}
@Component({
  selector: 'policy-studio-debug-timeline-card',
  template: require('./policy-studio-debug-timeline-card.component.html'),
  styles: [require('./policy-studio-debug-timeline-card.component.scss')],
})
export class PolicyStudioDebugTimelineCardComponent implements OnChanges {
  @Input()
  public timelineStep?: TimelineStep;

  public timelineCardVM: TimelineCardVM = {};

  ngOnChanges(): void {
    if (!this.timelineStep) {
      return;
    }

    const timelineCardTemplate: Record<TimelineStep['mode'], TimelineCardVM> = {
      CLIENT_APP: {
        icon: 'gio:smartphone-device',
        title: 'Client APP',
        color: 'green',
      },
      BACKEND_TARGET: {
        icon: 'gio:city',
        title: 'Backend Target',
        color: 'green',
      },
      REQUEST_INPUT: {
        icon: 'gio:nav-arrow-right',
        title: 'Request Input',
        color: 'blue',
      },
      REQUEST_OUTPUT: {
        icon: 'gio:nav-arrow-right',
        title: 'Request Output',
        color: 'blue',
      },
    };

    this.timelineCardVM = timelineCardTemplate[this.timelineStep.mode];
  }
}
