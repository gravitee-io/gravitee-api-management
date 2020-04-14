import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateCompiler, TranslateLoader, TranslateModule, TranslatePipe } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';
import { GvButtonCreateApplicationComponent } from '../../components/gv-button-create-application/gv-button-create-application.component';
import { GvSelectDashboardComponent } from '../../components/gv-select-dashboard/gv-select-dashboard.component';
import { SharedModule } from '../../shared/shared.module';
import { ApplicationAnalyticsComponent } from '../application/application-analytics/application-analytics.component';
import { ApplicationCreationComponent } from '../application/application-creation/application-creation.component';
import { ApplicationGeneralComponent } from '../application/application-general/application-general.component';
import { ApplicationLogsComponent } from '../application/application-logs/application-logs.component';
import { ApplicationMembersComponent } from '../application/application-members/application-members.component';
import { ApplicationNotificationsComponent } from '../application/application-notifications/application-notifications.component';
import { ApplicationSubscriptionsComponent } from '../application/application-subscriptions/application-subscriptions.component';
import { SubscriptionsComponent } from '../subscriptions/subscriptions.component';

import { ApplicationsRoutingModule } from './applications-routing.module';
import { ApplicationsComponent } from './applications.component';


@NgModule({
  declarations: [
    ApplicationsComponent,
    ApplicationAnalyticsComponent,
    ApplicationGeneralComponent,
    ApplicationMembersComponent,
    ApplicationNotificationsComponent,
    ApplicationSubscriptionsComponent,
    SubscriptionsComponent,
    ApplicationCreationComponent,
    GvButtonCreateApplicationComponent,
    ApplicationLogsComponent,
    GvSelectDashboardComponent,
  ],
  entryComponents: [GvButtonCreateApplicationComponent, GvSelectDashboardComponent],
  imports: [
    CommonModule,
    ApplicationsRoutingModule,
    SharedModule,
    TranslateModule.forChild({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new TranslateHttpLoader(http),
        deps: [HttpClient]
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler
      }
    }),
  ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ],
})
export class ApplicationsModule { }
