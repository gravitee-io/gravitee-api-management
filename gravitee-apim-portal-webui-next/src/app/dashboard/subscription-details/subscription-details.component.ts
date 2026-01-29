/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import {Component, inject, input} from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { ActivatedRoute } from '@angular/router';
import {PlanSecurityEnum, PlanUsageConfiguration} from "../../../entities/plan/plan";
import {
  SubscriptionConsumerConfiguration,
  SubscriptionConsumerStatusEnum,
  SubscriptionStatusEnum
} from "../../../entities/subscription";
import {ApiType} from "../../../entities/api/api";

interface SubscriptionDetailsVM {
  result?: SubscriptionDetailsData;
  error?: boolean;
}

interface SubscriptionDetailsData {
  applicationName: string;
  planName: string;
  planSecurity: PlanSecurityEnum;
  planUsageConfiguration: PlanUsageConfiguration;
  subscriptionStatus: SubscriptionStatusEnum;
  consumerStatus: SubscriptionConsumerStatusEnum;
  failureCause?: string;
  createdAt?: string;
  updatedAt?: string;
  apiKey?: string;
  apiKeyConfigUsername?: string;
  entrypointUrls?: string[];
  clientId?: string;
  clientSecret?: string;
  apiType?: ApiType;
  consumerConfiguration?: SubscriptionConsumerConfiguration;
}

@Component({
  selector: 'app-subscription-details',
  imports: [MatProgressBar, MatIcon, MatButton],
  templateUrl: './subscription-details.component.html',
  styleUrl: './subscription-details.component.scss',
})
export default class SubscriptionDetailsComponent {
  private route = inject(ActivatedRoute);

  subscription = input.required<SubscriptionDetailsVM>();

  // Get the ID from the URL
  subscriptionId = this.route.snapshot.paramMap.get('subscriptionId');

  copyToClipboard() {
    navigator.clipboard.writeText(this.subscriptionId!);
    alert('copied!')
    // Optional: Show a snackbar "Copied!"
  }
}
