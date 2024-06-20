import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { CustomUserField } from '../entities/customUserFields';

@Injectable({
  providedIn: 'root'
})
export class CustomUserFieldsService {

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) { }


  list(page: number = 1, size: number = 10): Observable<CustomUserField[]> {
    return this.httpClient.get<CustomUserField[]>(`${this.constants.org.baseURL}/configuration/custom-user-fields?page=${page}&perPage=${size}`);
  }

  create(field: CustomUserField): Observable<CustomUserField> {
    return this.httpClient.post<CustomUserField>(`${this.constants.org.baseURL}/configuration/custom-user-fields`, field);
  }

  update(field: CustomUserField): Observable<CustomUserField> {
    return this.httpClient.put<CustomUserField>(`${this.constants.org.baseURL}/configuration/custom-user-fields` + '/' + field.key, field);
  }

  delete(key: string) {
    return this.httpClient.delete(`${this.constants.org.baseURL}/configuration/custom-user-fields` + '/' + key);
  }
}
