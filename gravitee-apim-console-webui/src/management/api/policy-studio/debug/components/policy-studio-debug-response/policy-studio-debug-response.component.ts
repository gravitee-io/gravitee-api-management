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

import { PolicyListItem } from '../../../../../../entities/policy';
import { DebugResponse } from '../../models/DebugResponse';
import { HttpStatusCodeDescription } from '../../models/HttpStatusCodeDescription';
import { TimelineStep } from '../policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';
import { RequestDebugStep, ResponseDebugStep } from '../../models/DebugStep';

type ResponseDisplayableVM = {
  statusCode: number;
  statusCodeDescription: string;
  successfulRequest: boolean;
  errorRequest: boolean;
  timelineSteps: TimelineStep[];
};

@Component({
  selector: 'policy-studio-debug-response',
  template: require('./policy-studio-debug-response.component.html'),
  styles: [require('./policy-studio-debug-response.component.scss')],
})
export class PolicyStudioDebugResponseComponent implements OnChanges {
  @Input()
  public debugResponse?: DebugResponse;

  @Input()
  public listPolicies?: PolicyListItem[];

  public responseDisplayableVM: ResponseDisplayableVM;

  public selectedStep: RequestDebugStep | ResponseDebugStep;

  ngOnChanges(): void {
    if (this.debugResponse && !this.debugResponse.isLoading) {
      this.responseDisplayableVM = {
        statusCode: this.debugResponse.response.statusCode,
        statusCodeDescription: HttpStatusCodeDescription[this.debugResponse.response.statusCode].description,
        successfulRequest: 200 <= this.debugResponse?.response.statusCode && this.debugResponse?.response.statusCode < 300,
        errorRequest: 400 <= this.debugResponse?.response.statusCode && this.debugResponse?.response.statusCode < 600,
        timelineSteps: this.toTimelineSteps(this.debugResponse),
      };
    }
  }

  onSelectTimelineStep(timelineStep: TimelineStep) {
    if (timelineStep.mode === 'POLICY') {
      this.selectedStep = [...this.debugResponse.requestDebugSteps, ...this.debugResponse.responseDebugSteps].find(
        (value) => value.id === timelineStep.id,
      );
    }
  }

  private toTimelineSteps(debugResponse: DebugResponse) {
    const timelineSteps: TimelineStep[] = [
      {
        mode: 'CLIENT_APP',
      },
      {
        mode: 'REQUEST_INPUT',
      },
      ...debugResponse.requestDebugSteps.map((debugStep) => {
        const policy = this.listPolicies?.find((p) => p.id === debugStep.policyId);

        return {
          executionTime: debugStep.duration,
          executionStatus: debugStep.status,
          policyName: policy?.name ?? debugStep.policyId,
          mode: 'POLICY' as const,
          icon: policy?.icon,
          flowName: debugStep.scope,
          id: debugStep.id,
        };
      }),
      {
        mode: 'REQUEST_OUTPUT',
      },
      {
        mode: 'BACKEND_TARGET',
      },
      {
        mode: 'RESPONSE_INPUT',
      },

      ...debugResponse.responseDebugSteps.map((debugStep) => {
        const policy = this.listPolicies?.find((p) => p.id === debugStep.policyId);

        return {
          executionTime: debugStep.duration,
          executionStatus: debugStep.status,
          policyName: policy?.name ?? debugStep.policyId,
          mode: 'POLICY' as const,
          icon: policy?.icon,
          flowName: debugStep.scope,
          id: debugStep.id,
        };
      }),
      {
        mode: 'RESPONSE_OUTPUT',
      },
      {
        mode: 'CLIENT_APP',
      },
    ];
    return timelineSteps;
  }
}
