package com.moesif.moesifgraviteeplugin.v3;

import com.moesif.api.models.EventResponseModel;
import com.moesif.moesifgraviteeplugin.configuration.MoesifPolicyConfiguration;
import com.moesif.moesifgraviteeplugin.configuration.PolicyScope;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.api.el.EvaluableResponse;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_API;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class MoesifPolicyV3 {

    protected static final String ERROR_MESSAGE_FORMAT = "[api-id:%s] [request-id:%s] [request-path:%s] %s";


    protected final MoesifPolicyConfiguration configuration;

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (configuration.getScope() == null || configuration.getScope() == PolicyScope.REQUEST) {
            // Do transform
            transform(request.headers(), executionContext);
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        if (configuration.getScope() == PolicyScope.RESPONSE) {
            // Do transform
            transform(response.headers(), executionContext);
        }

        // Apply next policy in chain
        policyChain.doNext(request, response);
    }

    @OnRequestContent
    public ReadWriteStream<Buffer> onRequestContent(ExecutionContext executionContext) {
        log.error("MoesifPolicy onRequestContent");
        if (configuration.getScope() == PolicyScope.REQUEST_CONTENT) {
            return createStream(PolicyScope.REQUEST_CONTENT, executionContext);
        }

        return null;
    }

    @OnResponseContent
    public ReadWriteStream<Buffer> onResponseContent(ExecutionContext executionContext) {
        log.error("MoesifPolicy onResponseContent");
        if (configuration.getScope() == PolicyScope.RESPONSE_CONTENT) {
            return createStream(PolicyScope.RESPONSE_CONTENT, executionContext);
        }

        return null;
    }

    private ReadWriteStream<Buffer> createStream(PolicyScope scope, ExecutionContext context) {
        return new BufferedReadWriteStream() {
            Buffer buffer = Buffer.buffer();

            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                buffer.appendBuffer(content);
                return this;
            }

            @Override
            public void end() {
                initRequestResponseProperties(
                    context,
                    (scope == PolicyScope.REQUEST_CONTENT) ? buffer.toString() : null,
                    (scope == PolicyScope.RESPONSE_CONTENT) ? buffer.toString() : null
                );

                if (scope == PolicyScope.REQUEST_CONTENT) {
                    transform(context.request().headers(), context);
                } else {
                    transform(context.response().headers(), context);
                }

                if (buffer.length() > 0) {
                    super.write(buffer);
                }
                super.end();
            }
        };
    }

    private EventResponseModel eventResponseFromExecutionContext(final ExecutionContext ctx, String content) {
        Response response = ctx.response();
        EventResponseModel eventResponseModel = new EventResponseModel();
        eventResponseModel.setTime(Date.from(Instant.now()));
        eventResponseModel.setStatus(response.status());
        eventResponseModel.setHeaders(response.headers().toSingleValueMap());
        eventResponseModel.setBody(content);
        return eventResponseModel;
    }

    private void initRequestResponseProperties(ExecutionContext context, String requestContent, String responseContent) {
        log.error("MoesifPolicy initRequestResponseProperties");
        context
            .getTemplateEngine()
            .getTemplateContext()
            .setVariable(REQUEST_TEMPLATE_VARIABLE, new EvaluableRequest(context.request(), requestContent));

        context
            .getTemplateEngine()
            .getTemplateContext()
            .setVariable(RESPONSE_TEMPLATE_VARIABLE, new EvaluableResponse(context.response(), responseContent));
    }

    @SuppressWarnings("removal")
    void transform(HttpHeaders httpHeaders, ExecutionContext executionContext) {
        // Add or update response headers
        if (configuration.getAddHeaders() != null) {
            configuration
                .getAddHeaders()
                .forEach(header -> {
                    if (header.getName() != null && !header.getName().trim().isEmpty()) {
                        try {
                            String extValue = (header.getValue() != null)
                                ? executionContext.getTemplateEngine().convert(header.getValue())
                                : null;
                            if (extValue != null) {
                                httpHeaders.set(header.getName(), extValue);
                            }
                        } catch (Exception ex) {
                            MDC.put("api", String.valueOf(executionContext.getAttribute(ATTR_API)));
                            log.error(
                                String.format(
                                    ERROR_MESSAGE_FORMAT,
                                    executionContext.getAttribute(ATTR_API),
                                    executionContext.request().id(),
                                    executionContext.request().path(),
                                    ex.getMessage()
                                ),
                                ex.getCause()
                            );
                            MDC.remove("api");
                        }
                    }
                });
        }

        // verify the whitelist
        List<String> headersToRemove = configuration.getRemoveHeaders() == null
            ? new ArrayList<>()
            : new ArrayList<>(configuration.getRemoveHeaders());

        if (httpHeaders != null && configuration.getWhitelistHeaders() != null && !configuration.getWhitelistHeaders().isEmpty()) {
            httpHeaders
                .names()
                .forEach(headerName -> {
                    if (configuration.getWhitelistHeaders().stream().noneMatch(headerName::equalsIgnoreCase)) {
                        headersToRemove.add(headerName);
                    }
                });
        }

        // Remove request headers
        headersToRemove.forEach(headerName -> {
            if (headerName != null && !headerName.trim().isEmpty()) {
                httpHeaders.remove(headerName);
            }
        });
    }
}
