import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {ConfigService} from "./config.service";
import {Observable} from "rxjs";
import {ConnectorsResponse} from "../entities/connector/connector";

@Injectable({
  providedIn: 'root'
})
export class ConnectorService {
  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  getEntrypoints(): Observable<ConnectorsResponse> {
    return this.http.get<ConnectorsResponse>(`${this.configService.baseURL}/entrypoints`);
  }

  getEndpoints(): Observable<ConnectorsResponse> {
    return this.http.get<ConnectorsResponse>(`${this.configService.baseURL}/endpoints`);
  }
}
