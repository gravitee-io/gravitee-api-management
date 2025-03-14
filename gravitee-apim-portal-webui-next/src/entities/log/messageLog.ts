import {LogsResponseMetadata} from "./log";

export interface AggregatedMessageLog {
  requestId: string;
  timestamp: string;
  clientIdentifier: string;
  correlationId: string;
  parentCorrelationId: string;
  operation: MessageOperation;
  entrypoint: Message;
  endpoint: Message;
}

export interface Message {
  connectorId: string;
  timestamp: string;
  id: string;
  payload: string;
  isError: boolean;
  headers: Record<string, string[]>;
  metadata: Record<string, string>;
}

export type ConnectorType = 'ENDPOINT' | 'ENTRYPOINT';
export type MessageOperation = 'SUBSCRIBE' | 'PUBLISH';

export interface MessageLogsResponse {
  data: AggregatedMessageLog[];
  links?: unknown;
  metadata: LogsResponseMetadata;
}
