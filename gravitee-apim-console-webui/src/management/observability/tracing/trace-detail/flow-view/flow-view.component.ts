/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, Inject, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Trace, TraceEvent, TraceSpan, TracingGraph } from '../../tracing.model';
import { Constants } from '../../../../../entities/Constants';

interface PolicyPlugin {
  id: string;
  name: string;
  icon?: string;
}

type FlowCardType = 'client' | 'request' | 'security' | 'policy' | 'backend' | 'agent' | 'llm' | 'mcp' | 'response';

interface FlowCard {
  id: string;
  type: FlowCardType;
  label: string;
  subtitle: string;
  durationMs: number | null;
  span: TraceSpan | null;
  icon: string;
  iconUrl?: string;
  statusCode?: number;
  httpMethod?: string;
  httpUrl?: string;
  hasError?: boolean;
  errorMessage?: string;
}

interface DiffRow {
  key: string;
  before: string | null;
  after: string | null;
  changed: 'unchanged' | 'added' | 'removed' | 'modified';
}

type FlowItem =
  | { kind: 'card'; card: FlowCard }
  | { kind: 'group'; flowName: string; cards: FlowCard[] }
  | { kind: 'stretch'; direction: 'forward' | 'back' }
  | { kind: 'loopback-horizontal' };

interface FlowLine {
  items: FlowItem[];
  isResponse: boolean;
}

const CARD_COLORS: Record<FlowCardType, { bg: string; border: string; icon: string }> = {
  client:   { bg: '#f1f5f9', border: '#64748b', icon: 'person' },
  request:  { bg: '#e6faf2', border: '#0de598', icon: 'play_arrow' },
  security: { bg: '#fef3c7', border: '#f59e0b', icon: 'lock' },
  policy:   { bg: '#dbeafe', border: '#3b82f6', icon: 'policy' },
  backend:  { bg: '#eef2ff', border: '#6366f1', icon: 'cloud' },
  agent:    { bg: '#d6ffff', border: '#009999', icon: 'smart_toy' },
  llm:      { bg: '#e7e2fb', border: '#876fec', icon: 'psychology' },
  mcp:      { bg: '#e8e8ec', border: '#494b61', icon: 'dns' },
  response: { bg: '#e6faf2', border: '#0de598', icon: 'check_circle' },
};

function isAgentSpan(span: TraceSpan): boolean {
  return span.attributes['gen_ai.operation.name'] === 'invoke_agent'
      || span.operationName.startsWith('invoke_agent ');
}

function isMcpSpan(span: TraceSpan): boolean {
  return !!span.attributes['mcp.method.name']
      || (!!span.attributes['gen_ai.tool.name']
          && span.attributes['gen_ai.operation.name'] === 'execute_tool');
}

function isLlmSpan(span: TraceSpan): boolean {
  const op = span.attributes['gen_ai.operation.name'];
  return ['chat', 'generate_content', 'text_completion', 'stream_generate_content'].includes(op);
}

function isSecuritySpan(span: TraceSpan): boolean {
  return span.attributes['Security'] !== undefined || span.attributes['security'] !== undefined;
}

function isPolicySpan(span: TraceSpan): boolean {
  return span.attributes['gravitee.policy'] !== undefined;
}

function isBackendSpan(span: TraceSpan): boolean {
  // Gravitee tags the invoker span with an "Invoker" attribute (e.g. "endpoint-invoker") — it wraps the actual backend HTTP call.
  return span.attributes['Invoker'] !== undefined;
}

function getPhase(span: TraceSpan): 'request' | 'response' | null {
  const phase = span.attributes['gravitee.execution.phase'];
  if (phase === 'request' || phase === 'response') return phase;
  return null;
}

