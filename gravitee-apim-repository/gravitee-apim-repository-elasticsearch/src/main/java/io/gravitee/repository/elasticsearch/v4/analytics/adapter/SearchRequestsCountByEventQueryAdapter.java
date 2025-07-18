package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchRequestsCountByEventQueryAdapter {

    public static String adapt(RequestsCountQuery query, boolean isEntrypointIdKeyword, String aggregationField) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);
        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }
        jsonContent.put("aggs", buildEntrypointIdAggregate(isEntrypointIdKeyword, aggregationField));
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildEntrypointIdAggregate(boolean isEntrypointIdKeyword, String aggregationField) {
        return JsonObject.of(
            "entrypoints",
            JsonObject.of(
                "terms",
                JsonObject.of(
                    "field",
                    isEntrypointIdKeyword
                        ? (Objects.isNull(aggregationField) ? "entrypoint-id" : aggregationField)
                        : "entrypoint-id.keyword"
                )
            )
        );
    }

    private static JsonObject buildElasticQuery(RequestsCountQuery query) {
        if (query == null) {
            return null;
        }

        var terms = new ArrayList<JsonObject>();
        query.apiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        var timestamp = new JsonObject();
        query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (!terms.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
        }

        return null;
    }
}
