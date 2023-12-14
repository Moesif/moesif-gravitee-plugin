package com.moesif.moesifgraviteeplugin;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.moesif.moesifgraviteeplugin.configuration.MoesifPolicyConfiguration;
import com.moesif.moesifgraviteeplugin.v3.MoesifPolicyV3;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransformHeadersPolicyV4IntegrationTest extends AbstractPolicyTest<MoesifPolicyV3, MoesifPolicyConfiguration> {

    private MessageStorage messageStorage;

    @BeforeEach
    void setUp() {
        messageStorage = getBean(MessageStorage.class);
    }

    @AfterEach
    void tearDown() {
        messageStorage.reset();
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/add-update-whitelist-remove-headers-v4-proxy.json")
    void should_add_update_and_remove_headers_with_proxy_api(HttpClient client) throws InterruptedException {
        wiremock.stubFor(
            get("/endpoint")
                .willReturn(
                    ok()
                        .withHeader("toupdatekeyresponse", "responseToUpdate")
                        .withHeader("toremovekeyresponse", "willBeRemoved")
                        .withHeader("whitelistedkeyresponse", "whitelisted")
                        .withHeader("notinwhitelistkeyresponse1", "excluded")
                        .withHeader("notinwhitelistkeyresponse2", "excluded")
                )
        );

        final TestObserver<HttpClientResponse> obs = client
            .request(HttpMethod.GET, "/test")
            .flatMap(request ->
                request
                    .putHeader("toupdatekey", "firstValue")
                    .putHeader("toremovekey", "willBeRemoved")
                    .putHeader("whitelistedkey", "whitelisted")
                    .putHeader("notinwhitelistkey1", "excluded")
                    .putHeader("notinwhitelistkey2", "excluded")
                    .rxSend()
            )
            .test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("headerKeyResponse")).isEqualTo("headerValue");
                assertThat(response.headers().get("toUpdateKeyResponse")).isEqualTo("updatedValue");
                assertThat(response.headers().get("whitelistedKeyResponse")).isEqualTo("whitelisted");
                assertThat(response.headers().contains("toRemoveKeyResponse")).isFalse();
                assertThat(response.headers().contains("notInWhitelistKeyResponse1")).isFalse();
                assertThat(response.headers().contains("notInWhitelistKeyResponse2")).isFalse();
                return true;
            })
            .assertNoErrors();

        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint"))
                .withHeader("headerkey", equalTo("headerValue"))
                .withHeader("toupdatekey", equalTo("updatedValue"))
                .withHeader("whitelistedkey", equalTo("whitelisted"))
                .withoutHeader("toremovekey")
                .withoutHeader("notinwhitelistkey1")
                .withoutHeader("notinwhitelistkey2")
        );
    }
}
