import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {CatalogComponent} from './pages/catalog/catalog.component';
import {AppsComponent} from './pages/apps/apps.component';
import {LoginComponent} from './login/login.component';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';
import { UserComponent } from './user/user.component';
import { AppComponent } from './app.component';
import { LogoutComponent } from './logout/logout.component';



export const routes = [
  {path: 'dashboard', title: i18n('route.dashboard'), component: DashboardComponent},
  {path: 'catalog', title: i18n('route.catalog'), component: CatalogComponent},
  {path: 'apps', title: i18n('route.apps'), component: AppsComponent}
];

export const userRoutes = [
  {path: 'login', title: i18n('route.login'), component: LoginComponent},
  {path: 'logout', title: i18n('route.logout'), component: LogoutComponent},
  {path: 'user', title: i18n('route.user'), component: UserComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes), RouterModule.forRoot(userRoutes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
