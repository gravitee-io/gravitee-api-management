import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

export interface ApiSearchResult {
  id: string;
  name: string;
  definitionVersion?: string;
  labels?: string[];
  listeners?: Array<{ paths?: Array<{ path: string }> }>;
  proxy?: { virtualHosts?: Array<{ path: string }> };
}

export interface ApiSearchResponse {
  data: ApiSearchResult[];
  pagination?: { totalCount?: number };
}

export interface ApiSearchService {
  search(query: { query: string }, sortBy: any, page: number, size: number, manageOnly: boolean): Observable<ApiSearchResponse>;
}

export const API_SEARCH_SERVICE = new InjectionToken<ApiSearchService>('API_SEARCH_SERVICE');
