export interface Connector {
  id: string;
  name: string;
}

export interface ConnectorsResponse {
  data: Connector[];
  links?: unknown;
  metadata?: unknown;
}