@Component({
  selector: 'app-flow-view',
  standalone: true,
  imports: [CommonModule, MatTooltipModule],
  template: `
    <div class="flow-container">
      <!-- Chain (top) -->
      <div class="flow-chain">
        <div class="chain-label">FLOW CHAIN</div>
        <div class="chain-body" [class.chain-body--looped]="lines.length > 1">
          @if (lines.length > 1) {
            <div class="loopback-vertical"></div>
            <span class="material-icons loopback-arrowhead">arrow_upward</span>
          }
          @for (line of lines; track $index) {
            @let arrowIcon = line.isResponse ? 'arrow_back' : 'arrow_forward';
            <div class="chain-line" [class.chain-line-response]="line.isResponse">
              @for (item of line.items; track itemKey(item); let i = $index) {
                @switch (item.kind) {
                  @case ('card') {
                    <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: item.card }"></ng-container>
                  }
                  @case ('group') {
                    <div class="flow-group">
                      <div class="group-label">
                        <span class="material-icons">category</span>
                        Flow · {{ formatFlowName(item.flowName) }}
                      </div>
                      <div class="group-cards">
                        @for (card of item.cards; track card.id; let j = $index) {
                          <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: card }"></ng-container>
                          @if (j < item.cards.length - 1) {
                            <div class="arrow">
                              <span class="material-icons">{{ arrowIcon }}</span>
                            </div>
                          }
                        }
                      </div>
                    </div>
                  }
                  @case ('stretch') {
                    <div class="stretch-arrow">
                      <span class="material-icons stretch-head">arrow_downward</span>
                    </div>
                  }
                  @case ('loopback-horizontal') {
                    <div class="loopback-horizontal"></div>
                  }
                }
                @if (shouldShowArrow(line, i)) {
                  <div class="arrow">
                    <span class="material-icons">{{ arrowIcon }}</span>
                  </div>
                }
              }
            </div>
          }
        </div>
      </div>

      <ng-template #cardTpl let-card>
        <div class="flow-card"
             [class.selected]="selectedCardId === card.id"
             [class.flow-card-iconified]="!!card.iconUrl"
             [class.flow-card-error]="card.hasError"
             [style.border-top-color]="getCardColor(card.type).border"
             [matTooltip]="cardTooltip(card)"
             matTooltipPosition="above"
             (click)="selectCard(card)">
          @if (card.hasError) {
            <span class="card-error-mark material-icons">error</span>
          }
          @if (card.iconUrl) {
            <div class="card-icon-wrap">
              <img class="card-icon-img" [src]="card.iconUrl" [alt]="card.label" />
            </div>
            <div class="card-label card-label-center">{{ card.label }}</div>
          } @else {
            <div class="card-type-badge" [style.background]="getCardColor(card.type).border">
              <span class="material-icons">{{ card.icon }}</span>
              <span>{{ card.type | uppercase }}</span>
            </div>
            <div class="card-label">{{ card.label }}</div>
            <div class="card-subtitle">{{ card.subtitle }}</div>
          }
          <div class="card-footer">
            @if (card.durationMs !== null) {
              <span class="card-duration">{{ formatMs(card.durationMs) }}</span>
            }
            @if (card.statusCode !== undefined) {
              <span class="card-status" [class]="'card-status-' + statusClass(card.statusCode)">
                {{ card.statusCode }}
              </span>
            }
          </div>
        </div>
      </ng-template>

      <!-- Detail panel (bottom) -->
      <div class="flow-detail">
        @if (selectedCard && selectedCard.span) {
          <div class="detail-header">
            <span class="detail-icon material-icons" [style.color]="getCardColor(selectedCard.type).border">
              {{ selectedCard.icon }}
            </span>
            <div class="detail-title-block">
              <div class="detail-title">
                {{ selectedCard.type | uppercase }} &mdash; {{ selectedCard.span.operationName }}
              </div>
              @if (selectedCard.span.attributes['gravitee.policy.description']; as description) {
                <div class="detail-description">{{ description }}</div>
              }
            </div>
          </div>

          @if (selectedCard.hasError) {
            <div class="detail-error">
              <div class="detail-error-header">
                <span class="material-icons">error</span>
                <span>
                  Error &mdash; {{ exceptionInfo(selectedCard.span).type || 'Span reported ERROR status' }}
                </span>
              </div>
              @if (exceptionInfo(selectedCard.span).message; as message) {
                <div class="detail-error-message">{{ message }}</div>
              }
              @if (exceptionInfo(selectedCard.span).stacktrace; as stacktrace) {
                <details class="detail-error-stacktrace">
                  <summary>Stacktrace</summary>
                  <pre>{{ stacktrace }}</pre>
                </details>
              }
            </div>
          }

          <div class="detail-metrics">
            <div class="metric-card">
              <div class="metric-label">Duration</div>
              <div class="metric-value">{{ formatDuration(selectedCard.span.durationNanos) }}</div>
            </div>
            <div class="metric-card">
              <div class="metric-label">Service</div>
              <div class="metric-value">{{ selectedCard.span.serviceName }}</div>
            </div>
            @if (selectedCard.httpMethod) {
              <div class="metric-card">
                <div class="metric-label">Method</div>
                <div class="metric-value">{{ selectedCard.httpMethod }}</div>
              </div>
            }
            @if (selectedCard.httpUrl) {
              <div class="metric-card metric-card-url">
                <div class="metric-label">URL</div>
                <div class="metric-value metric-value-url" [title]="selectedCard.httpUrl">{{ selectedCard.httpUrl }}</div>
              </div>
            }
            @if (getTokens(selectedCard.span); as tokens) {
              <div class="metric-card">
                <div class="metric-label">Tokens</div>
                <div class="metric-value">{{ tokens }} total</div>
              </div>
            }
          </div>

          <!-- Policy event diff (pre/post) -->
          @if (diffRows.length > 0) {
            <div class="events-section">
              <div class="events-title">
                VERBOSE TRACING &mdash; attributes before vs. after policy execution
              </div>
              <table class="diff-table">
                <thead>
                  <tr>
                    <th class="diff-key-col">Attribute</th>
                    <th class="diff-col">Before (pre)</th>
                    <th class="diff-col">After (post)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (row of diffRows; track row.key) {
                    <tr [class]="'row-' + row.changed">
                      <td class="attr-key">{{ row.key }}</td>
                      <td class="diff-cell">
                        <div class="diff-cell-inner">
                          <span class="diff-value">{{ row.before ?? '—' }}</span>
                        </div>
                      </td>
                      <td class="diff-cell">
                        <div class="diff-cell-inner">
                          <span class="diff-value">{{ row.after ?? '—' }}</span>
                          @if (row.changed !== 'unchanged') {
                            <span class="diff-badge" [class]="'badge-' + row.changed">
                              {{ row.changed | uppercase }}
                            </span>
                          }
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else if (otherEvents.length > 0) {
            <!-- Non-pre/post events: list view -->
            <div class="events-section">
              <div class="events-title">SPAN EVENTS</div>
              @for (event of otherEvents; track event.name + event.time) {
                <div class="event-block">
                  <div class="event-header">{{ event.name }}</div>
                  <table class="attributes-table">
                    <tbody>
                      @for (attr of eventAttributes(event); track attr.key) {
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
          }

          <!-- Span attributes -->
          @if (selectedAttributes.length > 0) {
            <div class="attributes-section">
              <div class="attributes-title">SPAN ATTRIBUTES</div>
              <table class="attributes-table">
                <tbody>
                  @for (attr of selectedAttributes; track attr.key) {
                    <tr>
                      <td class="attr-key">{{ attr.key }}</td>
                      <td class="attr-value">{{ attr.value }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        } @else if (selectedCard) {
          <div class="detail-placeholder">
            <span class="material-icons" [style.color]="getCardColor(selectedCard.type).border">{{ selectedCard.icon }}</span>
            <div>No additional details for the {{ selectedCard.label }} card.</div>
          </div>
        } @else {
          <div class="detail-placeholder">
            <span class="material-icons">touch_app</span>
            <div>Select a card to view span details</div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; flex: 1; overflow: hidden; }

    .flow-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--gio-color-background, #f7f7f8);
    }

    .flow-chain {
      flex: 0 0 auto;
      max-height: 45%;
      padding: 16px 24px;
      overflow-x: auto;
      overflow-y: auto;
      border-bottom: 2px solid var(--gio-color-border, #e3e3e3);
      background: #ffffff;
    }
    .chain-label {
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--gio-color-text-secondary, #5c5959);
      margin-bottom: 16px;
    }

    /*
     * Two-line layout:
     *   [Client] → [request cards] ───────→ ↓
     *                     [Response] ← [response cards] ← [Backend]
     * Line 1 is left-aligned; line 2 is right-aligned so Backend sits directly below line 1's trailing arrow.
     * Line 2 uses arrow_back icons to show the RTL response flow while the DOM remains LTR.
     */
    .chain-body {
      position: relative;
      min-width: min-content;
    }
    .chain-body--looped {
      /* No extra padding: the loopback's vertical segment now sits under the Client card, not to its left. */
    }

    /*
     * Loopback path: Response → LEFT → UP → Client (from below).
     * Visually the L is composed of:
     *   - loopback-horizontal : flex:1 item at the left of line 2, dashed mid-line extending left to the Client's x-center
     *   - loopback-vertical   : absolute dashed line under the Client card, from just-below Client down to line 2's middle
     *   - loopback-arrowhead  : material-icon arrow_upward positioned below the Client, tip touching the Client's bottom edge
     * Card height 95px, width 130px (Client center x=65), chain-line margin-bottom 40px.
     * Line 1 middle = y:47, Line 2 top = y:135, Line 2 middle = y:182.
     */
    .loopback-vertical {
      position: absolute;
      left: 64px;         /* Client center x (card width 130 / 2) */
      top: 187px;         /* below the arrowhead icon, in the gap between lines */
      height: 109px;      /* reaches line 2 middle at chain-body y=296 (flow-group 158 + gap 40 + 38 card-top-offset + 60 card-middle) */
      border-left: 2px dashed #94a3b8;
      pointer-events: none;
    }
    .loopback-arrowhead {
      position: absolute;
      left: 54px;         /* 22px icon centered on x=65 */
      top: 165px;         /* tip 7px below Client's bottom (y=158); icon spans 165→187, meeting the vertical line's top */
      font-size: 22px;
      color: #94a3b8;
      pointer-events: none;
    }
    .loopback-horizontal {
      flex: 1;
      min-width: 60px;
      height: 120px;
      position: relative;
    }
    .loopback-horizontal::before {
      content: '';
      position: absolute;
      left: 64px;         /* meet the vertical line under the Client */
      right: 4px;         /* stop just before Response's left edge */
      top: 60px;          /* horizontal dashed at card middle (half of 120) */
      border-top: 2px dashed #94a3b8;
    }

    .chain-line {
      display: flex;
      flex-wrap: nowrap;
      gap: 0;
      align-items: flex-end;
      margin-bottom: 40px; /* room between lines for the vertical arrow stubs */
      min-width: min-content;
      justify-content: flex-start;
    }
    .chain-line:last-child { margin-bottom: 0; }

    /* Response line right-aligned: Backend ends up directly under line 1's trailing down-arrow. */
    .chain-line-response {
      justify-content: flex-end;
    }

    /*
     * Stretch arrow on line 1: horizontal dashed line filling remaining space, then a short vertical stub
     * dropping into the gap, ending with arrow_downward pointing into line 2.
     */
    .stretch-arrow {
      flex: 1;
      min-width: 60px;
      height: 120px;
      position: relative;
      color: #94a3b8;
    }
    .stretch-arrow::before {
      /* horizontal dashed line along line 1's middle (half of 120) */
      content: '';
      position: absolute;
      left: 8px;
      right: 15px;
      top: 60px;
      border-top: 2px dashed currentColor;
    }
    .stretch-arrow::after {
      /* short vertical stub descending past line 1 into the gap */
      content: '';
      position: absolute;
      right: 15px;
      top: 60px;
      height: 78px;       /* y=60 → y=138, meeting the arrow icon below */
      border-left: 2px dashed currentColor;
    }
    .stretch-head {
      position: absolute;
      right: 4px;         /* 22px icon centered on the vertical stub (at right ≈ 16) */
      top: 138px;         /* tip (bottom of arrow_downward) at y=160 within stretch-arrow → chain-body y=198, touching line 2's top */
      font-size: 22px;
      line-height: 1;
    }

    .flow-group {
      position: relative;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      border: 2px dashed #94a3b8;
      border-radius: 12px;
      padding: 28px 14px 10px;
      margin: 0 6px;
      background: rgba(148, 163, 184, 0.06);
    }
    .group-label {
      position: absolute;
      top: -12px;
      left: 16px;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 12px;
      background: #ffffff;
      border: 1.5px solid #94a3b8;
      border-radius: 14px;
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: #334155;
      white-space: nowrap;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    }
    .group-label .material-icons { font-size: 12px; }
    .group-cards {
      display: flex;
      align-items: flex-end;
    }

    .flow-card {
      width: 130px;
      height: 120px;
      flex: 0 0 auto;
      display: flex;
      flex-direction: column;
      background: #ffffff;
      border: 1px solid var(--gio-color-border, #e3e3e3);
      border-top: 3px solid;
      border-radius: 10px;
      padding: 8px 10px;
      cursor: pointer;
      transition: all 0.15s;
      box-shadow: 0 1px 4px rgba(0,0,0,0.04);
      box-sizing: border-box;
      overflow: hidden;
    }
    .flow-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
      transform: translateY(-2px);
    }
    .flow-card.selected {
      border-color: #009999;
      box-shadow: 0 0 0 2px rgba(0,153,153,0.2), 0 4px 12px rgba(0,0,0,0.08);
    }
    .flow-card-error {
      position: relative;
      border-color: #dc2626;
      box-shadow: 0 0 0 1px rgba(220, 38, 38, 0.35), 0 1px 4px rgba(0,0,0,0.04);
    }
    .flow-card-error.selected {
      box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.4), 0 4px 12px rgba(0,0,0,0.08);
    }
    .card-error-mark {
      position: absolute;
      top: 4px;
      right: 4px;
      font-size: 16px;
      color: #dc2626;
      pointer-events: none;
    }

    .card-type-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 8px;
      border-radius: 8px;
      font-size: 9px;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 6px;
    }
    .card-type-badge .material-icons { font-size: 12px; }

    .card-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--gio-color-text, #1e1b1b);
      margin-bottom: 2px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .card-icon-wrap {
      display: flex;
      justify-content: center;
      align-items: center;
    }
    .card-icon-img {
      width: 56px;
      height: 56px;
      object-fit: contain;
    }
    .card-label-center {
      margin: 0;
      text-align: center;
      font-size: 11px;
      line-height: 1.2;
      font-weight: 600;
      color: var(--gio-color-text, #1e1b1b);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .flow-card-iconified {
      padding: 4px 8px;
    }
    .card-subtitle {
      font-size: 10px;
      color: var(--gio-color-text-secondary, #5c5959);
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 6px;
      margin-top: auto;
    }
    .card-duration {
      font-size: 11px;
      font-weight: 600;
      color: var(--gio-color-text-secondary, #5c5959);
    }
    .card-status {
      font-size: 10px;
      font-weight: 700;
      padding: 1px 6px;
      border-radius: 6px;
      line-height: 1.4;
    }
    .card-status-success { color: #065f46; background: #d1fae5; }
    .card-status-redirect { color: #1e40af; background: #dbeafe; }
    .card-status-client-error { color: #9a3412; background: #ffedd5; }
    .card-status-server-error { color: #991b1b; background: #fee2e2; }
    .card-status-unknown { color: #334155; background: #e2e8f0; }

    .arrow {
      display: flex;
      align-items: center;
      padding: 0 4px;
      color: #999;
      height: 120px;      /* matches flow-card height so chevrons sit at the card's vertical middle */
    }
    .arrow .material-icons { font-size: 20px; }

    /* Detail panel at bottom */
    .flow-detail {
      flex: 1 1 auto;
      min-height: 0;
      overflow-y: auto;
      padding: 16px 24px;
      background: #ffffff;
    }

    .detail-header {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      margin-bottom: 16px;
    }
    .detail-icon { font-size: 22px; line-height: 1.3; }
    .detail-title-block {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .detail-title {
      font-size: 14px;
      font-weight: 700;
      color: var(--gio-color-text, #1e1b1b);
    }
    .detail-description {
      font-size: 12px;
      color: var(--gio-color-text-secondary, #5c5959);
      font-style: italic;
    }

    .detail-metrics {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      margin-bottom: 20px;
    }
    .metric-card {
      padding: 8px 14px;
      background: var(--gio-color-background, #f7f7f8);
      border: 1px solid var(--gio-color-border, #e3e3e3);
      border-radius: 8px;
      min-width: 100px;
    }
    .metric-label {
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--gio-color-text-secondary, #5c5959);
      margin-bottom: 2px;
    }
    .metric-value {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: var(--gio-color-text, #1e1b1b);
    }
    .metric-card-url {
      flex: 1 1 320px;
      min-width: 220px;
      max-width: 100%;
    }

    .detail-error {
      border: 1px solid #fecaca;
      border-radius: 8px;
      background: #fef2f2;
      padding: 10px 14px;
      margin-bottom: 16px;
    }
    .detail-error-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 700;
      color: #991b1b;
      font-size: 12px;
    }
    .detail-error-header .material-icons { font-size: 16px; }
    .detail-error-message {
      margin-top: 6px;
      font-size: 12px;
      color: #7f1d1d;
      word-break: break-word;
    }
    .detail-error-stacktrace {
      margin-top: 8px;
      font-size: 11px;
    }
    .detail-error-stacktrace summary {
      cursor: pointer;
      color: #991b1b;
      font-weight: 600;
    }
    .detail-error-stacktrace pre {
      margin: 6px 0 0;
      max-height: 220px;
      overflow: auto;
      background: #ffffff;
      border: 1px solid #fecaca;
      border-radius: 6px;
      padding: 8px;
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
      white-space: pre-wrap;
      word-break: break-all;
    }
    .metric-value-url {
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 12px;
      word-break: break-all;
    }

    .events-section { margin-bottom: 24px; }
    .events-title {
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--gio-color-text-secondary, #5c5959);
      margin-bottom: 8px;
    }

    .event-block { margin-bottom: 12px; }
    .event-header {
      font-size: 12px;
      font-weight: 600;
      color: var(--gio-color-text, #1e1b1b);
      padding: 4px 8px;
      background: #f0f2f5;
      border-radius: 4px;
      margin-bottom: 4px;
    }

    /* Pre/post diff table */
    .diff-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;
      border: 1px solid var(--gio-color-border, #e3e3e3);
      border-radius: 6px;
      overflow: hidden;
    }
    .diff-table thead {
      background: #f0f2f5;
    }
    .diff-table th {
      text-align: left;
      padding: 8px 12px;
      font-size: 11px;
      font-weight: 700;
      color: var(--gio-color-text-secondary, #5c5959);
      text-transform: uppercase;
      letter-spacing: 0.06em;
      border-bottom: 1px solid var(--gio-color-border, #e3e3e3);
    }
    .diff-key-col { width: 240px; }
    .diff-col { width: auto; }
    .diff-table {
      table-layout: fixed;
    }
    .diff-table tbody tr {
      border-bottom: 1px solid var(--gio-color-border, #eee);
    }
    .diff-table tbody tr:last-child { border-bottom: none; }
    .diff-table td {
      padding: 6px 12px;
      vertical-align: top;
    }
    .diff-cell-inner {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 8px;
      word-break: break-all;
    }
    .diff-value {
      flex: 1;
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
    }
    .diff-badge {
      flex: 0 0 auto;
      padding: 1px 6px;
      border-radius: 10px;
      font-size: 9px;
      font-weight: 700;
      letter-spacing: 0.04em;
      color: #ffffff;
    }
    .badge-added { background: #0de598; color: #0a6b4a; }
    .badge-removed { background: #fecaca; color: #b91c1c; }
    .badge-modified { background: #bfdbfe; color: #1e40af; }

    .row-modified { background: #fffbea; }
    .row-added { background: #f0fdf4; }
    .row-removed { background: #fef2f2; }

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
    .attributes-table td { padding: 5px 8px; }
    .attr-key {
      color: var(--gio-color-text-secondary, #5c5959);
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
      width: 240px;
      overflow-wrap: anywhere;
    }
    .attr-value {
      color: var(--gio-color-text, #1e1b1b);
      word-break: break-all;
      font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 11px;
    }

    .detail-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 60px 20px;
      color: var(--gio-color-text-secondary, #5c5959);
      font-size: 14px;
    }
    .detail-placeholder .material-icons { font-size: 32px; }
  `]
})
export class FlowViewComponent implements OnInit {
  @Input() trace!: Trace;
  @Input() tracing!: TracingGraph;

