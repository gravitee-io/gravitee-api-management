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

import { Component, Inject, OnInit } from '@angular/core';
import { capitalize, chain } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import { NotificationTemplate } from '../../../entities/notification/notificationTemplate';
import { NotificationTemplateService } from '../../../services-ngx/notification-template.service';
import { Constants } from '../../../entities/Constants';

interface NotificationTemplateVM {
  humanReadableScope: string;
  scope: string;
  name: string;
  hook: string;
  description: string;
  overridden: boolean;
  icon: string;
}

export type NotificationTemplatesByScope = Record<string, NotificationTemplateVM[]>;

@Component({
  selector: 'org-settings-notification-templates',
  styleUrls: ['./org-settings-notification-templates.component.scss'],
  templateUrl: './org-settings-notification-templates.component.html',
  standalone: false,
})
export class OrgSettingsNotificationTemplatesComponent implements OnInit {
  displayedColumns: string[] = ['name', 'description', 'actions'];

  notificationTemplatesByScope: NotificationTemplatesByScope;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly notificationTemplateService: NotificationTemplateService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.notificationTemplateService.search().subscribe(notificationTemplates => {
      this.notificationTemplatesByScope = this.groupNotificationTemplatesVMByScope(notificationTemplates);
    });
  }

  groupNotificationTemplatesVMByScope(notificationTemplates: NotificationTemplate[]): Record<string, NotificationTemplateVM[]> {
    let processedNotificationTemplates: NotificationTemplate[] = notificationTemplates;

    // Do not include TEMPLATES FOR ALERT if alert service is disabled
    if (!this.constants.org.settings.alert?.enabled) {
      processedNotificationTemplates = notificationTemplates.filter(
        notificationTemplate => notificationTemplate.scope !== 'TEMPLATES_FOR_ALERT',
      );
    }

    return chain(processedNotificationTemplates)
      .sort((a, b) => a.scope.localeCompare(b.scope) || a.name.localeCompare(b.name))
      .groupBy(notificationTemplate => `${notificationTemplate.scope}-${notificationTemplate.name}`)
      .mapValues(notificationTemplatesWithSameName => {
        const notificationTemplate = notificationTemplatesWithSameName[0];

        const notificationTemplateVM: NotificationTemplateVM = {
          humanReadableScope: this.getHumanReadableScope(notificationTemplate.scope),
          scope: notificationTemplate.scope,
          name: notificationTemplate.name,
          hook: notificationTemplate.hook,
          description: notificationTemplate.description,
          overridden: notificationTemplatesWithSameName.some(notificationTemplate => notificationTemplate.enabled === true),
          icon: this.getScopeIcon(notificationTemplate.scope),
        };
        return notificationTemplateVM;
      })
      .toArray()
      .groupBy('humanReadableScope')
      .value();
  }

  onEditActionClicked(notificationTemplateVM: NotificationTemplateVM) {
    const scope = notificationTemplateVM.scope;
    // FIXME: Find a way to handle this case in Notification Template screen and not here
    //   Keep it like this for now for compatibility
    //   If TEMPLATES TO INCLUDE, there is no hook. Must have at least a name to load the right template
    const hook =
      notificationTemplateVM.hook && notificationTemplateVM.hook !== '' ? notificationTemplateVM.hook : notificationTemplateVM.name;
    this.router.navigate(['../notification-templates', scope, hook], { relativeTo: this.activatedRoute });
  }

  getHumanReadableScope(scope: string): string {
    if (scope === 'API') {
      return 'API';
    }

    return capitalize(scope.split('_').join(' '));
  }

  getScopeIcon(scope: string): string {
    switch (scope) {
      case 'API':
        return 'dashboard';
      case 'APPLICATION':
        return 'list';
      case 'PORTAL':
        return 'important_devices';
      case 'TEMPLATES_FOR_ACTION':
        return 'assignment';
      case 'TEMPLATES_FOR_ALERT':
        return 'notifications';
      case 'TEMPLATES_TO_INCLUDE':
        return 'folder_open';
      default:
        return '';
    }
  }
}
