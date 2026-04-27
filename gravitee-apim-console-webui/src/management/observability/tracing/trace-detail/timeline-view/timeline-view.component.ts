import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Trace, TraceSpan } from '../../tracing.model';
import { ENTITY_TYPE_COLORS } from '../../entity-colors';

interface FlatSpan {
  span: TraceSpan;
  depth: number;
  offsetPercent: number;
  widthPercent: number;
  hasChildren: boolean;
  collapsed: boolean;
  hidden: boolean;
  spanType: string;
}

const LLM_OPS = new Set(['chat', 'generate_content', 'text_completion', 'stream_generate_content']);

function classifySpan(span: TraceSpan): string {
  const op = span.attributes['gen_ai.operation.name'];
  if (op === 'invoke_agent' || span.operationName.startsWith('invoke_agent ')) return 'agent';
  if (span.attributes['mcp.method.name']
    || (span.attributes['gen_ai.tool.name'] && op === 'execute_tool')) return 'mcp_server';
  if (op && LLM_OPS.has(op)) return 'llm';
  return 'gateway';
}

const GATEWAY_COLOR = '#4caf50';

function getSpanColor(spanType: string): string {
  const entityColor = ENTITY_TYPE_COLORS[spanType];
  if (entityColor) return entityColor.background;
  return GATEWAY_COLOR;
}

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="timeline-container">
      <!-- Time ruler header -->
      <div class="timeline-header">
        <div class="span-info-col header-label">SERVICE / OPERATION</div>
        <div class="time-col">
          <div class="time-ruler">
            @for (tick of timeTicks; track tick.label) {
              <div class="tick" [style.left.%]="tick.percent">
                <span class="tick-label">{{ tick.label }}</span>
              </div>
            }
          </div>
        </div>
      </div>

      <!-- Span rows -->
      <div class="timeline-body">
        @for (item of flatSpans; track item.span.spanId) {
          @if (!item.hidden) {
            <div class="span-row"
                 [class.selected]="selectedSpanId === item.span.spanId"
                 (click)="selectSpan(item)">
              <div class="span-info-col">
                <div class="span-label" [style.padding-left.px]="item.depth * 16">
                  @if (item.hasChildren) {
                    <button class="toggle-btn" (click)="toggleCollapse(item); $event.stopPropagation()">
                      <span class="material-icons">{{ item.collapsed ? 'chevron_right' : 'expand_more' }}</span>
                    </button>
                  } @else {
                    <span class="toggle-spacer"></span>
                  }
                  <span class="service-badge"
                        [style.background]="getSpanColor(item.spanType)">
                    {{ getSpanLabel(item) }}
                  </span>
                  <span class="operation-name">{{ item.span.operationName }}</span>
                </div>
              </div>
              <div class="time-col">
                <div class="bar-track">
                  <div class="bar"
                       [class.bar-error]="isError(item.span)"
                       [style.left.%]="item.offsetPercent"
                       [style.width.%]="mathMax(item.widthPercent, 0.5)"
                       [style.background]="isError(item.span) ? '#dc2626' : getSpanColor(item.spanType)"
                       [title]="formatDuration(item.span.durationNanos) + (isError(item.span) ? ' • ERROR' : '')">
                  </div>
                  @if (isError(item.span)) {
                    <span class="bar-error-mark material-icons">error</span>
                  }
                </div>
              </div>
            </div>
          }
        }

        @if (flatSpans.length === 0) {
          <div class="empty-state">
            <span class="material-icons">hourglass_empty</span>
            <div>No spans available</div>
          </div>
        }
      </div>

      <!-- Detail panel -->
      @if (selectedSpan) {
        <div class="detail-panel">
          <div class="detail-header">
            <div class="detail-title">
              <span class="material-icons">info</span>
              SPAN DETAIL &mdash; {{ selectedSpan.operationName }}
            </div>
            <button class="close-btn" (click)="closeDetail()">
              <span class="material-icons">close</span>
            </button>
          </div>
          <div class="detail-body">
            <div class="detail-summary">
              <div class="summary-item">
                <span class="summary-label">Service</span>
                <span class="summary-value">
                  <span class="service-dot" [style.background]="getSelectedSpanColor()"></span>
                  {{ selectedSpan.serviceName }}
                </span>
              </div>
              <div class="summary-item">
                <span class="summary-label">Duration</span>
                <span class="summary-value">{{ formatDuration(selectedSpan.durationNanos) }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">Status</span>
                <span class="summary-value">
                  @if (isError(selectedSpan)) {
                    <span class="status-dot error"></span> ERROR
                  } @else {
                    <span class="status-dot ok"></span> OK
                  }
                </span>
              </div>
              <div class="summary-item">
                <span class="summary-label">Span ID</span>
                <span class="summary-value mono">{{ selectedSpan.spanId }}</span>
              </div>
            </div>

            @if (selectedSpanAttributes.length > 0) {
              <div class="attributes-section">
                <div class="attributes-title">ATTRIBUTES</div>
                <table class="attributes-table">
                  <tbody>
                    @for (attr of selectedSpanAttributes; track attr.key) {
                      <tr>
                        <td class="attr-key">{{ attr.key }}</td>
                        <td class="attr-value">{{ attr.value }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; flex: 1; overflow: hidden; }

    .timeline-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--gio-color-background, #f7f7f8);
    }

    .timeline-header {
      display: flex;
      border-bottom: 1px solid var(--gio-color-border, #e3e3e3);
      background: #ffffff;
      position: sticky;
      top: 0;
      z-index: 5;
      box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    }
    .header-label {
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--gio-color-text-secondary, #5c5959);
      padding: 10px 16px;
      display: flex;
      align-items: center;
    }

    .span-info-col {
      width: 320px;
      min-width: 320px;
      flex-shrink: 0;
    }
    .time-col {
      flex: 1;
      position: relative;
      min-width: 200px;
    }

    .time-ruler {
      position: relative;
      height: 100%;
      padding: 10px 0;
    }
    .tick {
      position: absolute;
      top: 0;
      bottom: 0;
      border-left: 1px dashed var(--gio-color-border, #e3e3e3);
    }
    .tick-label {
      position: absolute;
      top: 10px;
      left: 4px;
      font-size: 10px;
      color: var(--gio-color-text-secondary, #5c5959);
      white-space: nowrap;
    }

    .timeline-body {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
    }

    .span-row {
      display: flex;
      align-items: center;
      height: 32px;
      border-bottom: 1px solid var(--gio-color-border, #e3e3e3);
      cursor: pointer;
      transition: background 0.1s;
    }
    .span-row:nth-child(even) { background: rgba(0,0,0,0.015); }
    .span-row:hover { background: var(--gio-color-background, #f7f7f8); }
    .span-row.selected { background: rgba(0,153,153,0.06); }

    .span-label {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 0 12px;
      overflow: hidden;
    }

    .toggle-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 20px;
      height: 20px;
      border: none;
      border-radius: 3px;
      background: transparent;
      cursor: pointer;
      color: var(--gio-color-text-secondary, #5c5959);
      padding: 0;
      flex-shrink: 0;
    }
    .toggle-btn:hover {
      background: rgba(0,0,0,0.08);
      color: var(--gio-color-text, #1e1b1b);
    }
    .toggle-btn .material-icons { font-size: 16px; }
    .toggle-spacer { width: 20px; flex-shrink: 0; }

    .service-badge {
      display: inline-block;
      padding: 1px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      color: #ffffff;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .operation-name {
      font-size: 12px;
      color: var(--gio-color-text, #1e1b1b);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .bar-track {
      position: relative;
      height: 100%;
      padding: 8px 0;
    }
    .bar {
      position: absolute;
      top: 8px;
      height: 16px;
      border-radius: 3px;
      opacity: 0.85;
      transition: opacity 0.15s;
    }
    .span-row:hover .bar { opacity: 1; }
    .bar-error { opacity: 1; box-shadow: 0 0 0 1px rgba(220, 38, 38, 0.5); }
    .bar-error-mark {
      position: absolute;
      top: 4px;
      right: 4px;
      font-size: 16px;
      color: #dc2626;
      pointer-events: none;
    }

    .detail-panel {
      border-top: 2px solid var(--gio-color-primary, #da3b00);
      background: #ffffff;
      max-height: 340px;
      overflow-y: auto;
      flex-shrink: 0;
      box-shadow: 0 -2px 6px rgba(0,0,0,0.06);
    }
    .detail-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 16px;
      border-bottom: 1px solid var(--gio-color-border, #e3e3e3);
    }
    .detail-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--gio-color-text, #1e1b1b);
    }
    .detail-title .material-icons { font-size: 16px; color: var(--gio-color-primary, #da3b00); }
    .close-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px; height: 24px;
      border: none;
      border-radius: 4px;
      background: transparent;
      cursor: pointer;
      color: var(--gio-color-text-secondary, #5c5959);
    }
    .close-btn:hover { background: var(--gio-color-background, #f7f7f8); color: var(--gio-color-text, #1e1b1b); }
    .close-btn .material-icons { font-size: 16px; }

    .detail-body { padding: 12px 16px; }

    .detail-summary {
      display: flex;
      gap: 24px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .summary-item {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .summary-label {
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--gio-color-text-secondary, #5c5959);
    }
    .summary-value {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 500;
      color: var(--gio-color-text, #1e1b1b);
    }
    .summary-value.mono {
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
    }
    .service-dot {
      width: 8px; height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .status-dot {
      width: 8px; height: 8px;
      border-radius: 50%;
    }
    .status-dot.ok { background: var(--gio-color-success, #0de598); }
    .status-dot.error { background: var(--gio-color-error, #dd1d1f); }

    .attributes-section { margin-top: 4px; }
    .attributes-title {
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--gio-color-text-secondary, #5c5959);
      margin-bottom: 6px;
    }
    .attributes-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;
    }
    .attributes-table tr { border-bottom: 1px solid var(--gio-color-border, #e3e3e3); }
    .attributes-table td { padding: 5px 10px; }
    .attr-key {
      color: var(--gio-color-text-secondary, #5c5959);
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
      width: 260px;
      white-space: nowrap;
    }
    .attr-value {
      color: var(--gio-color-text, #1e1b1b);
      word-break: break-all;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 60px 20px;
      color: var(--gio-color-text-secondary, #5c5959);
      font-size: 14px;
    }
    .empty-state .material-icons { font-size: 36px; }
  `]
})
export class TimelineViewComponent implements OnInit {
  @Input() trace!: Trace;

  flatSpans: FlatSpan[] = [];
  timeTicks: { percent: number; label: string }[] = [];
  selectedSpan: TraceSpan | null = null;
  selectedSpanId: string | null = null;
  selectedSpanAttributes: { key: string; value: string }[] = [];

  private traceStartNanos = 0;
  private traceDurationNanos = 0;
  private childrenMap = new Map<string, TraceSpan[]>();
  private collapsedSet = new Set<string>();

  mathMax = Math.max;

  ngOnInit(): void {
    this.buildTimeline();
  }

  private buildTimeline(): void {
    const spans = this.trace?.spans;
    if (!spans || spans.length === 0) return;

    const allSpans: TraceSpan[] = [];
    this.collectSpans(spans, allSpans);

    if (allSpans.length === 0) return;

    const startTimes = allSpans.map(s => this.parseStartTime(s.startTime));
    const endTimes = allSpans.map((s, i) => startTimes[i] + s.durationNanos);
    this.traceStartNanos = Math.min(...startTimes);
    const traceEndNanos = Math.max(...endTimes);
    this.traceDurationNanos = traceEndNanos - this.traceStartNanos;

    if (this.traceDurationNanos <= 0) {
      this.traceDurationNanos = 1;
    }

    const spanMap = new Map<string, TraceSpan>();
    const roots: TraceSpan[] = [];
    for (const span of allSpans) {
      spanMap.set(span.spanId, span);
    }
    for (const span of allSpans) {
      if (!span.parentSpanId || !spanMap.has(span.parentSpanId)) {
        roots.push(span);
      }
    }

    this.childrenMap = new Map<string, TraceSpan[]>();
    for (const span of allSpans) {
      if (span.parentSpanId && spanMap.has(span.parentSpanId)) {
        const children = this.childrenMap.get(span.parentSpanId) || [];
        children.push(span);
        this.childrenMap.set(span.parentSpanId, children);
      }
    }

    for (const children of this.childrenMap.values()) {
      children.sort((a, b) => this.parseStartTime(a.startTime) - this.parseStartTime(b.startTime));
    }

    roots.sort((a, b) => this.parseStartTime(a.startTime) - this.parseStartTime(b.startTime));

    this.flatSpans = [];
    const flatten = (span: TraceSpan, depth: number, parentHidden: boolean) => {
      const startNanos = this.parseStartTime(span.startTime);
      const offsetPercent = ((startNanos - this.traceStartNanos) / this.traceDurationNanos) * 100;
      const widthPercent = (span.durationNanos / this.traceDurationNanos) * 100;
      const children = this.childrenMap.get(span.spanId) || [];
      const hasChildren = children.length > 0;
      const collapsed = this.collapsedSet.has(span.spanId);
      const spanType = classifySpan(span);
      this.flatSpans.push({ span, depth, offsetPercent, widthPercent, hasChildren, collapsed, hidden: parentHidden, spanType });
      const childHidden = parentHidden || collapsed;
      for (const child of children) {
        flatten(child, depth + 1, childHidden);
      }
    };
    for (const root of roots) {
      flatten(root, 0, false);
    }

    this.buildTimeTicks();
  }

  toggleCollapse(item: FlatSpan): void {
    if (this.collapsedSet.has(item.span.spanId)) {
      this.collapsedSet.delete(item.span.spanId);
    } else {
      this.collapsedSet.add(item.span.spanId);
    }
    // Rebuild visibility
    this.rebuildVisibility();
  }

  private rebuildVisibility(): void {
    const isAncestorCollapsed = (spanId: string): boolean => {
      // Walk up from this span and check if any ancestor is collapsed
      for (const item of this.flatSpans) {
        if (item.span.spanId === spanId) {
          // found the item, now check parents
          break;
        }
      }
      return false;
    };

    // Simpler approach: walk the flatSpans and track collapsed ancestors by depth
    const collapsedAtDepth: (string | null)[] = [];
    for (const item of this.flatSpans) {
      // Trim collapsed stack to current depth
      while (collapsedAtDepth.length > item.depth) {
        collapsedAtDepth.pop();
      }

      item.hidden = collapsedAtDepth.length > 0 && collapsedAtDepth.some(id => id !== null);
      item.collapsed = this.collapsedSet.has(item.span.spanId);

      if (item.collapsed) {
        collapsedAtDepth.push(item.span.spanId);
      } else {
        collapsedAtDepth.push(null);
      }
    }
  }

  private collectSpans(spans: TraceSpan[], out: TraceSpan[]): void {
    for (const span of spans) {
      out.push(span);
      if (span.children && span.children.length > 0) {
        this.collectSpans(span.children, out);
      }
    }
  }

  private parseStartTime(startTime: string | number): number {
    if (typeof startTime === 'number') {
      // Jackson Instant as epoch seconds → nanoseconds
      return startTime * 1_000_000_000;
    }
    const d = new Date(startTime);
    if (!isNaN(d.getTime())) return d.getTime() * 1_000_000;
    const num = parseFloat(startTime);
    if (!isNaN(num)) return num * 1_000_000_000;
    return 0;
  }

  private buildTimeTicks(): void {
    const durationMs = this.traceDurationNanos / 1_000_000;
    const tickCount = 6;
    this.timeTicks = [];
    for (let i = 0; i <= tickCount; i++) {
      const percent = (i / tickCount) * 100;
      const ms = (i / tickCount) * durationMs;
      this.timeTicks.push({ percent, label: this.formatMs(ms) });
    }
  }

  private formatMs(ms: number): string {
    if (ms === 0) return '0ms';
    if (ms < 1000) return `${Math.round(ms)}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  }

  getSpanColor(spanType: string): string {
    return getSpanColor(spanType);
  }

  getSpanLabel(item: FlatSpan): string {
    const entityColor = ENTITY_TYPE_COLORS[item.spanType];
    if (entityColor) return entityColor.label;
    return item.span.serviceName;
  }

  getSelectedSpanColor(): string {
    if (!this.selectedSpan) return '#999';
    return getSpanColor(classifySpan(this.selectedSpan));
  }

  truncate(str: string, len: number): string {
    return str.length > len ? str.substring(0, len - 1) + '...' : str;
  }

  formatDuration(nanos: number): string {
    const ms = nanos / 1_000_000;
    if (ms < 1000) return `${ms.toFixed(0)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }

  isError(span: TraceSpan | null): boolean {
    const code = span?.attributes?.['otel.status_code'];
    return code === 'ERROR' || code === 'STATUS_CODE_ERROR' || code === '2';
  }

  selectSpan(item: FlatSpan): void {
    this.selectedSpan = item.span;
    this.selectedSpanId = item.span.spanId;
    this.selectedSpanAttributes = Object.entries(item.span.attributes || {})
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => a.key.localeCompare(b.key));
  }

  closeDetail(): void {
    this.selectedSpan = null;
    this.selectedSpanId = null;
    this.selectedSpanAttributes = [];
  }
}
