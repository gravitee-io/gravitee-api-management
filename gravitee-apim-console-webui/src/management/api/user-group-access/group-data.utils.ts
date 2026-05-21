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
import { GroupData } from './members/api-general-members.component';

import { Group } from '../../../entities/management-api-v2';

/**
 * Maps API group references to UI group data.
 * V2 APIs persist group names in {@code api.groups}; V4+ APIs persist group IDs.
 */
export function mapApiGroupsToGroupData(apiGroupRefs: string[] | undefined, allGroups: Group[]): GroupData[] {
  const groupData: GroupData[] = [];
  for (const ref of apiGroupRefs ?? []) {
    const group = allGroups.find((g) => g.id === ref || g.name === ref);
    if (group) {
      groupData.push({ id: group.id, name: group.name, isVisible: true });
    }
  }
  return groupData;
}

export function isGroupAssociatedWithApi(apiGroupRefs: string[] | undefined, group: Group): boolean {
  return apiGroupRefs?.some((ref) => ref === group.id || ref === group.name) ?? false;
}
