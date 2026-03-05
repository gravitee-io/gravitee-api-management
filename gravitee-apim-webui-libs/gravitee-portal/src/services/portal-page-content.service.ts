import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PortalConstants } from '../entities/Constants';
import { NewPortalPageContent, PortalPageContent, UpdatePortalPageContent } from '../entities/portalPageContent';

@Injectable({
  providedIn: 'root',
})
export class PortalPageContentService {
  constructor(
    private http: HttpClient,
    @Inject(PortalConstants) private readonly constants: PortalConstants,
  ) {}

  public getPageContent(contentId: string): Observable<PortalPageContent> {
    return this.http.get<PortalPageContent>(`${this.constants.env.v2BaseURL}/portal-page-contents/${contentId}`);
  }

  public createPageContent(newPortalPageContent: NewPortalPageContent): Observable<PortalPageContent> {
    return this.http.post<PortalPageContent>(`${this.constants.env.v2BaseURL}/portal-page-contents`, newPortalPageContent);
  }

  public updatePageContent(contentId: string, content: UpdatePortalPageContent): Observable<PortalPageContent> {
    return this.http.put<PortalPageContent>(`${this.constants.env.v2BaseURL}/portal-page-contents/${contentId}`, content);
  }
}
