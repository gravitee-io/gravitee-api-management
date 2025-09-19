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

import static java.util.stream.Collectors.groupingBy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.management.model.JdbcScoringRow;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ScoringEnvironmentApi;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.management.model.ScoringReport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
class JdbcScoringReportRepository extends JdbcAbstractRepository<JdbcScoringRow> implements ScoringReportRepository {

    private final String SCORING_REPORT_SUMMARY;

    static final JdbcHelper.ChildAdder<JdbcScoringRow> CHILD_ADDER = (JdbcScoringRow parent, ResultSet rs) -> {
        if (parent.getSummary() == null) {
            parent.setSummary(
                new ScoringReport.Summary(
                    rs.getDouble("score"),
                    rs.getLong("errors"),
                    rs.getLong("warnings"),
                    rs.getLong("infos"),
                    rs.getLong("hints")
                )
            );
        }
        if (parent.getCreatedAt() == null) {
            parent.setCreatedAt(new Date(rs.getTimestamp("created_at").getTime()));
        }
    };

    JdbcScoringReportRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "scoring_reports");
        SCORING_REPORT_SUMMARY = getTableNameFor("scoring_report_summary");
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
                if (!a.diagnostics().isEmpty()) {
                    return a
                        .diagnostics()
                        .stream()
                        .map(diagnostic -> buildDiagnosticScoringRow(report, a.type(), diagnostic, pageId));
                } else if (!a.errors().isEmpty()) {
                    return a
                        .errors()
                        .stream()
                        .map(error -> buildErrorScoringRow(report, a.type(), error, pageId));
                } else {
                    return Stream.of(
                        new JdbcScoringRow(
                            report.getId(),
                            report.getApiId(),
                            report.getEnvironmentId(),
                            pageId,
                            a.type(),
                            report.getCreatedAt()
                        )
                    );
                }
            })
            .toList();

        storeSummary(report);
        for (var row : rows) {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(row));
        }

        return findByReportId(report.getId()).orElse(null);
    }

    private static JdbcScoringRow buildDiagnosticScoringRow(
        ScoringReport report,
        String assetType,
        ScoringReport.Diagnostic diagnostic,
        String pageId
    ) {
        return JdbcScoringRow.builder()
            .reportId(report.getId())
            .apiId(report.getApiId())
            .environmentId(report.getEnvironmentId())
            .pageId(pageId)
            .type(assetType)
            .severity(diagnostic.severity())
            .startLine(diagnostic.range().start().line())
            .startCharacter(diagnostic.range().start().character())
            .endLine(diagnostic.range().end().line())
            .endCharacter(diagnostic.range().end().character())
            .rule(diagnostic.rule())
            .message(diagnostic.message())
            .path(diagnostic.path())
            .rowType(JdbcScoringRow.RowType.DIAGNOSTIC)
            .build();
    }

    private static JdbcScoringRow buildErrorScoringRow(
        ScoringReport report,
        String assetType,
        ScoringReport.ScoringError error,
        String pageId
    ) {
        return JdbcScoringRow.builder()
            .reportId(report.getId())
            .apiId(report.getApiId())
            .environmentId(report.getEnvironmentId())
            .pageId(pageId)
            .type(assetType)
            .errorCode(error.code())
            .path(error.path().toString())
            .rowType(JdbcScoringRow.RowType.ERROR)
            .build();
    }

    @Override
    public void deleteByApi(String apiId) {
        log.debug("JdbcScoringRepository.deleteByApi({})", apiId);

        jdbcTemplate.update("delete from " + getOrm().getTableName() + " where api_id = ?", apiId);
        jdbcTemplate.update("delete from " + SCORING_REPORT_SUMMARY + " where api_id = ?", apiId);
    }

    public Optional<ScoringReport> findByReportId(String reportId) {
        log.debug("JdbcScoringRepository.findByReportId({})", reportId);

        var query =
            "select * from " +
            SCORING_REPORT_SUMMARY +
            " r left join " +
            getOrm().getTableName() +
            " s on r.report_id = s.report_id where r.report_id = ?";

        var rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
        jdbcTemplate.query(query, rowMapper, reportId);
        return adaptScoringReport(rowMapper.getRows());
    }

    @Override
    public Optional<ScoringReport> findLatestFor(String apiId) {
        log.debug("JdbcScoringRepository.findByApi({})", apiId);
        var query =
            "select * from " +
            SCORING_REPORT_SUMMARY +
            " r left join " +
            getOrm().getTableName() +
            " s on r.report_id = s.report_id where r.api_id = ?";

        var rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
        jdbcTemplate.query(query, rowMapper, apiId);
        return adaptScoringReport(rowMapper.getRows());
    }

    @Override
    public Stream<ScoringReport> findLatestReports(Collection<String> apiIds) {
        log.debug("JdbcScoringRepository.findLatestReports({})", apiIds);
        var query =
            "select * from " +
            SCORING_REPORT_SUMMARY +
            " s left join " +
            getOrm().getTableName() +
            " r on r.report_id = s.report_id " +
            " where s.api_id in (" +
            getOrm().buildInClause(apiIds) +
            ")";

        var rowMapper = new JdbcHelper.CollatingRowMapper<>(getOrm().getRowMapper(), CHILD_ADDER, "id");
        jdbcTemplate.query(
            query,
            (PreparedStatement ps) -> {
                getOrm().setArguments(ps, apiIds, 1);
            },
            rowMapper
        );

        var rowsByApiId = rowMapper.getRows().stream().collect(groupingBy(JdbcScoringRow::getApiId));

        return rowsByApiId
            .entrySet()
            .stream()
            .flatMap(entry -> adaptScoringReport(entry.getValue()).stream());
    }

    @Override
    public Page<ScoringEnvironmentApi> findEnvironmentLatestReports(String environmentId, Pageable pageable) throws TechnicalException {
        var query =
            "SELECT " +
            "a.id as api_id, " +
            "a.name as api_name, " +
            "a.updated_at as api_updated_at, " +
            "sr.report_id as report_id, " +
            "sr.created_at as report_created_at, " +
            "COALESCE(sr.score, 0) as score, " +
            "sr.errors as errors, " +
            "sr.warnings as warnings, " +
            "sr.infos as infos, " +
            "sr.hints as hints " +
            "FROM " +
            getTableNameFor("apis") +
            " a LEFT JOIN " +
            SCORING_REPORT_SUMMARY +
            " sr ON a.id = sr.api_id " +
            "WHERE a.environment_id = ? " +
            "ORDER BY score DESC";

        var result = jdbcTemplate.query(query, ENVIRONMENT_API_SUMMARY_MAPPER, environmentId);

        return JdbcAbstractPageableRepository.getResultAsPage(pageable, result);
    }

    @Override
    public ScoringEnvironmentSummary getScoringEnvironmentSummary(String environmentId) {
        var result = jdbcTemplate.query(
            "select " +
                "environment_id, " +
                "AVG(CASE WHEN score != -1 THEN score ELSE NULL END) AS averageScore," +
                "SUM(errors) AS totalErrors," +
                "SUM(warnings) AS totalWarnings," +
                "SUM(infos) AS totalInfos," +
                "SUM(hints) AS totalHints" +
                " from " +
                SCORING_REPORT_SUMMARY +
                " where " +
                " environment_id = ?" +
                " group by environment_id",
            ENVIRONMENT_SUMMARY_MAPPER,
            environmentId
        );

        if (result == null) {
            return new ScoringEnvironmentSummary(environmentId);
        }

        return result;
    }

    @Override
    protected JdbcObjectMapper<JdbcScoringRow> buildOrm() {
        return JdbcObjectMapper.builder(JdbcScoringRow.class, this.tableName, "report_id")
            .addColumn("report_id", Types.NVARCHAR, String.class)
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("page_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("severity", Types.NVARCHAR, String.class)
            .addColumn("start_line", Types.INTEGER, Integer.class)
            .addColumn("start_character", Types.INTEGER, Integer.class)
            .addColumn("end_line", Types.INTEGER, Integer.class)
            .addColumn("end_character", Types.INTEGER, Integer.class)
            .addColumn("rule", Types.NVARCHAR, String.class)
            .addColumn("message", Types.NVARCHAR, String.class)
            .addColumn("path", Types.NVARCHAR, String.class)
            .addColumn("error_code", Types.NVARCHAR, String.class)
            .addColumn("row_type", Types.NVARCHAR, JdbcScoringRow.RowType.class)
            .build();
    }

    private void storeSummary(ScoringReport report) {
        jdbcTemplate.batchUpdate(
            "insert into " +
                SCORING_REPORT_SUMMARY +
                " ( report_id, api_id, environment_id, created_at, score, errors, warnings, infos, hints ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? )",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, report.getId());
                    ps.setString(2, report.getApiId());
                    ps.setString(3, report.getEnvironmentId());
                    ps.setTimestamp(4, new java.sql.Timestamp(report.getCreatedAt().toInstant().toEpochMilli()));
                    ps.setDouble(5, report.getSummary().score());
                    ps.setLong(6, report.getSummary().errors());
                    ps.setLong(7, report.getSummary().warnings());
                    ps.setLong(8, report.getSummary().infos());
                    ps.setLong(9, report.getSummary().hints());
                }

                @Override
                public int getBatchSize() {
                    return 1;
                }
            }
        );
    }

    private static Optional<ScoringReport> adaptScoringReport(List<JdbcScoringRow> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        if (rows.size() == 1 && rows.get(0).getPageId() == null) {
            // no asset
            return Optional.of(
                new ScoringReport(
                    rows.get(0).getReportId(),
                    rows.get(0).getApiId(),
                    rows.get(0).getEnvironmentId(),
                    rows.get(0).getCreatedAt(),
                    rows.get(0).getSummary(),
                    List.of()
                )
            );
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
                    .filter(row -> row.getRowType() == JdbcScoringRow.RowType.DIAGNOSTIC)
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
                var errors = entry
                    .getValue()
                    .stream()
                    .filter(row -> row.getRowType() == JdbcScoringRow.RowType.ERROR)
                    .map(row ->
                        new ScoringReport.ScoringError(
                            row.getErrorCode(),
                            Arrays.asList(row.getPath().substring(1, row.getPath().length() - 1).split(", "))
                        )
                    )
                    .toList();

                return new ScoringReport.Asset(pageId, entry.getValue().get(0).getType(), diagnostics, errors);
            })
            .toList();

        return Optional.of(
            new ScoringReport(
                rows.get(0).getReportId(),
                rows.get(0).getApiId(),
                rows.get(0).getEnvironmentId(),
                rows.get(0).getCreatedAt(),
                rows.get(0).getSummary(),
                assets
            )
        );
    }

    private static final ResultSetExtractor<ScoringEnvironmentSummary> ENVIRONMENT_SUMMARY_MAPPER = rs -> {
        if (!rs.next()) {
            return null;
        }

        return ScoringEnvironmentSummary.builder()
            .environmentId(rs.getString(1))
            .score(BigDecimal.valueOf(rs.getDouble(2)).setScale(2, RoundingMode.HALF_EVEN).doubleValue())
            .errors(rs.getLong(3))
            .warnings(rs.getLong(4))
            .infos(rs.getLong(5))
            .hints(rs.getLong(6))
            .build();
    };

    private static final ResultSetExtractor<List<ScoringEnvironmentApi>> ENVIRONMENT_API_SUMMARY_MAPPER = rs -> {
        var result = new ArrayList<ScoringEnvironmentApi>();
        while (rs.next()) {
            var reportId = rs.getString("report_id");
            result.add(
                ScoringEnvironmentApi.builder()
                    .apiId(rs.getString("api_id"))
                    .apiName(rs.getString("api_name"))
                    .apiUpdatedAt(new Date(rs.getTimestamp("api_updated_at").getTime()))
                    .reportId(reportId)
                    .reportCreatedAt(reportId != null ? new Date(rs.getTimestamp("report_created_at").getTime()) : null)
                    .score(
                        reportId != null
                            ? BigDecimal.valueOf(rs.getDouble("score")).setScale(2, RoundingMode.HALF_EVEN).doubleValue()
                            : null
                    )
                    .errors(reportId != null ? rs.getLong("errors") : null)
                    .warnings(reportId != null ? rs.getLong("warnings") : null)
                    .infos(reportId != null ? rs.getLong("infos") : null)
                    .hints(reportId != null ? rs.getLong("hints") : null)
                    .build()
            );
        }
        return result;
    };
}
