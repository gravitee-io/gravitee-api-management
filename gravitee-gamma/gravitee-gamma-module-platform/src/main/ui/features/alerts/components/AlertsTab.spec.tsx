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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useMemo, useState } from 'react';

import { AlertsTab } from './AlertsTab';
import {
    ALERT_RULES,
    getAlertRuleCategoriesForEnvironment,
    getAlertRulesForEnvironment,
    getFilterMetricsForRuleId,
    getMetricsForRuleId,
} from '../constants/alertConstants';
import type { AlertFormCondition, AlertRuleId } from '../types';
import { defaultFilterCondition } from '../utils/alertConditionComplete';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../hooks/useAlertLookupOptions', () => ({
    useAlertLookupOptions: () => ({ tenants: [], apis: [] }),
}));

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

function FilterHarness({ ruleId }: { ruleId: AlertRuleId }) {
    const selectedRule = ALERT_RULES.find(rule => rule.id === ruleId);
    const metricsForRule = useMemo(() => getMetricsForRuleId(ruleId), [ruleId]);
    const filterMetrics = useMemo(() => getFilterMetricsForRuleId(ruleId), [ruleId]);
    const [filters, setFilters] = useState<AlertFormCondition[]>([]);
    const noop = () => undefined;

    return (
        <AlertsTab
            name="Filter options"
            setName={noop}
            description=""
            setDescription={noop}
            severity="WARNING"
            setSeverity={noop}
            enabled
            setEnabled={noop}
            handleRuleChange={noop}
            isUpdate={false}
            canEdit
            errors={{}}
            setErrors={noop}
            markDirty={noop}
            timeframes={[]}
            addTimeframe={noop}
            removeTimeframe={noop}
            toggleTimeframeDay={noop}
            updateTimeframeHour={noop}
            setTimeframeDays={noop}
            updateTimeframeHours={noop}
            conditions={[]}
            updateCondition={noop}
            metricsForRule={metricsForRule}
            filterMetrics={filterMetrics}
            filters={filters}
            addFilter={() => setFilters(prev => [...prev, defaultFilterCondition(filterMetrics[0]?.key ?? '')])}
            updateFilter={noop}
            removeFilter={noop}
            selectedRule={selectedRule}
            ruleLabel={selectedRule?.description ?? ''}
            template={false}
            setTemplate={noop}
            associateOnApiCreate={false}
            setAssociateOnApiCreate={noop}
        />
    );
}

function emptyRuleHarness() {
    const noop = () => undefined;
    return (
        <AlertsTab
            name="New alert"
            setName={noop}
            description=""
            setDescription={noop}
            severity="INFO"
            setSeverity={noop}
            enabled={false}
            setEnabled={noop}
            handleRuleChange={noop}
            isUpdate={false}
            canEdit
            errors={{}}
            setErrors={noop}
            markDirty={noop}
            timeframes={[]}
            addTimeframe={noop}
            removeTimeframe={noop}
            toggleTimeframeDay={noop}
            updateTimeframeHour={noop}
            setTimeframeDays={noop}
            updateTimeframeHours={noop}
            conditions={[]}
            updateCondition={noop}
            metricsForRule={[]}
            filterMetrics={[]}
            filters={[]}
            addFilter={noop}
            updateFilter={noop}
            removeFilter={noop}
            selectedRule={undefined}
            ruleLabel=""
            template={false}
            setTemplate={noop}
            associateOnApiCreate={false}
            setAssociateOnApiCreate={noop}
        />
    );
}

