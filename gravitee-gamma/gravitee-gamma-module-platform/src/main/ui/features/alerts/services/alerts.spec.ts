/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    apiNotifToForm,
    createPlatformAlert,
    deletePlatformAlert,
    formConditionToApi,
    formNotifToApi,
    listPlatformAlertEvents,
    listPlatformAlerts,
    parseNotificationConfiguration,
    updatePlatformAlert,
    updatePlatformAlertFromForm,
} from './alerts';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { AlertFormCondition, AlertTrigger } from '../types/alert';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

const ALERT: AlertTrigger = {
    id: 'alert-1',
    name: 'Node down',
    description: 'Gateway stopped',
    severity: 'CRITICAL',
    enabled: true,
    source: 'NODE_LIFECYCLE',
    type: 'NODE_LIFECYCLE_CHANGED',
    conditions: [],
    filters: [],
    notifications: [],
    notificationPeriods: [],
    dampening: { mode: 'STRICT_COUNT', trueEvaluations: 1 },
};

const FORM_DATA = {
    name: 'High CPU',
    description: 'Node CPU',
    severity: 'WARNING' as const,
    enabled: true,
    source: 'NODE_HEARTBEAT',
    type: 'METRICS_SIMPLE_CONDITION',
    conditions: [{ type: 'THRESHOLD' as const, property: 'os.cpu.percent', operator: 'GT' as const, threshold: 80 }],
    filters: [],
    notifications: [],
    timeframes: [],
    dampening: { mode: 'STRICT_COUNT' as const, trueEvaluations: 1 },
};

