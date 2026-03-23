export type Phase = 'request' | 'response';

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS' | 'TRACE' | 'CONNECT';

export type PathOperator = 'STARTS_WITH' | 'EQUALS';

export interface HttpSelector {
  readonly type: 'HTTP';
  readonly path: string;
  readonly pathOperator: PathOperator;
  readonly methods?: HttpMethod[];
}

export type Selector = HttpSelector;

export interface Step {
  readonly id: string;
  readonly name?: string;
  readonly description?: string;
  readonly enabled: boolean;
  readonly policy: string;
  readonly configuration: Record<string, unknown>;
  readonly condition?: string;
}

export interface Flow {
  readonly name?: string;
  readonly enabled: boolean;
  readonly selectors?: Selector[];
  readonly request: Step[];
  readonly response: Step[];
}

export interface ApiV4 {
  readonly id: string;
  readonly name: string;
  readonly type: string;
  readonly definitionVersion: string;
  readonly flows?: Flow[];
  readonly listeners?: ReadonlyArray<{
    readonly type: string;
    readonly entrypoints?: ReadonlyArray<{ readonly type: string }>;
  }>;
}

export interface PolicyPlugin {
  readonly id: string;
  readonly name: string;
  readonly description?: string;
  readonly category?: string;
  readonly icon?: string;
  readonly flowPhaseCompatibility?: Record<string, Phase[]>;
}

export interface StepKey {
  readonly phase: Phase;
  readonly index: number;
}

export type DragData =
  | { type: 'step'; flowIndex: number; phase: Phase; index: number }
  | { type: 'policy'; policyId: string };
