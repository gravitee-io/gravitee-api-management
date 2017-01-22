/**
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
package io.gravitee.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.io.Resources;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Policy;
import io.gravitee.management.model.*;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiMembershipTypeFilter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_ExportAsJsonTest {

    private static final String API_ID = "id-api";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private GroupService groupService;

    @Before
    public void setUp() throws TechnicalException {
        PropertyFilter apiMembershipTypeFilter = new ApiMembershipTypeFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));

        Api api = new Api();
        api.setId(API_ID);
        api.setDescription("Gravitee.io");
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PageEntity page = new PageEntity();
        page.setName("My Title");
        page.setOrder(1);
        page.setType(PageType.MARKDOWN.toString());
        page.setContent("Read the doc");
        when(pageService.findByApi(API_ID)).thenReturn(Collections.singletonList(new PageListItem()));
        when(pageService.findById(any())).thenReturn(page);
        Membership membership = new Membership();
        membership.setUserId("johndoe");
        membership.setReferenceId(API_ID);
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setType(MembershipType.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferenceAndMembershipType(eq(MembershipReferenceType.API), eq(API_ID), any()))
                .thenReturn(Collections.singleton(membership));
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setUsername(membership.getUserId());
        memberEntity.setType(MembershipType.valueOf(membership.getType()));
        when(membershipService.getMembers(eq(MembershipReferenceType.API), eq(API_ID)))
                .thenReturn(Collections.singleton(memberEntity));
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(memberEntity.getUsername());
        when(userService.findByName(memberEntity.getUsername())).thenReturn(userEntity);

        api.setGroup("my-group");
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId("my-group");
        groupEntity.setName("My Group");
        groupEntity.setType(GroupEntityType.API);
        when(groupService.findById("my-group")).thenReturn(groupEntity);

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("plan-id");
        publishedPlan.setApis(Collections.singleton(API_ID));
        publishedPlan.setDescription("free plan");
        publishedPlan.setType(PlanType.API);
        publishedPlan.setSecurity(PlanSecurityType.API_KEY);
        publishedPlan.setValidation(PlanValidationType.AUTO);
        publishedPlan.setStatus(PlanStatus.PUBLISHED);
        Map<String, Path> paths = new HashMap<>();
        Path path = new Path();
        path.setPath("/");
        io.gravitee.definition.model.Rule rule = new io.gravitee.definition.model.Rule();
        rule.setEnabled(true);
        rule.setMethods(Collections.singletonList(HttpMethod.GET));
        Policy policy = new Policy();
        policy.setName("rate-limit");
        policy.setConfiguration("{\n" +
                "          \"rate\": {\n" +
                "            \"limit\": 1,\n" +
                "            \"periodTime\": 1,\n" +
                "            \"periodTimeUnit\": \"SECONDS\"\n" +
                "          }\n" +
                "        }");
        rule.setPolicy(policy);
        path.setRules(Collections.singletonList(rule));
        paths.put("/", path);
        publishedPlan.setPaths(paths);
        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("closedPlan-id");
        closedPlan.setApis(Collections.singleton(API_ID));
        closedPlan.setDescription("free closedPlan");
        closedPlan.setType(PlanType.API);
        closedPlan.setSecurity(PlanSecurityType.API_KEY);
        closedPlan.setValidation(PlanValidationType.AUTO);
        closedPlan.setStatus(PlanStatus.CLOSED);
        closedPlan.setPaths(paths);
        Set<PlanEntity> set = new HashSet<>();
        set.add(publishedPlan);
        set.add(closedPlan);
        when(planService.findByApi(API_ID)).thenReturn(set);
    }

    @Test
    public void shouldConvertAsJsonForExport() throws TechnicalException, IOException {

        String jsonForExport = apiService.exportAsJson(API_ID, MembershipType.PRIMARY_OWNER);

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExport.json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(jsonForExport).isEqualTo(expectedJson);
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers() throws IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, MembershipType.PRIMARY_OWNER, "members");

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportWithoutMembers.json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(jsonForExport).isEqualTo(expectedJson);
    }

    @Test
    public void shouldConvertAsJsonWithoutPages() throws IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, MembershipType.PRIMARY_OWNER, "pages");

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportWithoutPages.json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(jsonForExport).isEqualTo(expectedJson);
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans() throws IOException {
        String jsonForExport = apiService.exportAsJson(API_ID, MembershipType.PRIMARY_OWNER, "plans");

        URL url =  Resources.getResource("io/gravitee/management/service/export-convertAsJsonForExportWithoutPlans.json");
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(jsonForExport).isEqualTo(expectedJson);
    }
}
