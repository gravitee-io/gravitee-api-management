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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.management.model.JdbcScoringRow;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.repository.management.model.ScoringReport;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class JdbcScoringReportRepository extends JdbcAbstractRepository<JdbcScoringRow> implements ScoringReportRepository {

    JdbcScoringReportRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "scoring_reports");
    }

    @Override
    public ScoringReport create(final ScoringReport report) throws TechnicalException {
        log.debug("JdbcScoringRepository.create({})", report);

        if (report == null || report.getAssets() == null) {
            return null;
        }

        var rows = report
            .getAssets()
            .stream()
            .flatMap(a -> {
                var pageId = a.pageId() == null ? "" : a.pageId();
                if (a.diagnostics().isEmpty()) {
                    return Stream.of(new JdbcScoringRow(report.getId(), report.getApiId(), pageId, a.type(), report.getCreatedAt()));
                }
                return a
                    .diagnostics()
                    .stream()
                    .map(d ->
                        new JdbcScoringRow(
                            report.getId(),
                            report.getApiId(),
                            pageId,
                            a.type(),
                            report.getCreatedAt(),
                            d.severity(),
                            d.range().start().line(),
                            d.range().start().character(),
                            d.range().end().line(),
                            d.range().end().character(),
                            d.rule(),
                            d.message(),
                            d.path()
                        )
                    );
            })
            .toList();

        for (var row : rows) {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(row));
        }

        return findByReportId(report.getId()).orElse(null);
    }

    @Override
    public void deleteByApi(String apiId) {
        log.debug("JdbcScoringRepository.deleteByApi({})", apiId);

        jdbcTemplate.update("delete from " + getOrm().getTableName() + " where api_id = ?", apiId);
    }

    public Optional<ScoringReport> findByReportId(String reportId) {
        log.debug("JdbcScoringRepository.findByReportId({})", reportId);
        var rows = jdbcTemplate.query(getOrm().getSelectAllSql() + " where report_id = ?", getRowMapper(), reportId);
        return adaptScoringReport(rows);
    }

    @Override
    public Optional<ScoringReport> findLatestFor(String apiId) {
        log.debug("JdbcScoringRepository.findByApi({})", apiId);
        var rows = jdbcTemplate.query(getOrm().getSelectAllSql() + " where api_id = ?", getRowMapper(), apiId);
        return adaptScoringReport(rows);
    }

    @Override
    protected JdbcObjectMapper<JdbcScoringRow> buildOrm() {
        return JdbcObjectMapper
            .builder(JdbcScoringRow.class, this.tableName, "report_id")
            .addColumn("report_id", Types.NVARCHAR, String.class)
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("page_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("severity", Types.NVARCHAR, String.class)
            .addColumn("start_line", Types.INTEGER, Integer.class)
            .addColumn("start_character", Types.INTEGER, Integer.class)
            .addColumn("end_line", Types.INTEGER, Integer.class)
            .addColumn("end_character", Types.INTEGER, Integer.class)
            .addColumn("rule", Types.NVARCHAR, String.class)
            .addColumn("message", Types.NVARCHAR, String.class)
            .addColumn("path", Types.NVARCHAR, String.class)
            .build();
    }

    private static Optional<ScoringReport> adaptScoringReport(List<JdbcScoringRow> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        var grouped = rows.stream().collect(Collectors.groupingBy(JdbcScoringRow::getPageId));
        var assets = grouped
            .entrySet()
            .stream()
            .map(entry -> {
                var pageId = entry.getKey().isEmpty() ? null : entry.getKey();
                var diagnostics = entry
                    .getValue()
                    .stream()
                    .filter(row -> row.getSeverity() != null)
                    .map(row ->
                        new ScoringReport.Diagnostic(
                            row.getSeverity(),
                            new ScoringReport.Range(
                                new ScoringReport.Position(row.getStartLine(), row.getStartCharacter()),
                                new ScoringReport.Position(row.getEndLine(), row.getEndCharacter())
                            ),
                            row.getRule(),
                            row.getMessage(),
                            row.getPath()
                        )
                    )
                    .toList();

                return new ScoringReport.Asset(pageId, entry.getValue().get(0).getType(), diagnostics);
            })
            .toList();
        return Optional.of(new ScoringReport(rows.get(0).getReportId(), rows.get(0).getApiId(), rows.get(0).getCreatedAt(), assets));
    }
}
