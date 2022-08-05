package io.gravitee.repository.jdbc.management;/**
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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowConsumer;
import io.gravitee.repository.management.model.flow.FlowConsumerType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import io.gravitee.repository.management.model.flow.selector.FlowSelectorType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcFlowRepository extends JdbcAbstractCrudRepository<Flow, String> implements FlowRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcFlowRepository.class);
    private final String FLOW_STEPS;
    private final String FLOW_METHODS;
    private final String FLOW_CONSUMERS;
    private final String FLOW_SELECTORS;
    private final String FLOW_SELECTOR_HTTP_METHODS;
    private final String FLOW_SELECTOR_CHANNEL_OPERATIONS;
    private final String FLOW_TAGS;
    private final JdbcObjectMapper<FlowStep> stepsOrm;

    JdbcFlowRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "flows");
        FLOW_METHODS = getTableNameFor("flow_methods");
        FLOW_STEPS = getTableNameFor("flow_steps");
        FLOW_SELECTORS = getTableNameFor("flow_selectors");
        FLOW_SELECTOR_HTTP_METHODS = getTableNameFor("flow_selector_http_methods");
        FLOW_SELECTOR_CHANNEL_OPERATIONS = getTableNameFor("flow_selector_channel_operations");
        FLOW_TAGS = getTableNameFor("flow_tags");
        FLOW_CONSUMERS = getTableNameFor("flow_consumers");
        this.stepsOrm =
            JdbcObjectMapper
                .builder(FlowStep.class, FLOW_STEPS)
                .addColumn("policy", Types.NVARCHAR, String.class)
                .addColumn("description", Types.NVARCHAR, String.class)
                .addColumn("enabled", Types.BOOLEAN, boolean.class)
                .addColumn("name", Types.NVARCHAR, String.class)
                .addColumn("configuration", Types.NVARCHAR, String.class)
                .addColumn("order", Types.INTEGER, int.class)
                .addColumn("condition", Types.NVARCHAR, String.class)
                .build();
    }

    @Override
    protected JdbcObjectMapper<Flow> buildOrm() {
        return JdbcObjectMapper
            .builder(Flow.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("condition", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("enabled", Types.BOOLEAN, boolean.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("path", Types.NVARCHAR, String.class)
            .addColumn("operator", Types.NVARCHAR, FlowOperator.class)
            .addColumn("referenceId", Types.NVARCHAR, String.class)
            .addColumn("referenceType", Types.NVARCHAR, FlowReferenceType.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("order", Types.INTEGER, int.class)
            .build();
    }

    @Override
    public Optional<Flow> findById(String id) throws TechnicalException {
        final Optional<Flow> flow = super.findById(id);
        flow.ifPresent(this::addSteps);
        flow.ifPresent(this::addSelectors);
        flow.ifPresent(this::addTags);

        // deprecated data
        flow.ifPresent(this::addMethods);
        flow.ifPresent(this::addConsumers);
        return flow;
    }

    @Override
    protected String getId(Flow item) {
        return item.getId();
    }

    @Override
    public Flow create(Flow flow) throws TechnicalException {
        try {
            storeSteps(flow, false);
            storeSelectors(flow.getId(), flow.getSelectors(), false);
            storeTags(flow.getId(), flow.getTags(), false);

            // deprecated data
            storeMethods(flow.getId(), flow.getMethods(), false);
            storeConsumers(flow, false);
            return super.create(flow);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public Flow update(Flow flow) throws TechnicalException {
        try {
            storeSteps(flow, true);
            storeSelectors(flow.getId(), flow.getSelectors(), true);
            storeTags(flow.getId(), flow.getTags(), true);

            // deprecated data
            storeMethods(flow.getId(), flow.getMethods(), true);
            storeConsumers(flow, true);
            return super.update(flow);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + FLOW_STEPS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_SELECTORS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_SELECTOR_HTTP_METHODS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_TAGS + " where flow_id = ?", id);
        // deprecated data
        jdbcTemplate.update("delete from " + FLOW_METHODS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_CONSUMERS + " where flow_id = ?", id);
        super.delete(id);
    }

    @Override
    public Set<Flow> findAll() throws TechnicalException {
        Set<Flow> all = super.findAll();
        for (Flow flow : all) {
            this.addSteps(flow);
            this.addSelectors(flow);
            this.addTags(flow);
            // deprecated data
            this.addMethods(flow);
            this.addConsumers(flow);
        }
        return all;
    }

    @Override
    public List<Flow> findByReference(FlowReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcFlowRepository.findByReference({}, {))", referenceType, referenceId);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() +
                    " t where reference_id = ? and reference_type = ? order by " +
                    escapeReservedWord("order") +
                    " asc",
                    getOrm().getRowMapper(),
                    referenceId,
                    referenceType.name()
                )
                .stream()
                .peek(this::addSteps)
                .peek(this::addSelectors)
                .peek(this::addTags)
                // deprecated data
                .peek(this::addMethods)
                .peek(this::addConsumers)
                .collect(Collectors.toList());
        } catch (final Exception ex) {
            LOGGER.error("Failed to find flows by reference:", ex);
            throw new TechnicalException("Failed to find flows by reference", ex);
        }
    }

    @Override
    public void deleteByReference(FlowReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcFlowRepository.deleteByReference({}, {))", referenceType, referenceId);
        try {
            List<Flow> flows = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " t where reference_id = ? and reference_type = ?",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name()
            );

            if (flows.size() > 0) {
                List<String> flowIds = flows.stream().map(flow -> flow.getId()).collect(Collectors.toList());
                String buildInClause = getOrm().buildInClause(flowIds);
                String[] ids = flowIds.toArray(new String[0]);
                jdbcTemplate.update("delete from " + tableName + " where id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_STEPS + " where flow_id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_SELECTORS + " where flow_id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_SELECTOR_HTTP_METHODS + " where flow_id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " where flow_id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_TAGS + " where flow_id in (" + buildInClause + ")", ids);
                // deprecated data
                jdbcTemplate.update("delete from " + FLOW_METHODS + " where flow_id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_CONSUMERS + " where flow_id in (" + buildInClause + ")", ids);
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete flows by reference:", ex);
            throw new TechnicalException("Failed to delete flows by reference", ex);
        }
    }

    private void addSelectors(final Flow flow) {
        List<FlowSelector> flowSelectors = getSelectors(flow.getId());
        flow.setSelectors(flowSelectors);
    }

    private List<FlowSelector> getSelectors(final String flowId) {
        List<FlowSelector> flowSelectors = jdbcTemplate.query(
            "select type, path, path_operator, " +
            escapeReservedWord("condition") +
            ", channel, channel_operator from " +
            FLOW_SELECTORS +
            " where flow_id = ?",
            (resultSet, i) -> {
                String type = resultSet.getString(1);
                FlowSelectorType flowSelectorType = FlowSelectorType.valueOf(type);
                switch (flowSelectorType) {
                    case HTTP:
                        FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
                        flowHttpSelector.setPath(resultSet.getString(2));
                        flowHttpSelector.setPathOperator(FlowOperator.valueOf(resultSet.getString(3)));
                        return flowHttpSelector;
                    case CONDITION:
                        FlowConditionSelector flowConditionSelector = new FlowConditionSelector();
                        flowConditionSelector.setCondition(resultSet.getString(4));
                        return flowConditionSelector;
                    case CHANNEL:
                        FlowChannelSelector flowChannelSelector = new FlowChannelSelector();
                        flowChannelSelector.setChannel(resultSet.getString(5));
                        flowChannelSelector.setChannelOperator(FlowOperator.valueOf(resultSet.getString(6)));
                        return flowChannelSelector;
                    default:
                        return null;
                }
            },
            flowId
        );
        return flowSelectors
            .stream()
            .peek(
                flowSelector -> {
                    if (flowSelector.getType() == FlowSelectorType.HTTP) {
                        FlowHttpSelector flowHttpSelector = (FlowHttpSelector) flowSelector;
                        Set<HttpMethod> methods = getSelectorHttpMethods(flowId);
                        flowHttpSelector.setMethods(methods);
                    } else if (flowSelector.getType() == FlowSelectorType.CHANNEL) {
                        FlowChannelSelector flowChannelSelector = (FlowChannelSelector) flowSelector;
                        flowChannelSelector.setOperations(getChannelOperation(flowId));
                    }
                }
            )
            .collect(Collectors.toList());
    }

    private Set<FlowChannelSelector.Operation> getChannelOperation(final String flowId) {
        return jdbcTemplate
            .queryForList("select channel_operation from " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " where flow_id = ?", String.class, flowId)
            .stream()
            .map(FlowChannelSelector.Operation::valueOf)
            .collect(Collectors.toSet());
    }

    private void addSteps(Flow flow) {
        List<FlowStep> requestSteps = getPhaseSteps(flow.getId(), FlowStepPhase.REQUEST);
        flow.setRequest(requestSteps);
        List<FlowStep> responseSteps = getPhaseSteps(flow.getId(), FlowStepPhase.RESPONSE);
        flow.setResponse(responseSteps);
        List<FlowStep> subscribeSteps = getPhaseSteps(flow.getId(), FlowStepPhase.SUBSCRIBE);
        flow.setSubscribe(subscribeSteps);
        List<FlowStep> publishSteps = getPhaseSteps(flow.getId(), FlowStepPhase.PUBLISH);
        flow.setPublish(publishSteps);

        // Deprecated
        List<FlowStep> preSteps = getPhaseSteps(flow.getId(), FlowStepPhase.PRE);
        flow.setPre(preSteps);
        List<FlowStep> postSteps = getPhaseSteps(flow.getId(), FlowStepPhase.POST);
        flow.setPost(postSteps);
    }

    private List<FlowStep> getPhaseSteps(String flowId, FlowStepPhase phase) {
        return jdbcTemplate.query(
            this.stepsOrm.getSelectAllSql() + " where flow_id = ? and phase = ? order by " + escapeReservedWord("order") + " asc",
            this.stepsOrm.getRowMapper(),
            flowId,
            phase.name()
        );
    }

    private void addMethods(Flow flow) {
        Set<HttpMethod> methods = getMethods(FLOW_METHODS, flow.getId());
        flow.setMethods(methods);
    }

    private Set<HttpMethod> getSelectorHttpMethods(final String flowId) {
        return getMethods(FLOW_SELECTOR_HTTP_METHODS, flowId);
    }

    private Set<HttpMethod> getMethods(final String methodTableName, final String flowId) {
        return jdbcTemplate
            .queryForList("select method from " + methodTableName + " where flow_id = ?", String.class, flowId)
            .stream()
            .map(HttpMethod::valueOf)
            .collect(Collectors.toSet());
    }

    @Deprecated
    private void storeConsumers(Flow flow, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_CONSUMERS + " where flow_id = ?", flow.getId());
        }
        if (flow.getConsumers() != null && !flow.getConsumers().isEmpty()) {
            List<FlowConsumer> consumers = flow.getConsumers();
            jdbcTemplate.batchUpdate(
                "insert into " + FLOW_CONSUMERS + " ( flow_id, consumer_id, consumer_type ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, flow.getId());
                        ps.setString(2, consumers.get(i).getConsumerId());
                        ps.setString(3, consumers.get(i).getConsumerType().name());
                    }

                    @Override
                    public int getBatchSize() {
                        return flow.getConsumers().size();
                    }
                }
            );
        }
    }

    @Deprecated
    private void addConsumers(Flow flow) {
        List<FlowConsumer> consumers = getConsumers(flow.getId());
        flow.setConsumers(consumers);
    }

    @Deprecated
    private List<FlowConsumer> getConsumers(final String flowId) {
        return jdbcTemplate.query(
            "select consumer_type, consumer_id from " + FLOW_CONSUMERS + " where flow_id = ?",
            (resultSet, i) -> {
                FlowConsumer flowConsumer = new FlowConsumer();
                flowConsumer.setConsumerType(FlowConsumerType.valueOf(resultSet.getString(1)));
                flowConsumer.setConsumerId(resultSet.getString(2));
                return flowConsumer;
            },
            flowId
        );
    }

    private void storeTags(final String flowId, Set<String> tags, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_TAGS + " where flow_id = ?", flowId);
        }
        if (tags != null && !tags.isEmpty()) {
            Iterator<String> tagsIterator = tags.iterator();
            jdbcTemplate.batchUpdate(
                "insert into " + FLOW_TAGS + " ( flow_id, tag ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, flowId);
                        ps.setString(2, tagsIterator.next());
                    }

                    @Override
                    public int getBatchSize() {
                        return tags.size();
                    }
                }
            );
        }
    }

    private void addTags(Flow flow) {
        Set<String> tags = getTags(flow.getId());
        flow.setTags(tags);
    }

    private Set<String> getTags(final String flowId) {
        return new HashSet<>(jdbcTemplate.queryForList("select tag from " + FLOW_TAGS + " where flow_id = ?", String.class, flowId));
    }

    private void storeSteps(final Flow flow, final boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_STEPS + " where flow_id = ?", flow.getId());
        }
        storePhaseSteps(flow, flow.getPre(), FlowStepPhase.PRE);
        storePhaseSteps(flow, flow.getPost(), FlowStepPhase.POST);
        storePhaseSteps(flow, flow.getRequest(), FlowStepPhase.REQUEST);
        storePhaseSteps(flow, flow.getResponse(), FlowStepPhase.RESPONSE);
        storePhaseSteps(flow, flow.getSubscribe(), FlowStepPhase.SUBSCRIBE);
        storePhaseSteps(flow, flow.getPublish(), FlowStepPhase.PUBLISH);
    }

    private void storePhaseSteps(final Flow flow, final List<FlowStep> steps, final FlowStepPhase phase) {
        if (steps != null && !steps.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " +
                FLOW_STEPS +
                " ( flow_id, name, policy, description, configuration, enabled, " +
                escapeReservedWord("order") +
                ", " +
                escapeReservedWord("condition") +
                ", phase ) values ( ?, ?, ?, ?, ? , ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        FlowStep flowStep = steps.get(i);
                        ps.setString(1, flow.getId());
                        ps.setString(2, flowStep.getName());
                        ps.setString(3, flowStep.getPolicy());
                        ps.setString(4, flowStep.getDescription());
                        ps.setString(5, flowStep.getConfiguration());
                        ps.setBoolean(6, flowStep.isEnabled());
                        ps.setInt(7, flowStep.getOrder());
                        ps.setString(8, flowStep.getCondition());
                        ps.setString(9, phase.name());
                    }

                    @Override
                    public int getBatchSize() {
                        return steps.size();
                    }
                }
            );
        }
    }

    private void storeSelectors(String flowId, List<FlowSelector> selectors, boolean deleteFirst) {
        if (selectors != null && !selectors.isEmpty()) {
            if (deleteFirst) {
                jdbcTemplate.update("delete from " + FLOW_SELECTORS + " where flow_id = ?", flowId);
            }

            jdbcTemplate.batchUpdate(
                "insert into " +
                FLOW_SELECTORS +
                " ( flow_id, type, path, path_operator, " +
                escapeReservedWord("condition") +
                ", channel,channel_operator ) values ( ? ,? ,? ,? ,? ,? ,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        FlowSelector flowSelector = selectors.get(i);
                        ps.setString(1, flowId);
                        ps.setString(2, flowSelector.getType().name());
                        switch (flowSelector.getType()) {
                            default:
                            case HTTP:
                                FlowHttpSelector flowHttpSelector = (FlowHttpSelector) flowSelector;
                                ps.setString(3, flowHttpSelector.getPath());
                                ps.setString(4, flowHttpSelector.getPathOperator().name());
                                ps.setString(5, null);
                                ps.setString(6, null);
                                ps.setString(7, null);
                                break;
                            case CONDITION:
                                FlowConditionSelector flowConditionSelector = (FlowConditionSelector) flowSelector;
                                ps.setString(3, null);
                                ps.setString(4, null);
                                ps.setString(5, flowConditionSelector.getCondition());
                                ps.setString(6, null);
                                ps.setString(7, null);
                                break;
                            case CHANNEL:
                                FlowChannelSelector flowChannelSelector = (FlowChannelSelector) flowSelector;
                                ps.setString(3, null);
                                ps.setString(4, null);
                                ps.setString(5, null);
                                ps.setString(6, flowChannelSelector.getChannel());
                                ps.setString(7, flowChannelSelector.getChannelOperator().name());
                                break;
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        return selectors.size();
                    }
                }
            );
            selectors
                .stream()
                .filter(flowSelector -> flowSelector instanceof FlowHttpSelector)
                .map(flowSelector -> (FlowHttpSelector) flowSelector)
                .findFirst()
                .ifPresent(flowHttpSelector -> storeSelectorHttpMethods(flowId, flowHttpSelector.getMethods(), deleteFirst));

            selectors
                .stream()
                .filter(flowSelector -> flowSelector instanceof FlowChannelSelector)
                .map(flowSelector -> (FlowChannelSelector) flowSelector)
                .findFirst()
                .ifPresent(flowChannelSelector -> storeSelectorChannelOperation(flowId, flowChannelSelector.getOperations(), deleteFirst));
        }
    }

    private void storeSelectorHttpMethods(final String flowId, Set<HttpMethod> httpMethods, boolean deleteFirst) {
        storeMethods(FLOW_SELECTOR_HTTP_METHODS, flowId, httpMethods, deleteFirst);
    }

    private void storeMethods(final String flowId, Set<HttpMethod> httpMethods, boolean deleteFirst) {
        storeMethods(FLOW_METHODS, flowId, httpMethods, deleteFirst);
    }

    private void storeMethods(final String methodTableName, final String flowId, Set<HttpMethod> httpMethods, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + methodTableName + " where flow_id = ?", flowId);
        }
        if (httpMethods != null && !httpMethods.isEmpty()) {
            Iterator<HttpMethod> methods = httpMethods.iterator();
            jdbcTemplate.batchUpdate(
                "insert into " + methodTableName + " ( flow_id, method ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        HttpMethod next = methods.next();
                        ps.setString(1, flowId);
                        ps.setString(2, next.name());
                    }

                    @Override
                    public int getBatchSize() {
                        return httpMethods.size();
                    }
                }
            );
        }
    }

    private void storeSelectorChannelOperation(final String flowId, Set<FlowChannelSelector.Operation> operations, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " where flow_id = ?", flowId);
        }
        if (operations != null && !operations.isEmpty()) {
            Iterator<FlowChannelSelector.Operation> operationIterator = operations.iterator();
            jdbcTemplate.batchUpdate(
                "insert into " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " ( flow_id, channel_operation ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        FlowChannelSelector.Operation next = operationIterator.next();
                        ps.setString(1, flowId);
                        ps.setString(2, next.name());
                    }

                    @Override
                    public int getBatchSize() {
                        return operations.size();
                    }
                }
            );
        }
    }

    private enum FlowStepPhase {
        @Deprecated
        PRE,
        @Deprecated
        POST,
        REQUEST,
        RESPONSE,
        SUBSCRIBE,
        PUBLISH,
    }
}
