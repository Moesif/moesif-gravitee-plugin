package com.moesif.moesifgraviteeplugin;

import com.moesif.api.models.EventRequestModel;
import com.moesif.moesifgraviteeplugin.configuration.MoesifPolicyConfiguration;
import com.moesif.moesifgraviteeplugin.v3.MoesifPolicyV3;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpRequest;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Slf4j
public class MoesifPolicy extends MoesifPolicyV3 implements Policy {

    private static final String TRANSFORM_HEADERS_FAILURE = "TRANSFORM_HEADERS_FAILURE";

    public MoesifPolicy(final MoesifPolicyConfiguration configuration) {
        super(configuration);
        log.error("MoesifPolicy constructor");
    }

    @Override
    public String id() {
        return "moesif-gravitee-plugin";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        HttpRequest request = ctx.request();
        return Completable.defer(() -> {
            log.info("Transforming request headers");
            return transform(ctx, ctx.request().headers());
        });
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return Completable.defer(() -> {
            log.info("Transforming response headers");
            return transform(ctx, ctx.response().headers());
        });
    }

    private EventRequestModel eventRequestFromHttpExecutionContext(final HttpExecutionContext ctx) {
        HttpRequest request = ctx.request();
        EventRequestModel eventRequestModel = new EventRequestModel();
        eventRequestModel.setUri(request.uri());
        eventRequestModel.setVerb(request.method().name());
        eventRequestModel.setTime(Date.from(Instant.now()));
        eventRequestModel.setIpAddress(request.remoteAddress());
        request.headers().forEach(header -> {
            eventRequestModel.getHeaders().put(header.getKey(), header.getValue());
            if (header.getKey().equalsIgnoreCase("transfer-encoding")) {
                eventRequestModel.setTransferEncoding(header.getValue());
            }
        });
        // TODO: check best pattern to get body
        eventRequestModel.setBody(request.bodyOrEmpty().blockingGet().toString());
        return eventRequestModel;
    }

    private Completable transform(final HttpExecutionContext ctx, final HttpHeaders httpHeaders) {
        return transformHeaders(ctx.getTemplateEngine(), httpHeaders)
            .onErrorResumeWith(
                ctx.interruptWith(
                    new ExecutionFailure(500).key(TRANSFORM_HEADERS_FAILURE).message("Unable to apply headers transformation")
                )
            );
    }

    private Completable transformHeaders(final TemplateEngine templateEngine, final HttpHeaders httpHeaders) {
        return Maybe
            .fromCallable(configuration::getAddHeaders)
            .flatMapPublisher(Flowable::fromIterable)
            .filter(httpHeader -> httpHeader.getName() != null && !httpHeader.getName().trim().isEmpty() && httpHeader.getValue() != null)
            .flatMapCompletable(httpHeader ->
                templateEngine
                    .eval(httpHeader.getValue(), String.class)
                    .doOnSuccess(newValue -> httpHeaders.set(httpHeader.getName(), newValue))
                    .ignoreElement()
            )
            .andThen(
                Completable.fromRunnable(() -> {
                    log.error("Transforming headers");
                    // verify the whitelist
                    List<String> headersToRemove = configuration.getRemoveHeaders() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(configuration.getRemoveHeaders());

                    if (
                        httpHeaders != null && configuration.getWhitelistHeaders() != null && !configuration.getWhitelistHeaders().isEmpty()
                    ) {
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
                })
            );
    }
}
