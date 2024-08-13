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
import { MatTooltipModule } from '@angular/material/tooltip';

import { SharedPolicyGroup } from '../../../../entities/management-api-v2';

@Component({
  selector: 'shared-policy-groups-state-badge',
  standalone: true,
  imports: [MatTooltipModule],
  templateUrl: './shared-policy-groups-state-badge.component.html',
  styleUrl: './shared-policy-groups-state-badge.component.scss',
})
export class SharedPolicyGroupsStateBadgeComponent {
  @Input()
  lifecycleState: SharedPolicyGroup['lifecycleState'];
}
