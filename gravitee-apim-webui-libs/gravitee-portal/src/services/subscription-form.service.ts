import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PortalConstants } from '../entities/Constants';
import { SubscriptionForm, UpdateSubscriptionForm } from '../entities/subscriptionForm';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionFormService {
  constructor(
    private http: HttpClient,
    @Inject(PortalConstants) private readonly constants: PortalConstants,
  ) {}

  public getSubscriptionForm(): Observable<SubscriptionForm> {
    return this.http.get<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms`);
  }

  public updateSubscriptionForm(id: string, content: UpdateSubscriptionForm): Observable<SubscriptionForm> {
    return this.http.put<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms/${id}`, content);
  }

  public enableSubscriptionForm(id: string): Observable<SubscriptionForm> {
    return this.http.post<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms/${id}/_enable`, {});
  }

  public disableSubscriptionForm(id: string): Observable<SubscriptionForm> {
    return this.http.post<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms/${id}/_disable`, {});
  }
}