  cards: FlowCard[] = [];
  lines: FlowLine[] = [];
  selectedCard: FlowCard | null = null;
  selectedCardId: string | null = null;
  selectedAttributes: { key: string; value: string }[] = [];
  diffRows: DiffRow[] = [];
  otherEvents: TraceEvent[] = [];

  private spanById = new Map<string, TraceSpan>();
  private policyById = new Map<string, PolicyPlugin>();

  private readonly http = inject(HttpClient);
  constructor(@Inject(Constants) private readonly constants: Constants) {}

  ngOnInit(): void {
    // Policy catalogue powers per-card icons + hover tooltips; the chain renders first and is enriched once the list arrives.
    this.http
      .get<PolicyPlugin[]>(`${this.constants.org.v2BaseURL}/plugins/policies`, {
        params: new HttpParams().append('expand', 'icon'),
      })
      .subscribe({
        next: policies => {
          this.policyById.clear();
          for (const p of policies) {
            this.policyById.set(p.id, p);
          }
          this.buildFlowChain();
        },
        error: () => this.buildFlowChain(),
      });
  }

  itemKey(item: FlowItem): string {
    switch (item.kind) {
      case 'card':
        return `c:${item.card.id}`;
      case 'group':
        return `g:${item.flowName}:${item.cards.map(c => c.id).join(',')}`;
      case 'stretch':
        return `s:${item.direction}`;
      case 'loopback-horizontal':
        return 'lb-h';
    }
  }

