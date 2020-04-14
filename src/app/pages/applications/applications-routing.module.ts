import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { GvButtonCreateApplicationComponent } from '../../components/gv-button-create-application/gv-button-create-application.component';
import { GvHeaderItemComponent } from '../../components/gv-header-item/gv-header-item.component';
import { GvSelectDashboardComponent } from '../../components/gv-select-dashboard/gv-select-dashboard.component';
import { FeatureEnum } from '../../model/feature.enum';
import { ApplicationResolver } from '../../resolvers/application.resolver';
import { DashboardsResolver } from '../../resolvers/dashboards.resolver';
import { FeatureGuardService } from '../../services/feature-guard.service';
import { ApplicationAnalyticsComponent } from '../application/application-analytics/application-analytics.component';
import { ApplicationCreationComponent } from '../application/application-creation/application-creation.component';
import { ApplicationGeneralComponent } from '../application/application-general/application-general.component';
import { ApplicationLogsComponent } from '../application/application-logs/application-logs.component';
import { ApplicationMembersComponent } from '../application/application-members/application-members.component';
import { ApplicationNotificationsComponent } from '../application/application-notifications/application-notifications.component';
import { ApplicationSubscriptionsComponent } from '../application/application-subscriptions/application-subscriptions.component';
import { SubscriptionsComponent } from '../subscriptions/subscriptions.component';
import { ApplicationsComponent } from './applications.component';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

const routes: Routes = [
  { path: '', redirectTo: 'mine', pathMatch: 'full' },
  {
    path: 'mine',
    component: ApplicationsComponent,
    data: {
      title: i18n('route.myApplications'),
      icon: 'devices:server',
      animation: { type: 'slide', group: 'apps', index: 1 },
      menu: { slots: { right: GvButtonCreateApplicationComponent, expectedFeature: FeatureEnum.applicationCreation } }
    }
  },
  {
    path: 'subscriptions',
    component: SubscriptionsComponent,
    data: {
      title: i18n('route.mySubscriptions'),
      icon: 'finance:share',
      animation: { type: 'slide', group: 'apps', index: 2 },
      menu: { slots: { right: GvButtonCreateApplicationComponent, expectedFeature: FeatureEnum.applicationCreation } }
    }
  },
  {
    path: 'creation',
    component: ApplicationCreationComponent,
    canActivate: [FeatureGuardService],
    data: {
      title: i18n('route.applicationCreation'),
      expectedFeature: FeatureEnum.applicationCreation,
      animation: { type: 'fade' },
    }
  },
  {
    path: ':applicationId',
    data: {
      menu: { slots: { top: GvHeaderItemComponent }, animation: { type: 'fade' } },
    },
    resolve: {
      application: ApplicationResolver
    },
    children: [
      {
        path: '',
        component: ApplicationGeneralComponent,
        data: {
          icon: 'general:clipboard',
          title: i18n('route.catalogApi'),
          animation: { type: 'slide', group: 'app', index: 1 }
        }
      },
      {
        path: 'subscriptions',
        component: ApplicationSubscriptionsComponent,
        data: {
          icon: 'home:key',
          title: i18n('route.subscriptions'),
          animation: { type: 'slide', group: 'app', index: 2 }
        }
      },
      {
        path: 'members',
        component: ApplicationMembersComponent,
        data: {
          icon: 'communication:group',
          title: i18n('route.members'),
          animation: { type: 'slide', group: 'apps', index: 3 }
        }
      },
      {
        path: 'analytics',
        component: ApplicationAnalyticsComponent,
        data: {
          icon: 'shopping:chart-line#1',
          menu: { slots: { right: GvSelectDashboardComponent } },
          title: i18n('route.analyticsApplication'),
          animation: { type: 'slide', group: 'app', index: 4 }
        },
        resolve: {
          dashboards: DashboardsResolver
        }
      },
      {
        path: 'logs',
        component: ApplicationLogsComponent,
        data: {
          icon: 'communication:clipboard-list',
          title: i18n('route.logsApplication'),
          animation: { type: 'slide', group: 'app', index: 5 }
        }
      },
      {
        path: 'notifications',
        component: ApplicationNotificationsComponent,
        data: {
          icon: 'general:notifications#2',
          title: i18n('route.notifications'),
          animation: { type: 'slide', group: 'app', index: 6 }
        }
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ApplicationsRoutingModule {
}
