export interface Api {
  id: string;
  name: string;
  version: string;
  type?: 'NATIVE' | 'MESSAGE' | 'PROXY';
  definitionVersion: 'V2' | 'V4' | 'FEDERATED';
  description: string;
  _public?: boolean;
  running?: boolean;
  entrypoints: string[];
  listener_type?: string;
  labels?: string[];
  owner: { id: string; displayName: string };
  created_at?: string;
  updated_at?: string;
  categories?: string[];
  _links?: {
    self?: string;
    picture?: string;
    background?: string;
  };
}

export interface ApisResponse {
  data?: Api[];
  metadata?: {
    pagination?: {
      current_page?: number;
      first?: number;
      last?: number;
      size?: number;
      total?: number;
      total_pages?: number;
    };
  };
}
