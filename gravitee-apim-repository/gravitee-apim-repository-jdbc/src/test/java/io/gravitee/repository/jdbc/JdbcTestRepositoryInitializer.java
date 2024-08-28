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
package io.gravitee.repository.jdbc;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

import io.gravitee.repository.config.TestRepositoryInitializer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author njt
 */
@Component
public class JdbcTestRepositoryInitializer implements TestRepositoryInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTestRepositoryInitializer.class);
    private static final List<String> tablesToTruncate = List.of(
        "apis",
        "keys",
        "key_subscriptions",
        "subscriptions",
        "access_points",
        "api_groups",
        "api_headers",
        "api_labels",
        "api_tags",
        "api_categories",
        "applications",
        "application_groups",
        "application_metadata",
        "audits",
        "audit_properties",
        "commands",
        "command_acknowledgments",
        "command_tags",
        "client_registration_providers",
        "client_registration_provider_scopes",
        "custom_user_fields",
        "custom_user_fields_values",
        "dictionaries",
        "dictionary_property",
        "environments",
        "environment_hrids",
        "events",
        "event_properties",
        "event_environments",
        "event_organizations",
        "events_latest",
        "events_latest_properties",
        "events_latest_environments",
        "events_latest_organizations",
        "generic_notification_configs",
        "generic_notification_config_hooks",
        "groups",
        "group_event_rules",
        "identity_providers",
        "identity_provider_activations",
        "installation",
        "installation_informations",
        "licenses",
        "memberships",
        "metadata",
        "media",
        "notification_templates",
        "organizations",
        "organization_hrids",
        "parameters",
        "pages",
        "page_acl",
        "page_attached_media",
        "page_configuration",
        "page_revisions",
        "plans",
        "plan_characteristics",
        "plan_excluded_groups",
        "plan_tags",
        "portal_notifications",
        "portal_notification_configs",
        "portal_notification_config_hooks",
        "ratings",
        "rating_answers",
        "roles",
        "role_permissions",
        "tags",
        "tickets",
        "tenants",
        "users",
        "categories",
        "alert_triggers",
        "entrypoints",
        "page_metadata",
        "promotions",
        "invitations",
        "tag_groups",
        "workflows",
        "quality_rules",
        "api_quality_rules",
        "dashboards",
        "alert_events",
        "themes",
        "alert_event_rules",
        "tokens",
        "node_monitoring",
        "flows",
        "flow_steps",
        "flow_methods",
        "flow_consumers",
        "flow_selectors",
        "flow_selector_http_methods",
        "flow_selector_channel_operations",
        "flow_selector_channel_entrypoints",
        "flow_tags",
        "upgraders",
        "integrations",
        "integration_groups",
        "integrationjobs",
        "api_category_orders",
        "sharedpolicygroups",
        "sharedpolicygrouphistories",
        "portal_menu_links"
    );
    private static final List<String> tablesToDrop = concatenate(tablesToTruncate, List.of("databasechangelog", "databasechangeloglock"));
    private final DataSource dataSource;
    private final String prefix;
    private final String rateLimitPrefix;

    @Autowired
    public JdbcTestRepositoryInitializer(DataSource dataSource, Properties graviteeProperties) {
        LOGGER.debug("Constructed");
        this.dataSource = dataSource;
        this.prefix = graviteeProperties.getProperty("management.jdbc.prefix", "");
        this.rateLimitPrefix = graviteeProperties.getProperty("ratelimit.jdbc.prefix", "");
        final JdbcTemplate jt = new JdbcTemplate(dataSource);
        for (String table : tablesToDrop) {
            LOGGER.debug("Dropping {}", table);
            jt.execute("drop table if exists " + escapeReservedWord(prefix + table));
        }
        jt.execute("drop table if exists " + escapeReservedWord(rateLimitPrefix + "ratelimit"));
    }

    private static <T> List<T> concatenate(List<T> first, List<T> second) {
        final List<T> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    @Override
    public void setUp() {
        LOGGER.debug("setUp");
        LOGGER.debug("Running Liquibase on {}", dataSource);

        System.setProperty("liquibase.databaseChangeLogTableName", prefix + "databasechangelog");
        System.setProperty("liquibase.databaseChangeLogLockTableName", prefix + "databasechangeloglock");
        System.setProperty("gravitee_prefix", prefix);
        System.setProperty("gravitee_rate_limit_prefix", rateLimitPrefix);

        try (final Connection conn = dataSource.getConnection()) {
            final Liquibase liquibase = new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(conn)
            );
            liquibase.update((Contexts) null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to set up database: ", ex);
        }
    }

    @Override
    public void tearDown() {
        LOGGER.debug("tearDown");
        final JdbcTemplate jt = new JdbcTemplate(dataSource);
        System.clearProperty("liquibase.databaseChangeLogTableName");
        System.clearProperty("liquibase.databaseChangeLogLockTableName");
        System.clearProperty("gravitee_prefix");
        for (final String table : tablesToTruncate) {
            jt.update("delete from " + escapeReservedWord(prefix + table));
        }
        jt.update("delete from " + escapeReservedWord(rateLimitPrefix + "ratelimit"));
    }
}
