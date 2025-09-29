package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AuditServiceImplTest {

    private AuditServiceImpl auditService = new AuditServiceImpl();

    @Nested
    class AnonymizeData {

        @Test
        void no_path_to_anonymize() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            String data = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "123456"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;
            JsonNode diff = mapper.readTree(data);

            auditService.anonymizeData(diff, List.of("/otherPath"));

            assertThat(diff).isEqualTo(mapper.readTree(data));
        }

        @Test
        void one_path_to_anonymize() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            String data = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "123456"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;
            JsonNode diff = mapper.readTree(data);

            auditService.anonymizeData(diff, List.of("/initialAccessToken"));

            String anonymizedData = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "*****"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;

            assertThat(diff).isEqualTo(mapper.readTree(anonymizedData));
        }

        @Test
        void one_path_to_anonymize_present_without_value_field() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            String data = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "123456"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;
            JsonNode diff = mapper.readTree(data);

            auditService.anonymizeData(diff, List.of("/clientId"));

            assertThat(diff).isEqualTo(mapper.readTree(data));
        }
    }
}
