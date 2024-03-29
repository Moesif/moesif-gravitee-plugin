package com.moesif.moesifgraviteeplugin.v3;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.moesif.moesifgraviteeplugin.configuration.MoesifPolicyConfiguration;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.ExecutionMode;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest(v2ExecutionMode = ExecutionMode.V3)
@DeployApi("/apis/add-update-whitelist-remove-headers.json")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MoesifPolicyV3IntegrationTest extends AbstractPolicyTest<MoesifPolicyV3, MoesifPolicyConfiguration> {

    @Test
    @DisplayName("Should add, update, whitelist and remove headers")
    void shouldAddUpdateAndRemoveHeaders(HttpClient client) throws InterruptedException {
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
