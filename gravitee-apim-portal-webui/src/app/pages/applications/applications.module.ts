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
import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateCompiler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { GvAnalyticsDashboardComponent } from '../../components/gv-analytics-dashboard/gv-analytics-dashboard.component';
import { GvAnalyticsFiltersComponent } from '../../components/gv-analytics-filters/gv-analytics-filters.component';
import { GvButtonCreateApplicationComponent } from '../../components/gv-button-create-application/gv-button-create-application.component';
import { GvSelectDashboardComponent } from '../../components/gv-select-dashboard/gv-select-dashboard.component';
import { SharedModule } from '../../shared/shared.module';
import { ApplicationAnalyticsComponent } from '../application/application-analytics/application-analytics.component';
import { ApplicationCreationStep1Component } from '../application/application-creation/application-creation-step1/application-creation-step1.component';
import { ApplicationCreationStep2Component } from '../application/application-creation/application-creation-step2/application-creation-step2.component';
import { ApplicationCreationStep3Component } from '../application/application-creation/application-creation-step3/application-creation-step3.component';
import { ApplicationCreationStep4Component } from '../application/application-creation/application-creation-step4/application-creation-step4.component';
import { ApplicationCreationStep5Component } from '../application/application-creation/application-creation-step5/application-creation-step5.component';
import { ApplicationCreationComponent } from '../application/application-creation/application-creation.component';
import { ApplicationGeneralComponent } from '../application/application-general/application-general.component';
import { ApplicationLogsComponent } from '../application/application-logs/application-logs.component';
import { ApplicationMembersComponent } from '../application/application-members/application-members.component';
import { ApplicationMetadataComponent } from '../application/application-metadata/application-metadata.component';
import { ApplicationNotificationsComponent } from '../application/application-notifications/application-notifications.component';
import { ApplicationSubscriptionsComponent } from '../application/application-subscriptions/application-subscriptions.component';
import { SubscriptionsComponent } from '../subscriptions/subscriptions.component';

import { ApplicationsRoutingModule } from './applications-routing.module';
import { ApplicationsComponent } from './applications.component';
import { ApplicationAlertsComponent } from '../application/application-alerts/application-alerts.component';
import { GvAlertComponent } from '../../components/gv-alert/gv-alert.component';

@NgModule({
  declarations: [
    ApplicationsComponent,
    ApplicationAnalyticsComponent,
    ApplicationGeneralComponent,
    ApplicationMembersComponent,
    ApplicationMetadataComponent,
    ApplicationNotificationsComponent,
    ApplicationSubscriptionsComponent,
    SubscriptionsComponent,
    ApplicationCreationComponent,
    ApplicationCreationStep1Component,
    ApplicationCreationStep2Component,
    ApplicationCreationStep3Component,
    ApplicationCreationStep4Component,
    ApplicationCreationStep5Component,
    GvButtonCreateApplicationComponent,
    GvAnalyticsDashboardComponent,
    GvAnalyticsFiltersComponent,
    ApplicationLogsComponent,
    ApplicationAlertsComponent,
    GvAlertComponent,
    GvSelectDashboardComponent,
  ],
  imports: [
    ApplicationsRoutingModule,
    CommonModule,
    SharedModule,
    TranslateModule.forChild({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new TranslateHttpLoader(http),
        deps: [HttpClient],
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler,
      },
    }),
  ],
  exports: [SharedModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ApplicationsModule {}
