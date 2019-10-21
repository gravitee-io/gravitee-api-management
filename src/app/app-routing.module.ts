import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {CatalogComponent} from './pages/catalog/catalog.component';
import {AppsComponent} from './pages/apps/apps.component';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';

export const explicitRoutes = [
  {path: 'dashboard', title: i18n('route.dashboard'), component: DashboardComponent},
  {path: 'catalog', title: i18n('route.catalog'), component: CatalogComponent},
  {path: 'apps', title: i18n('route.apps'), component: AppsComponent}
];

export const routes: Routes = explicitRoutes;

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