  shouldShowArrow(line: FlowLine, i: number): boolean {
    if (i >= line.items.length - 1) return false;
    const current = line.items[i];
    const next = line.items[i + 1];
    // Chevrons suppressed around stretch arrows and loopback fillers — they carry their own visual.
    const isDecor = (it: FlowItem) => it.kind === 'stretch' || it.kind === 'loopback-horizontal';
    return !isDecor(current) && !isDecor(next);
  }

  private buildFlowChain(): void {
    const spans = this.trace?.spans;
    if (!spans || spans.length === 0) return;

    const allSpans: TraceSpan[] = [];
    this.collectSpans(spans, allSpans);

    this.spanById.clear();
    for (const s of allSpans) {
      this.spanById.set(s.spanId, s);
    }

    const rootSpan = allSpans.find(s => isAgentSpan(s)) || allSpans[0];
    // Inbound HTTP SERVER span: has http.method, NOT an outbound client call (no http.url), NOT the invoker wrapper, and ideally
    // carries server-side markers like client.address / net.host.*. We then sort those candidates by startTime and pick the EARLIEST
    // one — the true root wraps every child and therefore starts first. That defends against traces where Tempo returns multiple
    // candidate "server-shaped" spans (e.g. from the callout policy's downstream chain).
    const isHttpServerSpan = (s: TraceSpan) =>
      !!s.attributes['http.method'] &&
      !s.attributes['http.url'] &&
      s.attributes['Invoker'] === undefined &&
      (s.attributes['client.address'] !== undefined ||
        s.attributes['net.host.name'] !== undefined ||
        s.attributes['http.target'] !== undefined);
    const httpServerCandidates = allSpans
      .filter(isHttpServerSpan)
      .sort((a, b) => this.parseStartTime(a.startTime) - this.parseStartTime(b.startTime));
    const rootHttpSpan = httpServerCandidates[0];
    const requestSpan = rootHttpSpan || rootSpan || null;

    const highLevel = allSpans.filter(
      s => isAgentSpan(s) || isLlmSpan(s) || isMcpSpan(s) || isSecuritySpan(s) || isPolicySpan(s) || isBackendSpan(s),
    );
    highLevel.sort((a, b) => this.parseStartTime(a.startTime) - this.parseStartTime(b.startTime));

    this.cards = [];

    // Caller of the API — anchors the left side of the loop diagram.
    this.cards.push({
      id: '__client__',
      type: 'client',
      label: 'Client',
      subtitle: 'Caller',
      durationMs: null,
      span: null,
      icon: 'person',
    });

    this.cards.push({
      id: '__request__',
      type: 'request',
      label: 'Request',
      subtitle: this.trace.rootOperation || 'Request',
      durationMs: requestSpan ? requestSpan.durationNanos / 1_000_000 : null,
      span: requestSpan,
      icon: 'play_arrow',
    });

    let llmCounter = 0;
    for (const span of highLevel) {
      const durationMs = span.durationNanos / 1_000_000;

      if (isSecuritySpan(span)) {
        const type = span.attributes['Security'] || span.attributes['security'] || 'security';
        this.cards.push({
          id: span.spanId,
          type: 'security',
          label: 'Security',
          subtitle: type,
          durationMs,
          span,
          icon: 'lock',
        });
      } else if (isPolicySpan(span)) {
        const policyId = span.attributes['gravitee.policy'] ?? 'policy';
        const plugin = this.policyById.get(policyId);
        const phase = getPhase(span);
        this.cards.push({
          id: span.spanId,
          type: 'policy',
          label: plugin?.name ?? policyId,
          subtitle: phase ? `Policy · ${phase}` : 'Policy',
          durationMs,
          span,
          icon: 'policy',
          iconUrl: plugin?.icon,
        });
      } else if (isBackendSpan(span)) {
        // Enrich with the underlying HTTP client call (child span that carries http.url / http.method / http.status_code).
        const httpChild = allSpans.find(s => s.parentSpanId === span.spanId && !!s.attributes['http.url']);
        const endpointId = span.attributes['gravitee.endpoint.id'];
        const url = httpChild?.attributes['http.url'];
        const method = httpChild?.attributes['http.method'];
        const statusRaw = httpChild?.attributes['http.status_code'];
        const statusCode = statusRaw != null ? Number(statusRaw) : undefined;
        const label = endpointId ?? span.attributes['Invoker'] ?? 'Backend';
        this.cards.push({
          id: span.spanId,
          type: 'backend',
          label,
          subtitle: span.attributes['Invoker'] ?? 'Backend call',
          durationMs,
          span,
          icon: 'cloud',
          statusCode: Number.isFinite(statusCode) ? statusCode : undefined,
          httpMethod: method,
          httpUrl: url,
        });
      } else if (isAgentSpan(span)) {
        const agentName =
          span.attributes['gen_ai.agent.name'] || span.operationName.replace('invoke_agent ', '') || 'Agent';
        this.cards.push({
          id: span.spanId,
          type: 'agent',
          label: agentName,
          subtitle: span.serviceName,
          durationMs,
          span,
          icon: 'smart_toy',
        });
      } else if (isLlmSpan(span)) {
        llmCounter++;
        const model = span.attributes['gen_ai.request.model'] || span.attributes['gen_ai.response.model'] || 'LLM';
        this.cards.push({
          id: span.spanId,
          type: 'llm',
          label: `LLM #${llmCounter}`,
          subtitle: model,
          durationMs,
          span,
          icon: 'psychology',
        });
      } else if (isMcpSpan(span)) {
        const toolName = span.attributes['gen_ai.tool.name'] || span.attributes['mcp.method.name'] || 'MCP';
        this.cards.push({
          id: span.spanId,
          type: 'mcp',
          label: toolName,
          subtitle: span.serviceName,
          durationMs,
          span,
          icon: 'dns',
        });
      }
    }

    const totalDurationMs = this.trace.durationNanos ? this.trace.durationNanos / 1_000_000 : null;
    // Response is a synthetic aggregation card — no span-level attributes worth surfacing in the detail panel.
    this.cards.push({
      id: '__response__',
      type: 'response',
      label: 'Response',
      subtitle: totalDurationMs !== null ? `Total: ${this.formatMs(totalDurationMs)}` : '',
      durationMs: totalDurationMs,
      span: null,
      icon: 'check_circle',
    });

    // Stamp error status from the span (otel.status_code=ERROR) + the first "exception" event message if present.
    // Request/Response cards wrap the root HTTP span which always inherits its children's error status — that would light up
    // both end-cards for any failure, drowning out the real culprit, so they're excluded from the check.
    const isErrorStatus = (s: TraceSpan) => {
      const raw = s.attributes['otel.status_code'];
      return raw === 'ERROR' || raw === 'STATUS_CODE_ERROR' || raw === '2';
    };
    for (const card of this.cards) {
      if (!card.span) continue;
      if (card.type === 'request' || card.type === 'response') continue;
      if (isErrorStatus(card.span)) {
        card.hasError = true;
        const exceptionEvent = (card.span.events || []).find(e => e.name === 'exception');
        const msg = exceptionEvent?.attributes?.['exception.message'];
        if (msg) card.errorMessage = msg;
      }
    }

    this.lines = this.buildLines(this.cards);
  }

