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
package io.gravitee.repository.elasticsearch.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.analytics.ElasticsearchAnalyticsRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.monitoring.MonitoringRepository;
import io.gravitee.repository.monitoring.model.MonitoringResponse;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume Waignier (zenika)
 * @author Sebastien Devaux (zenika)
 */
public class ElasticsearchMonitoringRepository extends AbstractElasticsearchRepository implements MonitoringRepository {

	 /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ElasticsearchAnalyticsRepository.class);
    
	/**
	 * Name of the FreeMarker template used to query monitoring document types.
	 */
    private final static String MONITORING_TEMPLATE = "monitor/monitoringRequest.ftl";

    private final static String FIELD_GATEWAY_NAME = "gateway";
    private final static String FIELD_TIMESTAMP = "@timestamp";
    private final static String FIELD_HOSTNAME = "hostname";

    private final static String FIELD_JVM = "jvm";
    private final static String FIELD_PROCESS = "process";
    private final static String FIELD_OS = "os";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected RepositoryConfiguration configuration;

    @Override
    public MonitoringResponse query(final String gatewayId) {
    	final String sQuery = this.createElasticsearchJsonQuery(gatewayId);
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);

        try {
            final Single<SearchResponse> result = this.client.search(
                    this.indexNameGenerator.getTodayIndexName(Type.MONITOR, clusters),
                    !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.MONITOR.getType(),
                    sQuery);

            final SearchHits hits = result.blockingGet().getSearchHits();
            if (hits != null && hits.getHits().size() > 0) {
                return this.convert(hits.getHits().get(0).getSource());
            }
        } catch(Exception eex) {
        	logger.error("Impossible to make query for monitoring", eex);
        	return null;
        }
        //TODO return null?
        return null;
    }

    /**
     * Create JSON Elasticsearch query for the monitoring
     * @param gatewayId id of the gateway
     * @return JSON Elasticsearch query
     */
	private String createElasticsearchJsonQuery(final String gatewayId) {
		final Map<String, Object> data = new HashMap<>();
		data.put("gateway", gatewayId);

		return this.freeMarkerComponent.generateFromTemplate(MONITORING_TEMPLATE, data);
	}

    /**
     * Convert the raw Elasticsearch response
     * @param source Raw json elasticsearch response
     * @return monitoring result
     */
    @SuppressWarnings("unchecked")
    private MonitoringResponse convert(final JsonNode source) {

        final MonitoringResponse monitoringResponse = new MonitoringResponse();
        monitoringResponse.setGatewayId(source.get(FIELD_GATEWAY_NAME).asText());
        monitoringResponse.setTimestamp(ZonedDateTime.parse(source.get(FIELD_TIMESTAMP).asText()));
        monitoringResponse.setHostname(source.get(FIELD_HOSTNAME).asText());

        // OS
        final JsonNode os = source.get(FIELD_OS);

        final JsonNode cpu = os.get("cpu");
        monitoringResponse.setOsCPUPercent(cpu.get("percent").asInt());
        monitoringResponse.setOsCPULoadAverage((objectMapper.convertValue(cpu.get("load_average"), Map.class)));

        final JsonNode osMem = os.get("mem");
        monitoringResponse.setOsMemUsedInBytes(osMem.get("used_in_bytes").asLong());
        monitoringResponse.setOsMemFreeInBytes(osMem.get("free_in_bytes").asLong());
        monitoringResponse.setOsMemTotalInBytes(osMem.get("total_in_bytes").asLong());
        monitoringResponse.setOsMemUsedPercent(osMem.get("used_percent").asInt());
        monitoringResponse.setOsMemFreePercent(osMem.get("free_percent").asInt());

        // Process

        final JsonNode process = source.get(FIELD_PROCESS);
        final JsonNode processCpu = process.get("cpu");
        monitoringResponse.setProcessCPUPercent(processCpu.get("percent").asInt());
        monitoringResponse.setProcessOpenFileDescriptors(process.get("open_file_descriptors").asInt());
        monitoringResponse.setProcessMaxFileDescriptors(process.get("max_file_descriptors").asInt());

        // JVM

        final JsonNode jvm = source.get(FIELD_JVM);

        monitoringResponse.setJvmUptimeInMillis(jvm.get("uptime_in_millis").asLong());
        monitoringResponse.setJvmTimestamp(jvm.get("timestamp").asLong());

        final JsonNode jvmMem =  jvm.get("mem");
        monitoringResponse.setJvmHeapCommittedInBytes(jvmMem.get("heap_committed_in_bytes").asLong());
        monitoringResponse.setJvmHeapUsedPercent(jvmMem.get("heap_used_percent").asInt());
        monitoringResponse.setJvmHeapMaxInBytes(jvmMem.get("heap_max_in_bytes").asLong());
        monitoringResponse.setJvmNonHeapCommittedInBytes(jvmMem.get("non_heap_committed_in_bytes").asLong());
        monitoringResponse.setJvmHeapUsedInBytes(jvmMem.get("heap_used_in_bytes").asLong());
        monitoringResponse.setJvmNonHeapUsedInBytes(jvmMem.get("non_heap_used_in_bytes").asLong());

        final JsonNode jvmMemPools = jvmMem.get("pools");

        final JsonNode jvmMemPoolsYoung = jvmMemPools.get("young");

        monitoringResponse.setJvmMemPoolYoungUsedInBytes(jvmMemPoolsYoung.get("used_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolYoungPeakUsedInBytes(jvmMemPoolsYoung.get("peak_used_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolYoungMaxInBytes(jvmMemPoolsYoung.get("max_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolYoungPeakMaxInBytes(jvmMemPoolsYoung.get("peak_max_in_bytes").asLong());

        final JsonNode jvmMemPoolsOld = jvmMemPools.get("old");

        monitoringResponse.setJvmMemPoolOldUsedInBytes(jvmMemPoolsOld.get("used_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolOldPeakUsedInBytes(jvmMemPoolsOld.get("peak_used_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolOldMaxInBytes(jvmMemPoolsOld.get("max_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolOldPeakMaxInBytes(jvmMemPoolsOld.get("peak_max_in_bytes").asLong());

        final JsonNode jvmMemPoolsSurvivor = jvmMemPools.get("survivor");

        monitoringResponse.setJvmMemPoolSurvivorUsedInBytes(jvmMemPoolsSurvivor.get("used_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolSurvivorPeakUsedInBytes(jvmMemPoolsSurvivor.get("peak_used_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolSurvivorMaxInBytes(jvmMemPoolsSurvivor.get("max_in_bytes").asLong());
        monitoringResponse.setJvmMemPoolSurvivorPeakMaxInBytes(jvmMemPoolsSurvivor.get("peak_max_in_bytes").asLong());

        final JsonNode jvmThreads = jvm.get("threads");

        monitoringResponse.setJvmThreadCount(jvmThreads.get("count").asInt());
        monitoringResponse.setJvmThreadPeakCount(jvmThreads.get("peak_count").asInt());

        final JsonNode jvmGC = jvm.get("gc");

        final JsonNode jvmGCCollectors = jvmGC.get("collectors");

        final JsonNode jvmGCCollectorsYoung = jvmGCCollectors.get("young");

        monitoringResponse.setJvmGCCollectorsYoungCollectionCount(jvmGCCollectorsYoung.get("collection_count").asInt());
        monitoringResponse.setJvmGCCollectorsYoungCollectionTimeInMillis(jvmGCCollectorsYoung.get("collection_time_in_millis").asLong());

        final JsonNode jvmGCCollectorsOld = jvmGCCollectors.get("old");

        monitoringResponse.setJvmGCCollectorsOldCollectionCount(jvmGCCollectorsOld.get("collection_count").asInt());
        monitoringResponse.setJvmGCCollectorsOldCollectionTimeInMillis(jvmGCCollectorsOld.get("collection_time_in_millis").asLong());

        return monitoringResponse;
    }
}
