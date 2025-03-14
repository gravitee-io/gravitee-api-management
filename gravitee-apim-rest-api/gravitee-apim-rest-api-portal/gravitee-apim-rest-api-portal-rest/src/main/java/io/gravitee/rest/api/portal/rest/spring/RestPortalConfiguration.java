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
package io.gravitee.rest.api.portal.rest.spring;

import io.gravitee.rest.api.portal.rest.mapper.AlertMapper;
import io.gravitee.rest.api.portal.rest.mapper.AnalyticsMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.mapper.ConfigurationMapper;
import io.gravitee.rest.api.portal.rest.mapper.ConnectorMapper;
import io.gravitee.rest.api.portal.rest.mapper.DashboardMapper;
import io.gravitee.rest.api.portal.rest.mapper.IdentityProviderMapper;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.LogMapper;
import io.gravitee.rest.api.portal.rest.mapper.MemberMapper;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.mapper.PortalMenuLinkMapper;
import io.gravitee.rest.api.portal.rest.mapper.PortalNotificationMapper;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.mapper.ReferenceMetadataMapper;
import io.gravitee.rest.api.portal.rest.mapper.ThemeMapper;
import io.gravitee.rest.api.portal.rest.mapper.TicketMapper;
import io.gravitee.rest.api.portal.rest.mapper.UserMapper;
import io.gravitee.rest.api.portal.security.SecurityPortalConfiguration;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.rest.api.portal.rest.mapper", // please prefer using Mapstruct mappers and not declare new manual mappers here
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            AlertMapper.class,
            AnalyticsMapper.class,
            ApiMapper.class,
            ApplicationMapper.class,
            CategoryMapper.class,
            ConfigurationMapper.class,
            ConnectorMapper.class,
            DashboardMapper.class,
            IdentityProviderMapper.class,
            KeyMapper.class,
            LogMapper.class,
            MemberMapper.class,
            PageMapper.class,
            PlanMapper.class,
            PortalMenuLinkMapper.class,
            PortalNotificationMapper.class,
            RatingMapper.class,
            ReferenceMetadataMapper.class,
            ThemeMapper.class,
            TicketMapper.class,
            UserMapper.class,
        }
    )
)
@Import({ ServiceConfiguration.class, SecurityPortalConfiguration.class })
@EnableAsync
public class RestPortalConfiguration {}