describe('platform alerts service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    describe('listPlatformAlerts', () => {
        it('GETs /platform/alerts with event counts enabled', async () => {
            await listPlatformAlerts('env-1');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts?event_counts=true');
        });
    });

    describe('createPlatformAlert', () => {
        it('POSTs to /platform/alerts with ENVIRONMENT reference', async () => {
            mockApimFetchJsonV1Env.mockResolvedValue({ ...ALERT, id: 'created' });
            await createPlatformAlert('env-1', FORM_DATA);

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
                'env-1',
                '/platform/alerts',
                expect.objectContaining({
                    method: 'POST',
                    body: expect.stringContaining('"reference_type":"ENVIRONMENT"'),
                }),
            );
            const body = JSON.parse((mockApimFetchJsonV1Env.mock.calls[0][2] as { body: string }).body);
            expect(body.reference_id).toBe('env-1');
            expect(body.name).toBe('High CPU');
            expect(body.template).toBe(false);
        });

        it('omits notifications that have no channel type', async () => {
            mockApimFetchJsonV1Env.mockResolvedValue({ ...ALERT, id: 'created' });
            await createPlatformAlert('env-1', {
                ...FORM_DATA,
                notifications: [
                    { type: '', configuration: {} },
                    { type: 'webhook-notifier', configuration: { url: 'https://example.com' } },
                ],
            });

            const body = JSON.parse((mockApimFetchJsonV1Env.mock.calls[0][2] as { body: string }).body);
            expect(body.notifications).toEqual([{ type: 'webhook-notifier', configuration: { url: 'https://example.com' } }]);
        });

        it('maps timeframe start/end as seconds since midnight (classic beginHour/endHour)', async () => {
            mockApimFetchJsonV1Env.mockResolvedValue({ ...ALERT, id: 'created' });
            await createPlatformAlert('env-1', {
                ...FORM_DATA,
                timeframes: [{ days: [1, 2, 3, 4, 5], startHour: 9 * 3600, endHour: 18 * 3600 }],
            });

            const body = JSON.parse((mockApimFetchJsonV1Env.mock.calls[0][2] as { body: string }).body);
            expect(body.notificationPeriods[0]).toEqual(
                expect.objectContaining({
                    days: [1, 2, 3, 4, 5],
                    beginHour: 32400,
                    endHour: 64800,
                }),
            );
        });
    });

    describe('updatePlatformAlert', () => {
        it('PUTs the alert entity to /platform/alerts/{id}', async () => {
            const updated = { ...ALERT, enabled: false };
            await updatePlatformAlert('env-1', updated);

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts/alert-1', {
                method: 'PUT',
                body: JSON.stringify(updated),
            });
        });

        it('URL-encodes the alert id', async () => {
            await updatePlatformAlert('env-1', { ...ALERT, id: 'a/b' });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts/a%2Fb', expect.any(Object));
        });
    });

    describe('updatePlatformAlertFromForm', () => {
        it('PUTs converted form data to /platform/alerts/{id}', async () => {
            await updatePlatformAlertFromForm('env-1', 'alert-1', FORM_DATA);

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
                'env-1',
                '/platform/alerts/alert-1',
                expect.objectContaining({ method: 'PUT' }),
            );
            const body = JSON.parse((mockApimFetchJsonV1Env.mock.calls[0][2] as { body: string }).body);
            expect(body.id).toBe('alert-1');
            expect(body.name).toBe('High CPU');
        });

        it('preserves template and event_rules from the existing trigger', async () => {
            await updatePlatformAlertFromForm('env-1', 'tpl-1', FORM_DATA, {
                template: true,
                event_rules: [{ event: 'API_CREATE' }],
            });

            const body = JSON.parse((mockApimFetchJsonV1Env.mock.calls[0][2] as { body: string }).body);
            expect(body.template).toBe(true);
            expect(body.event_rules).toEqual([{ event: 'API_CREATE' }]);
        });

        it('preserves classic group-by projections from the existing trigger', async () => {
            await updatePlatformAlertFromForm('env-1', 'alert-1', FORM_DATA, {
                projections: [{ type: 'PROPERTY', property: 'api' }],
            });

            const body = JSON.parse((mockApimFetchJsonV1Env.mock.calls[0][2] as { body: string }).body);
            expect(body.projections).toEqual([{ type: 'PROPERTY', property: 'api' }]);
        });
    });

    describe('deletePlatformAlert', () => {
        it('DELETEs /platform/alerts/{id}', async () => {
            await deletePlatformAlert('env-1', 'alert-1');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts/alert-1', { method: 'DELETE' });
        });
    });

    describe('listPlatformAlertEvents', () => {
        it('GETs /platform/alerts/{id}/events with pagination', async () => {
            mockApimFetchJsonV1Env.mockResolvedValue({ content: [], totalElements: 0 });
            await listPlatformAlertEvents('env-1', 'alert-1', 2, 20);

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts/alert-1/events?page=2&size=20');
        });
    });

    describe('formConditionToApi', () => {
        it('maps RATE conditions with nested comparison', () => {
            const condition: AlertFormCondition = {
                type: 'RATE',
                property: 'os.cpu.percent',
                operator: 'GT',
                threshold: 50,
                rateOperator: 'GTE',
                rateThreshold: 10,
                duration: 5,
                timeUnit: 'MINUTES',
            };
            expect(formConditionToApi(condition)).toEqual({
                type: 'RATE',
                operator: 'GTE',
                threshold: 10,
                comparison: { type: 'THRESHOLD', property: 'os.cpu.percent', operator: 'GT', threshold: 50 },
                duration: 5,
                timeUnit: 'MINUTES',
            });
        });

        it('round-trips group-by projections on a THRESHOLD condition', () => {
            expect(
                formConditionToApi({
                    type: 'THRESHOLD',
                    property: 'response.response_time',
                    operator: 'GT',
                    threshold: 500,
                    projections: [{ type: 'PROPERTY', property: 'api' }],
                }),
            ).toEqual({
                type: 'THRESHOLD',
                property: 'response.response_time',
                operator: 'GT',
                threshold: 500,
                projections: [{ type: 'PROPERTY', property: 'api' }],
            });
        });

        it('defaults THRESHOLD operator to GT when missing (classic requires operator)', () => {
            expect(
                formConditionToApi({
                    type: 'THRESHOLD',
                    property: 'response.response_time',
                    threshold: 10,
                }),
            ).toEqual({
                type: 'THRESHOLD',
                property: 'response.response_time',
                operator: 'GT',
                threshold: 10,
            });
        });

        it('maps THRESHOLD_RANGE with classic BETWEEN operator', () => {
            expect(
                formConditionToApi({
                    type: 'THRESHOLD_RANGE',
                    property: 'response.response_time',
                    thresholdLow: 200,
                    thresholdHigh: 488,
                }),
            ).toEqual({
                type: 'THRESHOLD_RANGE',
                property: 'response.response_time',
                operator: 'BETWEEN',
                operatorLow: 'INCLUSIVE',
                thresholdLow: 200,
                operatorHigh: 'INCLUSIVE',
                thresholdHigh: 488,
            });
        });
    });

    describe('parseNotificationConfiguration', () => {
        it('returns an object as-is', () => {
            expect(parseNotificationConfiguration({ to: 'ops@example.com' })).toEqual({ to: 'ops@example.com' });
        });

        it('parses a JSON string from the API', () => {
            expect(parseNotificationConfiguration('{"url":"https://hooks.example.com","method":"POST"}')).toEqual({
                url: 'https://hooks.example.com',
                method: 'POST',
            });
        });

        it('returns {} for empty values', () => {
            expect(parseNotificationConfiguration(undefined)).toEqual({});
            expect(parseNotificationConfiguration('')).toEqual({});
        });

        it('throws when the JSON string is malformed or not an object', () => {
            expect(() => parseNotificationConfiguration('not-json')).toThrow(/not valid JSON/i);
            expect(() => parseNotificationConfiguration('[]')).toThrow(/JSON object/i);
        });
    });

    describe('formNotifToApi / apiNotifToForm', () => {
        it('passes through type and full configuration', () => {
            const form = {
                type: 'slack-notifier',
                configuration: { channel: '#ops', token: 'xoxb-test' },
            };
            expect(formNotifToApi(form)).toEqual({
                type: 'slack-notifier',
                configuration: { channel: '#ops', token: 'xoxb-test' },
            });
        });

        it('parses string configuration when loading an alert', () => {
            expect(
                apiNotifToForm({
                    type: 'webhook-notifier',
                    configuration: '{"method":"POST","url":"https://example.com"}',
                }),
            ).toEqual({
                type: 'webhook-notifier',
                configuration: { method: 'POST', url: 'https://example.com' },
            });
        });
    });
});