  /**
   * Builds the two-line layout: line 1 (left-to-right) ends with a stretch arrow turning down to the backend on line 2. Line 2 is
   * logically right-to-left — Backend at the right, Response at the left of the response cluster — but rendered LTR with arrow_back
   * icons. Line 2 is right-aligned so Backend sits directly below line 1's trailing arrow. Adjacent cards whose parent span carries
   * a "flow" attribute are wrapped in flow groups.
   */
  private buildLines(cards: FlowCard[]): FlowLine[] {
    const backendIdx = cards.findIndex(c => c.type === 'backend');

    // No backend — single-line GenAI-style trace.
    if (backendIdx < 0) {
      return [{ items: this.groupByFlow(cards), isResponse: false }];
    }

    const requestCards = cards.slice(0, backendIdx);
    const requestItems = this.groupByFlow(requestCards);
    requestItems.push({ kind: 'stretch', direction: 'forward' });

    // Line 2 data reversed so DOM LTR = visual LTR: [Response, ...response cards reversed, Backend]. Cards stay adjacent.
    const responseCards = [...cards.slice(backendIdx)].reverse();
    const responseItems: FlowItem[] = [{ kind: 'loopback-horizontal' }, ...this.groupByFlow(responseCards)];

    return [
      { items: requestItems, isResponse: false },
      { items: responseItems, isResponse: true },
    ];
  }

