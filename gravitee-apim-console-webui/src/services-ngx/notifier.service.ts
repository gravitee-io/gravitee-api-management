import { Inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";

import { Constants } from "../entities/Constants";

@Injectable({
  providedIn: 'root'
})
export class NotifierService {

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) { }


  list() {
    return this.http.get(`${this.constants.env.baseURL}/notifiers/`);
  }

  getSchema(notifier: string) {
    return this.http.get(`${this.constants.env.baseURL}/notifiers/` + notifier + '/schema');
  }
}
