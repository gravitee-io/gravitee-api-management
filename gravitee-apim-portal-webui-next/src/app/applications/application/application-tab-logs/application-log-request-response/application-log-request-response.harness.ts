import {ComponentHarness} from "@angular/cdk/testing";
import {MatExpansionPanelHarness} from "@angular/material/expansion/testing";

export class ApplicationLogRequestResponseHarness extends ComponentHarness {
  public static hostSelector = 'app-application-log-request-response';

  async getRequestHeaders(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Request Headers');
  }

  async getRequestBody(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Request Body');
  }

  async getResponseHeaders(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Response Headers');
  }

  async  getResponseBody(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Response Body');
  }

  async getExpansionPanelByAriaLabel(value: string): Promise<MatExpansionPanelHarness> {
    return harnessLoader.getHarness(MatExpansionPanelHarness.with({ selector: `[aria-label="${value}"]` }));
  }
}