  private groupByFlow(cards: FlowCard[]): FlowItem[] {
    const items: FlowItem[] = [];
    let i = 0;
    while (i < cards.length) {
      const card = cards[i];
      const flowName = this.getFlowName(card);
      if (!flowName) {
        items.push({ kind: 'card', card });
        i++;
        continue;
      }
      const group: FlowCard[] = [card];
      let j = i + 1;
      while (j < cards.length && this.getFlowName(cards[j]) === flowName) {
        group.push(cards[j]);
        j++;
      }
      items.push({ kind: 'group', flowName, cards: group });
      i = j;
    }
    return items;
  }

  private getFlowName(card: FlowCard): string | null {
    if (!card.span || card.type === 'request' || card.type === 'response' || card.type === 'backend') return null;
    const parent = card.span.parentSpanId ? this.spanById.get(card.span.parentSpanId) : null;
    const flow = parent?.attributes['flow'];
    return flow && flow.length > 0 ? flow : null;
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
    if (typeof startTime === 'number') return startTime * 1_000_000_000;
    const d = new Date(startTime);
    if (!isNaN(d.getTime())) return d.getTime() * 1_000_000;
    const num = parseFloat(startTime);
    if (!isNaN(num)) return num * 1_000_000_000;
    return 0;
  }

  getCardColor(type: FlowCardType): { bg: string; border: string; icon: string } {
    return CARD_COLORS[type];
  }

