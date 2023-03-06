import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { MessagePayload } from '../entities/message/messagePayload';

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  sendFromPortal(payload: MessagePayload): Observable<number> {
    return this.http.post<number>(`${this.constants.env.baseURL}/messages`, payload);
  }

  sendFromApi(apiId: string, payload: MessagePayload): Observable<number> {
    return this.http.post<number>(`${this.constants.env.baseURL}/apis/${apiId}/messages`, payload);
  }
}
