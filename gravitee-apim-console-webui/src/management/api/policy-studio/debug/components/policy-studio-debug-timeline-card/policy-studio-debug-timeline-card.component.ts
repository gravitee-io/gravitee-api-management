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
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

export type TimelineStep = (
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
    }
  | {
      mode: 'POLICY';
      flowName: string;
      policyName: string;
      icon: string;
      executionTime: number;
      executionStatus: 'ERROR' | 'SKIPPED' | 'COMPLETED';
      id: string;
    }
  | {
      mode: 'RESPONSE_INPUT';
    }
  | {
      mode: 'RESPONSE_OUTPUT';
    }
) & { id: string };

interface TimelineCardVM {
  icon?: string;
  iconUrl?: SafeUrl;
  headerLabel?: string;
  title?: string;
  color?: 'green' | 'blue' | 'default';
  executionTimeLabel?: string;
  rightIcon?: string;
  id?: string;
}

@Component({
  selector: 'policy-studio-debug-timeline-card',
  template: require('./policy-studio-debug-timeline-card.component.html'),
  styles: [require('./policy-studio-debug-timeline-card.component.scss')],
})
export class PolicyStudioDebugTimelineCardComponent implements OnChanges {
  @Input()
  public timelineStep?: TimelineStep;

  @Input()
  public selected?: boolean;

  public timelineCardVM: TimelineCardVM = {};

  constructor(private readonly sanitizer: DomSanitizer) {}

  ngOnChanges(): void {
    if (!this.timelineStep) {
      return;
    }
    this.timelineCardVM = this.toTimelineCardVM(this.timelineStep);
  }

  private toTimelineCardVM(timelineStep: TimelineStep): TimelineCardVM {
    switch (timelineStep.mode) {
      case 'CLIENT_APP':
        return {
          icon: 'gio:smartphone-device',
          title: 'Client APP',
          color: 'green',
        };
      case 'BACKEND_TARGET':
        return {
          icon: 'gio:city',
          title: 'Backend Target',
          color: 'green',
        };
      case 'REQUEST_INPUT':
        return {
          icon: 'gio:nav-arrow-right',
          title: 'Request Input',
          color: 'blue',
        };
      case 'REQUEST_OUTPUT':
        return {
          icon: 'gio:nav-arrow-right',
          title: 'Request Output',
          color: 'blue',
        };
      case 'POLICY':
        return {
          iconUrl: this.sanitizer.bypassSecurityTrustUrl(timelineStep.icon),
          headerLabel: timelineStep.flowName,
          title: timelineStep.policyName,
          color: 'default',
          executionTimeLabel: `${timelineStep.executionTime / 1_000_000}ms`,
          rightIcon: timelineStep.executionStatus === 'ERROR' ? 'gio:warning-circled-outline' : undefined,
          id: timelineStep.id,
        };
      case 'RESPONSE_INPUT':
        return {
          icon: 'gio:nav-arrow-right',
          title: 'Response Input',
          color: 'blue',
        };
      case 'RESPONSE_OUTPUT':
        return {
          icon: 'gio:nav-arrow-right',
          title: 'Response Output',
          color: 'blue',
        };
    }
  }
}