  selectCard(card: FlowCard): void {
    this.selectedCard = card;
    this.selectedCardId = card.id;

    if (card.span) {
      this.selectedAttributes = Object.entries(card.span.attributes || {})
        .map(([key, value]) => ({ key, value }))
        .sort((a, b) => a.key.localeCompare(b.key));
    } else {
      this.selectedAttributes = [];
    }

    this.computeEventView(card.span);
  }

  /**
   * Looks for paired pre/post policy events. Gravitee emits `gravitee.policy.pre` and `gravitee.policy.post` events on a policy span
   * when verbose tracing is active — we render those as a before/after diff. For any other events we fall back to a plain list.
   */
  private computeEventView(span: TraceSpan | null): void {
    this.diffRows = [];
    this.otherEvents = [];

    if (!span || !span.events || span.events.length === 0) return;

    const pre = span.events.find(e => e.name === 'gravitee.policy.pre');
    const post = span.events.find(e => e.name === 'gravitee.policy.post');

    if (pre && post) {
      this.diffRows = this.buildDiff(pre.attributes || {}, post.attributes || {});
      // Surface any other events (shouldn't normally happen, but don't hide them)
      this.otherEvents = span.events.filter(e => e !== pre && e !== post);
    } else {
      this.otherEvents = [...span.events];
    }
  }

