import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PortalConstants } from '../entities/Constants';
import {
  NewPortalNavigationItem,
  PortalArea,
  PortalNavigationItem,
  PortalNavigationItemsResponse,
  UpdatePortalNavigationItem,
} from '../entities/portalNavigationItem';

@Injectable({
  providedIn: 'root',
})
export class PortalNavigationItemService {
  constructor(
    private http: HttpClient,
    @Inject(PortalConstants) private readonly constants: PortalConstants,
  ) {}

  public getNavigationItems(portalArea: PortalArea): Observable<PortalNavigationItemsResponse> {
    return this.http.get<PortalNavigationItemsResponse>(`${this.constants.env.v2BaseURL}/portal-navigation-items?area=${portalArea}`);
  }

  public createNavigationItem(newPortalNavigationItem: NewPortalNavigationItem): Observable<PortalNavigationItem> {
    return this.http.post<PortalNavigationItem>(`${this.constants.env.v2BaseURL}/portal-navigation-items`, newPortalNavigationItem);
  }

  public createNavigationItemsInBulk(items: NewPortalNavigationItem[]): Observable<PortalNavigationItemsResponse> {
    return this.http.post<PortalNavigationItemsResponse>(`${this.constants.env.v2BaseURL}/portal-navigation-items/_bulk`, { items });
  }

  public updateNavigationItem(
    portalNavigationItemId: string,
    updatePortalNavigationItem: UpdatePortalNavigationItem,
  ): Observable<PortalNavigationItem> {
    return this.http.put<PortalNavigationItem>(
      `${this.constants.env.v2BaseURL}/portal-navigation-items/${portalNavigationItemId}`,
      updatePortalNavigationItem,
    );
  }

  public deleteNavigationItem(portalNavigationItemId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/portal-navigation-items/${portalNavigationItemId}`);
  }
}
