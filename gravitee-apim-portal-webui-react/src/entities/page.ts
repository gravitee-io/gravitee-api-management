export type PageType = 'ASCIIDOC' | 'ASYNCAPI' | 'SWAGGER' | 'MARKDOWN' | 'FOLDER' | 'ROOT' | 'LINK';

export interface Page {
  id: string;
  name: string;
  type: PageType;
  order: number;
  parent?: string;
  updated_at?: string;
  content?: string;
  _links?: { self?: string; content?: string };
}

export interface PagesResponse {
  data?: Page[];
  metadata?: Record<string, unknown>;
}
