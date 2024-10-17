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
package io.gravitee.repository.jdbc.management;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcFlowRepository extends JdbcAbstractCrudRepository<Flow, String> implements FlowRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcFlowRepository.class);
    private final String FLOWS;
    private final String FLOW_STEPS;
    private final String FLOW_METHODS;
    private final String FLOW_CONSUMERS;
    private final String FLOW_SELECTORS;
    private final String FLOW_SELECTOR_HTTP_METHODS;
    private final String FLOW_SELECTOR_CHANNEL_OPERATIONS;
    private final String FLOW_SELECTOR_CHANNEL_ENTRYPOINTS;
    private final String FLOW_TAGS;
    private final JdbcObjectMapper<FlowStep> stepsOrm;

    JdbcFlowRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "flows");
        FLOWS = getTableNameFor("flows");
        FLOW_METHODS = getTableNameFor("flow_methods");
        FLOW_STEPS = getTableNameFor("flow_steps");
        FLOW_SELECTORS = getTableNameFor("flow_selectors");
        FLOW_SELECTOR_HTTP_METHODS = getTableNameFor("flow_selector_http_methods");
        FLOW_SELECTOR_CHANNEL_OPERATIONS = getTableNameFor("flow_selector_channel_operations");
        FLOW_SELECTOR_CHANNEL_ENTRYPOINTS = getTableNameFor("flow_selector_channel_entrypoints");
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
                .addColumn("message_condition", Types.NVARCHAR, String.class)
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
        jdbcTemplate.update("delete from " + FLOW_SELECTOR_CHANNEL_ENTRYPOINTS + " where flow_id = ?", id);
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
        LOGGER.debug("JdbcFlowRepository.findByReference({}, {})", referenceType, referenceId);

        try {
            StringBuilder selectQueryBuilder = new StringBuilder("select");
            selectQueryBuilder.append(" f.id as \"flows.id\",");
            selectQueryBuilder.append(" f.").append(escapeReservedWord("condition")).append(" as \"flows.condition\",");
            selectQueryBuilder.append(" f.created_at as \"flows.createdAt\",");
            selectQueryBuilder.append(" f.enabled as \"flows.enabled\",");
            selectQueryBuilder.append(" f.name as \"flows.name\",");
            selectQueryBuilder.append(" f.path as \"flows.path\",");
            selectQueryBuilder.append(" f.operator as \"flows.operator\",");
            selectQueryBuilder.append(" f.reference_id as \"flows.referenceId\",");
            selectQueryBuilder.append(" f.reference_type as \"flows.referenceType\",");
            selectQueryBuilder.append(" f.updated_at as \"flows.updatedAt\",");
            selectQueryBuilder.append(" f.").append(escapeReservedWord("order")).append(" as \"flows.order\",");
            selectQueryBuilder.append(" fs.configuration as \"flowSteps.configuration\",");
            selectQueryBuilder.append(" fs.description as \"flowSteps.description\",");
            selectQueryBuilder.append(" fs.enabled as \"flowSteps.enabled\",");
            selectQueryBuilder.append(" fs.name as \"flowSteps.name\",");
            selectQueryBuilder.append(" fs.policy as \"flowSteps.policy\",");
            selectQueryBuilder.append(" fs.").append(escapeReservedWord("order")).append(" as \"flowSteps.order\",");
            selectQueryBuilder.append(" fs.phase as \"flowSteps.phase\",");
            selectQueryBuilder.append(" fs.").append(escapeReservedWord("condition")).append(" as \"flowSteps.condition\",");
            selectQueryBuilder.append(" fs.id as \"flowSteps.id\",");
            selectQueryBuilder.append(" fs.message_condition as \"flowSteps.messageCondition\",");
            selectQueryBuilder.append(" fse.type as \"flowSelectors.type\",");
            selectQueryBuilder.append(" fse.path as \"flowSelectors.path\",");
            selectQueryBuilder.append(" fse.path_operator as \"flowSelectors.pathOperator\",");
            selectQueryBuilder.append(" fse.").append(escapeReservedWord("condition")).append(" as \"flowSelectors.condition\",");
            selectQueryBuilder.append(" fse.channel as \"flowSelectors.channel\",");
            selectQueryBuilder.append(" fse.channel_operator as \"flowSelectors.channelOperator\",");
            selectQueryBuilder.append(" ft.tag as \"flowTags.tag\",");
            selectQueryBuilder.append(" fm.method as \"flowMethods.method\",");
            selectQueryBuilder.append(" fc.consumer_type as \"flowConsumers.consumerType\",");
            selectQueryBuilder.append(" fc.consumer_id as \"flowConsumers.consumerId\",");
            selectQueryBuilder.append(" fsce.channel_entrypoint as \"flowSelectorChannelEntrypoints.channelEntrypoint\",");
            selectQueryBuilder.append(" fsco.channel_operation as \"flowSelectorChannelOperations.channelOperation\",");
            selectQueryBuilder.append(" fshm.method as \"flowSelectorHttpMethods.method\"");
            selectQueryBuilder.append(" from ").append(FLOWS).append(" f");
            selectQueryBuilder.append(" left join ").append(FLOW_STEPS).append(" fs on f.id = fs.flow_id");
            selectQueryBuilder.append(" left join ").append(FLOW_SELECTORS).append(" fse on f.id = fse.flow_id");
            selectQueryBuilder
                .append(" left join ")
                .append(FLOW_SELECTOR_CHANNEL_ENTRYPOINTS)
                .append(" fsce on f.id = fsce.flow_id and fse.type = 'CHANNEL'");
            selectQueryBuilder
                .append(" left join ")
                .append(FLOW_SELECTOR_CHANNEL_OPERATIONS)
                .append(" fsco on f.id = fsco.flow_id  and fse.type = 'CHANNEL'");
            selectQueryBuilder
                .append(" left join ")
                .append(FLOW_SELECTOR_HTTP_METHODS)
                .append(" fshm on f.id = fshm.flow_id  and fse.type = 'HTTP'");
            selectQueryBuilder.append(" left join ").append(FLOW_TAGS).append(" ft on f.id = ft.flow_id");
            selectQueryBuilder.append(" left join ").append(FLOW_METHODS).append(" fm on f.id = fm.flow_id");
            selectQueryBuilder.append(" left join ").append(FLOW_CONSUMERS).append(" fc on f.id = fc.flow_id");
            selectQueryBuilder.append(" where f.reference_id = ? and f.reference_type = ?");
            selectQueryBuilder
                .append(" order by f.id, fs.phase, fs.")
                .append(escapeReservedWord("order"))
                .append(", fm.method, ft.tag, fc.consumer_id, fse.type, fsce.channel_entrypoint, fsco.channel_operation asc");

            SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(selectQueryBuilder.toString(), referenceId, referenceType.name());
            return computeFlowList(sqlRowSet);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find flows by reference:", ex);
            throw new TechnicalException("Failed to find flows by reference", ex);
        }
    }

    private List<Flow> computeFlowList(SqlRowSet rs) {
        Map<String, Flow> flowsById = new HashMap<>();
        Map<String, FlowStep> flowStepsById = new HashMap<>();
        Map<String, FlowSelector> flowSelectorsByFlowId = new HashMap<>();

        while (rs.next()) {
            // get flow from cache or create it
            String flowId = rs.getString("flows.id");
            if (flowId == null || flowId.isEmpty()) {
                return null;
            }
            Flow flow = flowsById.computeIfAbsent(
                flowId,
                key -> {
                    Flow newFlow = new Flow();
                    newFlow.setId(key);
                    newFlow.setCondition(rs.getString("flows.condition"));
                    newFlow.setCreatedAt(rs.getDate("flows.createdAt"));
                    newFlow.setEnabled(rs.getBoolean("flows.enabled"));
                    newFlow.setName(rs.getString("flows.name"));
                    newFlow.setPath(rs.getString("flows.path"));
                    String operator = rs.getString("flows.operator");
                    if (operator != null) {
                        newFlow.setOperator(FlowOperator.valueOf(operator));
                    }
                    newFlow.setReferenceId(rs.getString("flows.referenceId"));
                    newFlow.setReferenceType(FlowReferenceType.valueOf(rs.getString("flows.referenceType")));
                    newFlow.setUpdatedAt(rs.getDate("flows.updatedAt"));
                    newFlow.setOrder(rs.getInt("flows.order"));
                    newFlow.setConsumers(new ArrayList<>());
                    newFlow.setPre(new ArrayList<>());
                    newFlow.setPost(new ArrayList<>());
                    newFlow.setRequest(new ArrayList<>());
                    newFlow.setResponse(new ArrayList<>());
                    newFlow.setPublish(new ArrayList<>());
                    newFlow.setSubscribe(new ArrayList<>());
                    newFlow.setInteract(new ArrayList<>());
                    newFlow.setConnect(new ArrayList<>());
                    newFlow.setMethods(new HashSet<>());
                    newFlow.setTags(new HashSet<>());
                    newFlow.setSelectors(new ArrayList<>());
                    return newFlow;
                }
            );

            // create step and add it to right phase if not already added
            String flowStepId = rs.getString("flowSteps.id");
            if (flowStepId != null && !flowStepId.isEmpty()) {
                flowStepsById.computeIfAbsent(
                    flowStepId,
                    key -> {
                        FlowStep newFlowStep = new FlowStep();
                        newFlowStep.setPolicy(rs.getString("flowSteps.policy"));
                        newFlowStep.setDescription(rs.getString("flowSteps.description"));
                        newFlowStep.setEnabled(rs.getBoolean("flowSteps.enabled"));
                        newFlowStep.setName(rs.getString("flowSteps.name"));
                        newFlowStep.setConfiguration(rs.getString("flowSteps.configuration"));
                        newFlowStep.setOrder(rs.getInt("flowSteps.order"));
                        newFlowStep.setCondition(rs.getString("flowSteps.condition"));
                        newFlowStep.setMessageCondition(rs.getString("flowSteps.messageCondition"));

                        FlowStepPhase phase = FlowStepPhase.valueOf(rs.getString("flowSteps.phase"));
                        List<FlowStep> steps;
                        switch (phase) {
                            case REQUEST:
                                steps = flow.getRequest();
                                break;
                            case RESPONSE:
                                steps = flow.getResponse();
                                break;
                            case SUBSCRIBE:
                                steps = flow.getSubscribe();
                                break;
                            case PUBLISH:
                                steps = flow.getPublish();
                                break;
                            // deprecated data
                            case PRE:
                                steps = flow.getPre();
                                break;
                            case POST:
                                steps = flow.getPost();
                                break;
                            case INTERACT:
                                steps = flow.getInteract();
                                break;
                            case CONNECT:
                                steps = flow.getConnect();
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + phase);
                        }

                        steps.add(newFlowStep);
                        return newFlowStep;
                    }
                );
            }

            // add method to flow if not already added
            String flowMethod = rs.getString("flowMethods.method");
            if (flowMethod != null && !flowMethod.isEmpty()) {
                Set<HttpMethod> flowMethods = flow.getMethods();
                flowMethods.add(HttpMethod.valueOf(flowMethod));
            }

            // add tag to flow if not already added
            String flowTag = rs.getString("flowTags.tag");
            if (flowTag != null && !flowTag.isEmpty()) {
                Set<String> flowTags = flow.getTags();
                flowTags.add(flowTag);
            }

            // add consumer (type and id) to flow if not already added
            String consumerId = rs.getString("flowConsumers.consumerId");
            String consumerType = rs.getString("flowConsumers.consumerType");
            if (consumerId != null && !consumerId.isEmpty() && consumerType != null && !consumerType.isEmpty()) {
                FlowConsumer flowConsumer = new FlowConsumer();
                flowConsumer.setConsumerId(consumerId);
                flowConsumer.setConsumerType(FlowConsumerType.valueOf(consumerType));

                List<FlowConsumer> consumers = flow.getConsumers();
                boolean consumerAlreadyAdded = consumers.stream().anyMatch(c -> c.getConsumerId().equals(consumerId));
                if (!consumerAlreadyAdded) {
                    consumers.add(flowConsumer);
                }
            }

            // get or create selector
            String flowSelectorType = rs.getString("flowSelectors.type");
            if (flowSelectorType != null && !flowSelectorType.isEmpty()) {
                FlowSelector flowSelector = flowSelectorsByFlowId.computeIfAbsent(
                    flowId + flowSelectorType,
                    key -> {
                        FlowSelectorType selectorType = FlowSelectorType.valueOf(flowSelectorType);
                        switch (selectorType) {
                            case HTTP:
                                FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
                                flowHttpSelector.setPath(rs.getString("flowSelectors.path"));
                                flowHttpSelector.setPathOperator(FlowOperator.valueOf(rs.getString("flowSelectors.pathOperator")));
                                flowHttpSelector.setMethods(new HashSet<>());
                                flow.getSelectors().add(flowHttpSelector);
                                return flowHttpSelector;
                            case CONDITION:
                                FlowConditionSelector flowConditionSelector = new FlowConditionSelector();
                                flowConditionSelector.setCondition(rs.getString("flowSelectors.condition"));
                                flow.getSelectors().add(flowConditionSelector);
                                return flowConditionSelector;
                            case CHANNEL:
                                FlowChannelSelector flowChannelSelector = new FlowChannelSelector();
                                flowChannelSelector.setChannel(rs.getString("flowSelectors.channel"));
                                flowChannelSelector.setChannelOperator(FlowOperator.valueOf(rs.getString("flowSelectors.channelOperator")));
                                flowChannelSelector.setEntrypoints(new HashSet<>());
                                flowChannelSelector.setOperations(new HashSet<>());
                                flow.getSelectors().add(flowChannelSelector);
                                return flowChannelSelector;
                            default:
                                return null;
                        }
                    }
                );
                if (flowSelector != null && flowSelector.getType() == FlowSelectorType.CHANNEL) {
                    FlowChannelSelector flowChannelSelector = (FlowChannelSelector) flowSelector;
                    Set<FlowChannelSelector.Operation> operations = flowChannelSelector.getOperations();
                    String channelOperation = rs.getString("flowSelectorChannelOperations.channelOperation");
                    if (channelOperation != null && !channelOperation.isEmpty()) {
                        operations.add(FlowChannelSelector.Operation.valueOf(channelOperation));
                    }

                    Set<String> entrypoints = flowChannelSelector.getEntrypoints();
                    String channelEntrypoint = rs.getString("flowSelectorChannelEntrypoints.channelEntrypoint");
                    if (channelEntrypoint != null && !channelEntrypoint.isEmpty()) {
                        entrypoints.add(channelEntrypoint);
                    }
                } else if (flowSelector != null && flowSelector.getType() == FlowSelectorType.HTTP) {
                    FlowHttpSelector flowHttpSelector = (FlowHttpSelector) flowSelector;
                    Set<HttpMethod> methods = flowHttpSelector.getMethods();
                    String method = rs.getString("flowSelectorHttpMethods.method");
                    if (method != null && !method.isEmpty()) {
                        methods.add(HttpMethod.valueOf(method));
                    }
                }
            }
        }
        return new ArrayList<>(flowsById.values());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, FlowReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcFlowRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceType, referenceId);
        try {
            final var flows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );
            this.deleteAllById(flows);
            LOGGER.debug("JdbcFlowRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceType, referenceId);
            return flows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete flows for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete flows by reference", ex);
        }
    }

    @Override
    public void deleteAllById(Collection<String> ids) throws TechnicalException {
        LOGGER.debug("JdbcFlowRepository.deleteByIds({})", ids);
        try {
            if (!ids.isEmpty()) {
                String buildInClause = getOrm().buildInClause(ids);
                String[] flowIds = ids.toArray(new String[0]);
                jdbcTemplate.update("delete from " + tableName + " where id in (" + buildInClause + ")", flowIds);
                jdbcTemplate.update("delete from " + FLOW_STEPS + " where flow_id in (" + buildInClause + ")", flowIds);
                jdbcTemplate.update("delete from " + FLOW_SELECTORS + " where flow_id in (" + buildInClause + ")", flowIds);
                jdbcTemplate.update("delete from " + FLOW_SELECTOR_HTTP_METHODS + " where flow_id in (" + buildInClause + ")", flowIds);
                jdbcTemplate.update(
                    "delete from " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " where flow_id in (" + buildInClause + ")",
                    flowIds
                );
                jdbcTemplate.update(
                    "delete from " + FLOW_SELECTOR_CHANNEL_ENTRYPOINTS + " where flow_id in (" + buildInClause + ")",
                    flowIds
                );
                jdbcTemplate.update("delete from " + FLOW_TAGS + " where flow_id in (" + buildInClause + ")", flowIds);
                // deprecated data
                jdbcTemplate.update("delete from " + FLOW_METHODS + " where flow_id in (" + buildInClause + ")", flowIds);
                jdbcTemplate.update("delete from " + FLOW_CONSUMERS + " where flow_id in (" + buildInClause + ")", flowIds);
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
            .peek(flowSelector -> {
                if (flowSelector.getType() == FlowSelectorType.HTTP) {
                    FlowHttpSelector flowHttpSelector = (FlowHttpSelector) flowSelector;
                    Set<HttpMethod> methods = getSelectorHttpMethods(flowId);
                    flowHttpSelector.setMethods(methods);
                } else if (flowSelector.getType() == FlowSelectorType.CHANNEL) {
                    FlowChannelSelector flowChannelSelector = (FlowChannelSelector) flowSelector;
                    flowChannelSelector.setOperations(getChannelOperations(flowId));
                    flowChannelSelector.setEntrypoints(getChannelEntrypoints(flowId));
                }
            })
            .collect(Collectors.toList());
    }

    private Set<FlowChannelSelector.Operation> getChannelOperations(final String flowId) {
        return jdbcTemplate
            .queryForList("select channel_operation from " + FLOW_SELECTOR_CHANNEL_OPERATIONS + " where flow_id = ?", String.class, flowId)
            .stream()
            .map(FlowChannelSelector.Operation::valueOf)
            .collect(Collectors.toSet());
    }

    private Set<String> getChannelEntrypoints(final String flowId) {
        return jdbcTemplate
            .queryForList(
                "select channel_entrypoint from " + FLOW_SELECTOR_CHANNEL_ENTRYPOINTS + " where flow_id = ?",
                String.class,
                flowId
            )
            .stream()
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
        List<FlowStep> interactSteps = getPhaseSteps(flow.getId(), FlowStepPhase.INTERACT);
        flow.setInteract(interactSteps);
        List<FlowStep> connectSteps = getPhaseSteps(flow.getId(), FlowStepPhase.CONNECT);
        flow.setConnect(connectSteps);

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
        storePhaseSteps(flow, flow.getInteract(), FlowStepPhase.INTERACT);
        storePhaseSteps(flow, flow.getConnect(), FlowStepPhase.CONNECT);
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
                ", phase, message_condition ) values ( ?, ?, ?, ?, ? , ?, ?, ?, ?, ?)",
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
                        ps.setString(10, flowStep.getMessageCondition());
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
            selectors.forEach(flowSelector -> {
                if (flowSelector instanceof FlowHttpSelector) {
                    FlowHttpSelector flowHttpSelector = (FlowHttpSelector) flowSelector;
                    storeSelectorHttpMethods(flowId, flowHttpSelector.getMethods(), deleteFirst);
                } else if (flowSelector instanceof FlowChannelSelector) {
                    FlowChannelSelector flowChannelSelector = (FlowChannelSelector) flowSelector;
                    storeSelectorChannelOperations(flowId, flowChannelSelector.getOperations(), deleteFirst);
                    storeSelectorChannelEntrypoints(flowId, flowChannelSelector.getEntrypoints(), deleteFirst);
                }
            });
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

    private void storeSelectorChannelOperations(final String flowId, Set<FlowChannelSelector.Operation> operations, boolean deleteFirst) {
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

    private void storeSelectorChannelEntrypoints(final String flowId, Set<String> entrypoints, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_SELECTOR_CHANNEL_ENTRYPOINTS + " where flow_id = ?", flowId);
        }
        if (entrypoints != null && !entrypoints.isEmpty()) {
            Iterator<String> operationIterator = entrypoints.iterator();
            jdbcTemplate.batchUpdate(
                "insert into " + FLOW_SELECTOR_CHANNEL_ENTRYPOINTS + " ( flow_id, channel_entrypoint ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        String next = operationIterator.next();
                        ps.setString(1, flowId);
                        ps.setString(2, next);
                    }

                    @Override
                    public int getBatchSize() {
                        return entrypoints.size();
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
        INTERACT,
        CONNECT,
    }
}
