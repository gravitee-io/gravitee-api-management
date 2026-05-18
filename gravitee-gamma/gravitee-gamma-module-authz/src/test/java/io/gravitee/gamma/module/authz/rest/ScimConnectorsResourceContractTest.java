package io.gravitee.gamma.module.authz.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScimConnectorsResourceContractTest {

    private static final Set<Class<? extends Annotation>> JAXRS_HTTP_METHODS = Set.of(GET.class, POST.class, PUT.class, DELETE.class);

    private static final Map<String, Set<RolePermissionAction>> EXPECTED_ACLS;

    static {
        Map<String, Set<RolePermissionAction>> m = new LinkedHashMap<>();
        m.put("list", Set.of(RolePermissionAction.READ));
        m.put("get", Set.of(RolePermissionAction.READ));
        m.put("create", Set.of(RolePermissionAction.CREATE));
        m.put("update", Set.of(RolePermissionAction.UPDATE));
        m.put("delete", Set.of(RolePermissionAction.DELETE));
        m.put("syncNow", Set.of(RolePermissionAction.UPDATE));
        EXPECTED_ACLS = Map.copyOf(m);
    }

    @Test
    void every_endpoint_carries_permissions_annotation() {
        for (Method method : ScimConnectorsResource.class.getDeclaredMethods()) {
            if (!isJaxRsEndpoint(method)) continue;
            assertThat(method.getAnnotation(Permissions.class)).as("Endpoint %s lacks @Permissions", method.getName()).isNotNull();
        }
    }

    @Test
    void every_permission_targets_environment_authorization() {
        for (Method method : ScimConnectorsResource.class.getDeclaredMethods()) {
            Permissions a = method.getAnnotation(Permissions.class);
            if (a == null) continue;
            for (Permission p : a.value()) {
                assertThat(p.value())
                    .as("Endpoint %s pins to non-authz RolePermission", method.getName())
                    .isEqualTo(RolePermission.ENVIRONMENT_AUTHORIZATION);
            }
        }
    }

    @Test
    void each_endpoint_has_exact_expected_acls() {
        for (Method method : ScimConnectorsResource.class.getDeclaredMethods()) {
            if (!isJaxRsEndpoint(method)) continue;
            Set<RolePermissionAction> expected = EXPECTED_ACLS.get(method.getName());
            assertThat(expected).as("Endpoint %s not in EXPECTED_ACLS — add it", method.getName()).isNotNull();

            Permissions a = method.getAnnotation(Permissions.class);
            Set<RolePermissionAction> actual = Arrays.stream(a.value())
                .flatMap(p -> Arrays.stream(p.acls()))
                .collect(Collectors.toUnmodifiableSet());

            assertThat(actual).as("Endpoint %s wrong ACL set", method.getName()).isEqualTo(expected);
        }
    }

    @Test
    void expected_acls_has_no_stale_entries() {
        Set<String> live = Arrays.stream(ScimConnectorsResource.class.getDeclaredMethods())
            .filter(ScimConnectorsResourceContractTest::isJaxRsEndpoint)
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(live).as("EXPECTED_ACLS contains stale entries").containsAll(EXPECTED_ACLS.keySet());
    }

    private static boolean isJaxRsEndpoint(Method m) {
        for (Annotation a : m.getDeclaredAnnotations()) {
            if (JAXRS_HTTP_METHODS.contains(a.annotationType())) return true;
        }
        return false;
    }
}
