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
import io.gravitee.repository.management.model.flow.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
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
    private final JdbcObjectMapper<FlowStep> stepsOrm;

    JdbcFlowRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "flows");
        FLOW_METHODS = getTableNameFor("flow_methods");
        FLOW_STEPS = getTableNameFor("flow_steps");
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
        flow.ifPresent(this::addMethods);
        flow.ifPresent(this::addSteps);
        flow.ifPresent(this::addConsumers);
        return flow;
    }

    private List<FlowStep> getPhaseSteps(String flowId, FlowStepPhase phase) {
        return jdbcTemplate.query(
            this.stepsOrm.getSelectAllSql() + " where flow_id = ? and phase = ? order by " + escapeReservedWord("order") + " asc",
            this.stepsOrm.getRowMapper(),
            flowId,
            phase.name()
        );
    }

    private void addSteps(Flow flow) {
        List<FlowStep> preSteps = getPhaseSteps(flow.getId(), FlowStepPhase.PRE);
        flow.setPre(preSteps);
        List<FlowStep> postSteps = getPhaseSteps(flow.getId(), FlowStepPhase.POST);
        flow.setPost(postSteps);
    }

    private Set<HttpMethod> getMethods(final String flowId) {
        return jdbcTemplate
            .queryForList("select method from " + FLOW_METHODS + " where flow_id = ?", String.class, flowId)
            .stream()
            .map(method -> HttpMethod.valueOf(method))
            .collect(Collectors.toSet());
    }

    private void addMethods(Flow flow) {
        Set<HttpMethod> methods = getMethods(flow.getId());
        flow.setMethods(methods);
    }

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

    @Override
    protected String getId(Flow item) {
        return item.getId();
    }

    @Override
    public Flow create(Flow flow) throws TechnicalException {
        storeMethods(flow, false);
        storeSteps(flow, false);
        storeConsumers(flow, false);
        return super.create(flow);
    }

    @Override
    public Flow update(Flow flow) throws TechnicalException {
        storeMethods(flow, true);
        storeSteps(flow, true);
        storeConsumers(flow, true);
        return super.update(flow);
    }

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

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + FLOW_METHODS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_STEPS + " where flow_id = ?", id);
        jdbcTemplate.update("delete from " + FLOW_CONSUMERS + " where flow_id = ?", id);
        super.delete(id);
    }

    private void storeMethods(Flow flow, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_METHODS + " where flow_id = ?", flow.getId());
        }
        if (flow.getMethods() != null && !flow.getMethods().isEmpty()) {
            Iterator<HttpMethod> methods = flow.getMethods().iterator();
            jdbcTemplate.batchUpdate(
                "insert into " + FLOW_METHODS + " ( flow_id, method ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        HttpMethod next = methods.next();
                        ps.setString(1, flow.getId());
                        ps.setString(2, next.name());
                    }

                    @Override
                    public int getBatchSize() {
                        return flow.getMethods().size();
                    }
                }
            );
        }
    }

    private void storeSteps(Flow flow, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + FLOW_STEPS + " where flow_id = ?", flow.getId());
        }
        storePhaseSteps(flow, flow.getPre(), FlowStepPhase.PRE);
        storePhaseSteps(flow, flow.getPost(), FlowStepPhase.POST);
    }

    private void storePhaseSteps(Flow flow, List<FlowStep> steps, FlowStepPhase phase) {
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

    @Override
    public Set<Flow> findAll() throws TechnicalException {
        Set<Flow> all = super.findAll();
        for (Flow flow : all) {
            this.addMethods(flow);
            this.addSteps(flow);
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
                .peek(this::addMethods)
                .peek(this::addSteps)
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
                jdbcTemplate.update("delete from " + FLOW_METHODS + " where flow_id in (" + buildInClause + ")", ids);
                jdbcTemplate.update("delete from " + FLOW_CONSUMERS + " where flow_id in (" + buildInClause + ")", ids);
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete flows by reference:", ex);
            throw new TechnicalException("Failed to delete flows by reference", ex);
        }
    }

    private void addConsumers(Flow flow) {
        List<FlowConsumer> consumers = getConsumers(flow.getId());
        flow.setConsumers(consumers);
    }

    private enum FlowStepPhase {
        PRE,
        POST,
    }
}
