package com.moesif.moesifgraviteeplugin.v3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moesif.moesifgraviteeplugin.configuration.HttpHeader;
import com.moesif.moesifgraviteeplugin.configuration.MoesifPolicyConfiguration;
import com.moesif.moesifgraviteeplugin.configuration.PolicyScope;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.policy.api.PolicyChain;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MoesifPolicyV3Test {

    private MoesifPolicyV3 transformHeadersPolicy;

    @Mock
    private MoesifPolicyConfiguration moesifPolicyConfiguration;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Request request;

    private final HttpHeaders requestHttpHeaders = HttpHeaders.create();
    private HttpHeaders responseHttpHeaders = HttpHeaders.create();

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @SuppressWarnings("removal")
    @BeforeEach
    public void init() {
        transformHeadersPolicy = new MoesifPolicyV3(moesifPolicyConfiguration);
        lenient().when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        lenient().when(request.headers()).thenReturn(requestHttpHeaders);
        lenient().when(response.headers()).thenReturn(responseHttpHeaders);
        lenient().when(templateEngine.convert(any(String.class))).thenAnswer(returnsFirstArg());
    }

    @Test
    void test_OnRequest_noTransformation() {
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnResponse_noTransformation() {
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnRequest_invalidScope() {
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        verify(moesifPolicyConfiguration, never()).getAddHeaders();
        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnResponse_invalidScope() {
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        verify(moesifPolicyConfiguration, never()).getAddHeaders();
        verify(policyChain).doNext(request, response);
    }

    @Test
    void test_OnRequest_addHeader() {
        // Prepare
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnResponse_addHeader() {
        // Prepare
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getRemoveHeaders()).thenReturn(null);
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnResponse_addMultipleHeaders() {
        // Prepare
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Arrays.asList(new HttpHeader("X-Gravitee-Header1", "Header1"), new HttpHeader("X-Gravitee-Header2", "Header2")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Header1")).isEqualTo("Header1");
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Header2")).isEqualTo("Header2");
    }

    @Test
    void test_OnRequest_addHeader_nullValue() {
        // Prepare
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_addHeader_nullValue() {
        // Prepare
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", null)));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnRequest_addHeader_nullName() {
        // Prepare
        when(moesifPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(new HttpHeader(null, "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_addHeader_nullName() {
        // Prepare
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders()).thenReturn(Collections.singletonList(new HttpHeader(null, "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnRequest_updateHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnResponse_updateHeader() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Gravitee-Test", "Value")));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Value");
    }

    @Test
    void test_OnRequest_removeHeader() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_removeHeader() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(moesifPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-Test"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isNull();
    }

    @Test
    void test_OnResponse_removeHeaderNull() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(moesifPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList(null));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isEqualTo("Initial");
    }

    @Test
    void test_OnResponse_removeHeaderAndWhiteList() {
        // Prepare
        responseHttpHeaders.set("x-gravitee-toremove", "Initial");
        responseHttpHeaders.set("x-gravitee-white", "Initial");
        responseHttpHeaders.set("x-gravitee-black", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getAddHeaders()).thenReturn(null);
        when(moesifPolicyConfiguration.getRemoveHeaders()).thenReturn(Collections.singletonList("X-Gravitee-ToRemove"));
        when(moesifPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-Gravitee-White"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-ToRemove")).isNull();
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Black")).isNull();
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-White")).isNotNull();
    }

    @Test
    void test_OnResponse_doNothing() {
        // Prepare
        responseHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Gravitee-Test")).isNotNull();
    }

    @Test
    void test_OnRequest_doNothing() {
        // Prepare
        requestHttpHeaders.set("X-Gravitee-Test", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Gravitee-Test")).isNotNull();
    }

    @Test
    void test_OnResponse_whitelistHeader() {
        // Prepare
        responseHttpHeaders.set("x-walter", "Initial");
        responseHttpHeaders.set("x-white", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(moesifPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onResponse(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(responseHttpHeaders.getFirst("X-Walter")).isNull();
        assertThat(responseHttpHeaders.getFirst("X-White")).isNotNull();
    }

    @Test
    void test_OnRequest_whitelistHeader() {
        // Prepare
        requestHttpHeaders.set("x-walter", "Initial");
        requestHttpHeaders.set("x-white", "Initial");
        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(moesifPolicyConfiguration.getWhitelistHeaders()).thenReturn(Collections.singletonList("X-White"));

        // Run
        transformHeadersPolicy.onRequest(request, response, executionContext, policyChain);

        // Verify
        verify(policyChain).doNext(request, response);
        assertThat(requestHttpHeaders.getFirst("X-Walter")).isNull();
        assertThat(requestHttpHeaders.getFirst("X-White")).isNotNull();
    }

    @Test
    void test_OnRequestContent_addHeader() {
        // Prepare
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Product-Id", "{#jsonPath(#request.content, '$.product.id')}")));

        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.REQUEST_CONTENT);
        when(executionContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        when(executionContext.request()).thenReturn(request);

        // Run
        new MoesifPolicyV3(moesifPolicyConfiguration)
            .onRequestContent(executionContext)
            .write(Buffer.buffer("{\n" + "  \"product\": {\n" + "    \"id\": \"1234\"\n" + "  }\n" + "}"))
            .end();

        // Verify
        assertThat(requestHttpHeaders.getFirst("X-Product-Id")).isNotNull().isEqualTo("1234");
    }

    @Test
    void test_OnResponseContent_addHeader() {
        // Prepare
        when(moesifPolicyConfiguration.getAddHeaders())
            .thenReturn(Collections.singletonList(new HttpHeader("X-Product-Id", "{#jsonPath(#response.content, '$.product.id')}")));

        when(moesifPolicyConfiguration.getScope()).thenReturn(PolicyScope.RESPONSE_CONTENT);
        when(executionContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        when(executionContext.response()).thenReturn(response);

        // Run
        new MoesifPolicyV3(moesifPolicyConfiguration)
            .onResponseContent(executionContext)
            .write(Buffer.buffer("{\n" + "  \"product\": {\n" + "    \"id\": \"1234\"\n" + "  }\n" + "}"))
            .end();

        // Verify
        assertThat(responseHttpHeaders.getFirst("X-Product-Id")).isNotNull().isEqualTo("1234");
    }
}