describe('AlertsTab filter options', () => {
    it('seeds a request-aggregation filter from full API metrics, not the numeric condition list', async () => {
        const user = userEvent.setup();
        render(<FilterHarness ruleId="REQUEST@METRICS_AGGREGATION" />);

        await user.click(screen.getByRole('button', { name: /add filter/i }));

        expect(screen.getByText('Filter 1')).not.toBeNull();
        expect(screen.getByText('Response Time (ms)')).not.toBeNull();
        expect(getFilterMetricsForRuleId('REQUEST@METRICS_AGGREGATION').some(m => m.key === 'api')).toBe(true);
        expect(getMetricsForRuleId('REQUEST@METRICS_AGGREGATION').some(m => m.key === 'api')).toBe(false);
    });

    it('seeds a health-check filter with Classic old-status, not an API request metric', async () => {
        const user = userEvent.setup();
        render(<FilterHarness ruleId="ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED" />);

        await user.click(screen.getByRole('button', { name: /add filter/i }));

        expect(screen.getByText('Old Status')).not.toBeNull();
        expect(screen.queryByText('Status Code')).toBeNull();
    });

    it('shows Classic endpoint projection on health-check', () => {
        const selectedRule = ALERT_RULES.find(rule => rule.id === 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED');
        const noop = () => undefined;
        render(
            <AlertsTab
                name="Health-check"
                setName={noop}
                description=""
                setDescription={noop}
                severity="WARNING"
                setSeverity={noop}
                enabled
                setEnabled={noop}
                handleRuleChange={noop}
                isUpdate={false}
                canEdit
                errors={{}}
                setErrors={noop}
                markDirty={noop}
                timeframes={[]}
                addTimeframe={noop}
                removeTimeframe={noop}
                toggleTimeframeDay={noop}
                updateTimeframeHour={noop}
                setTimeframeDays={noop}
                updateTimeframeHours={noop}
                conditions={[{ type: 'STRING_COMPARE', property: 'status.old', property2: 'status.new', operator: 'NOT_EQUALS' }]}
                updateCondition={noop}
                metricsForRule={getMetricsForRuleId('ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED')}
                filterMetrics={getFilterMetricsForRuleId('ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED')}
                filters={[]}
                addFilter={noop}
                updateFilter={noop}
                removeFilter={noop}
                selectedRule={selectedRule}
                ruleLabel={selectedRule?.description ?? ''}
                template={false}
                setTemplate={noop}
                associateOnApiCreate={false}
                setAssociateOnApiCreate={noop}
            />,
        );

        expect(screen.getByText('Set a projection')).not.toBeNull();
    });

    it('seeds a node-lifecycle filter with Classic hostname, not an API request metric', async () => {
        const user = userEvent.setup();
        render(<FilterHarness ruleId="NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED" />);

        await user.click(screen.getByRole('button', { name: /add filter/i }));

        expect(screen.getByText('Hostname')).not.toBeNull();
        expect(screen.queryByText('Response Time (ms)')).toBeNull();
        expect(screen.queryByText('OS CPU (%)')).toBeNull();
    });
});

describe('AlertsTab create chrome', () => {
    it('asks to select a rule before the condition when none is chosen', () => {
        render(emptyRuleHarness());

        expect(screen.getByText(/select a rule before setting the condition/i)).not.toBeNull();
        expect(screen.queryByText('When')).toBeNull();
    });

    it('hides Node rules when they are omitted from the environment list', async () => {
        const user = userEvent.setup();
        const noop = () => undefined;
        render(
            <AlertsTab
                name="New alert"
                setName={noop}
                description=""
                setDescription={noop}
                severity="INFO"
                setSeverity={noop}
                enabled={false}
                setEnabled={noop}
                handleRuleChange={noop}
                isUpdate={false}
                canEdit
                errors={{}}
                setErrors={noop}
                markDirty={noop}
                timeframes={[]}
                addTimeframe={noop}
                removeTimeframe={noop}
                toggleTimeframeDay={noop}
                updateTimeframeHour={noop}
                setTimeframeDays={noop}
                updateTimeframeHours={noop}
                conditions={[]}
                updateCondition={noop}
                metricsForRule={[]}
                filterMetrics={[]}
                filters={[]}
                addFilter={noop}
                updateFilter={noop}
                removeFilter={noop}
                selectedRule={undefined}
                ruleLabel=""
                template={false}
                setTemplate={noop}
                associateOnApiCreate={false}
                setAssociateOnApiCreate={noop}
                rules={getAlertRulesForEnvironment(true)}
                ruleCategories={getAlertRuleCategoriesForEnvironment(true)}
            />,
        );

        await user.click(screen.getByLabelText(/^rule/i));

        expect(screen.queryByRole('option', { name: /lifecycle status of a node/i })).toBeNull();
        expect(screen.getByRole('option', { name: /metric of the request validates/i })).not.toBeNull();
    });
});