  private buildDiff(before: Record<string, string>, after: Record<string, string>): DiffRow[] {
    const keys = new Set<string>([...Object.keys(before), ...Object.keys(after)]);
    const rows: DiffRow[] = [];
    for (const key of keys) {
      const b = before[key] ?? null;
      const a = after[key] ?? null;
      let changed: DiffRow['changed'];
      if (b === null && a !== null) changed = 'added';
      else if (b !== null && a === null) changed = 'removed';
      else if (b !== a) changed = 'modified';
      else changed = 'unchanged';
      rows.push({ key, before: b, after: a, changed });
    }
    // Changed rows first, then alphabetical
    const order = { modified: 0, added: 1, removed: 2, unchanged: 3 };
    rows.sort((x, y) => order[x.changed] - order[y.changed] || x.key.localeCompare(y.key));
    return rows;
  }

  eventAttributes(event: TraceEvent): { key: string; value: string }[] {
    return Object.entries(event.attributes || {})
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => a.key.localeCompare(b.key));
  }

  exceptionInfo(span: TraceSpan | null): { type?: string; message?: string; stacktrace?: string } {
    if (!span) return {};
    const event = (span.events || []).find(e => e.name === 'exception');
    if (!event) return {};
    const a = event.attributes || {};
    return { type: a['exception.type'], message: a['exception.message'], stacktrace: a['exception.stacktrace'] };
  }

  cardTooltip(card: FlowCard): string {
    if (card.hasError && card.errorMessage) return card.errorMessage;
    if (card.hasError) return 'Error on this span';
    return card.iconUrl ? card.label : '';
  }

  formatFlowName(flowName: string): string {
    if (flowName.startsWith('organization-all')) return 'org';
    // Strip the trailing "-/" suffix the gateway appends when a flow matches the root path (e.g. "plan-all-/" → "plan-all").
    return flowName.endsWith('-/') ? flowName.slice(0, -2) : flowName;
  }

  statusClass(code: number): 'success' | 'redirect' | 'client-error' | 'server-error' | 'unknown' {
    if (code >= 200 && code < 300) return 'success';
    if (code >= 300 && code < 400) return 'redirect';
    if (code >= 400 && code < 500) return 'client-error';
    if (code >= 500 && code < 600) return 'server-error';
    return 'unknown';
  }

  private shortenUrl(url: string): string {
    try {
      const u = new URL(url);
      return u.host + u.pathname;
    } catch {
      return url;
    }
  }

  formatMs(ms: number): string {
    if (ms < 1) return `${Math.round(ms * 1000)}µs`;
    if (ms < 10) return `${ms.toFixed(1)}ms`;
    if (ms < 1000) return `${Math.round(ms)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }

  formatDuration(nanos: number): string {
    if (nanos < 1_000) return `${nanos}ns`;
    if (nanos < 1_000_000) return `${Math.round(nanos / 1_000)}µs`;
    const ms = nanos / 1_000_000;
    if (ms < 10) return `${ms.toFixed(1)}ms`;
    if (ms < 1000) return `${Math.round(ms)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }

  getTokens(span: TraceSpan): number | null {
    const input = parseInt(span.attributes['gen_ai.usage.input_tokens'] || '0', 10);
    const output = parseInt(span.attributes['gen_ai.usage.output_tokens'] || '0', 10);
    const total = input + output;
    return total > 0 ? total : null;
  }
}
