import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { PortalConstants } from '../entities/Constants';
import { Theme, ThemeType, UpdateTheme } from '../entities/portalCustomization';

@Injectable({ providedIn: 'root' })
export class UiPortalThemeService {
  constructor(
    private readonly http: HttpClient,
    @Inject(PortalConstants) private readonly constants: PortalConstants,
  ) {}

  getDefaultTheme(themeType: ThemeType = 'PORTAL'): Observable<Theme> {
    return this.http.get<Theme>(`${this.constants.env.v2BaseURL}/ui/themes/_default?type=${themeType}`);
  }

  getCurrentTheme(themeType: ThemeType = 'PORTAL'): Observable<Theme> {
    return this.http.get<Theme>(`${this.constants.env.v2BaseURL}/ui/themes/_current?type=${themeType}`);
  }

  updateTheme(updateTheme: UpdateTheme): Observable<Theme> {
    return this.http.put<Theme>(`${this.constants.env.v2BaseURL}/ui/themes/${updateTheme.id}`, updateTheme);
  }
}
