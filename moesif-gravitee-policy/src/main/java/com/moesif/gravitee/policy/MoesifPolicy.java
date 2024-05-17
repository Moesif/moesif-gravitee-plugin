/*
 * Copyright © 2024 Moesif (https://moesif.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.moesif.gravitee.policy;

import com.moesif.api.models.EventModel;
import com.moesif.api.models.EventRequestModel;
import com.moesif.api.models.EventResponseModel;
import com.moesif.gravitee.resource.api.MoesifResource;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpRequest;
import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoesifPolicy implements Policy {

  protected final MoesifPolicyConfiguration configuration;

  public MoesifPolicy(MoesifPolicyConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String id() {
    return "moesif-policy";
  }

  @Override
  public Completable onRequest(HttpExecutionContext ctx) {
    HttpRequest request = ctx.request();
    EventModel eventModel = new EventModel();
    EventRequestModel eventRequestModel = new EventRequestModel();

    eventRequestModel.setTime(new Date(request.timestamp()));
    eventRequestModel.setUri(request.uri());
    eventRequestModel.setVerb(request.method().toString());
    eventRequestModel.setHeaders(request.headers().toSingleValueMap());
    eventRequestModel.setIpAddress(request.remoteAddress());

    eventModel.setRequest(eventRequestModel);
    if (
      configuration.userIdHeader != null &&
      request.headers().contains(configuration.userIdHeader)
    ) {
      eventModel.setUserId(request.headers().get(configuration.userIdHeader));
    }
    if (
      configuration.companyIdHeader != null &&
      request.headers().contains(configuration.companyIdHeader)
    ) {
      eventModel.setCompanyId(
        request.headers().get(configuration.companyIdHeader)
      );
    }
    ctx.setAttribute("eventModel", eventModel);

    return request
      .body()
      .doOnSuccess(buffer -> {
        String body = buffer.toString();
        if (configuration().debug) log.info("Request Body: {}", body);
        eventRequestModel.setBody(body);
      })
      .doOnError(error -> {
        log.error("An error occurred while reading the request body", error);
      })
      .ignoreElement()
      .doFinally(() -> {
        ctx.setAttribute("eventModel", eventModel);
      });
  }

  @Override
  public Completable onResponse(HttpExecutionContext ctx) {
    HttpResponse response = ctx.response();
    // Retrieve the event model from the context, or throw an exception if it's not present since that is fatal
    EventModel eventModel = Optional
      .ofNullable((EventModel) ctx.getAttribute("eventModel"))
      .orElseThrow();
    EventResponseModel eventResponseModel = new EventResponseModel();

    eventResponseModel.setTime(new Date()); // set response time to utc now
    eventResponseModel.setStatus(response.status());
    eventResponseModel.setHeaders(response.headers().toSingleValueMap());

    eventModel.setResponse(eventResponseModel);

    return response
      .body()
      .doOnSuccess(buffer -> {
        String body = buffer.toString();
        if (configuration().debug) log.info("Response Body: {}", body);
        eventResponseModel.setBody(body);
      })
      .doOnError(error -> {
        log.error("An error occurred while reading the response body", error);
      })
      .ignoreElement()
      .doFinally(() -> {
        logMoesifEvent(ctx, eventModel);
      });
  }

  private void logMoesifEvent(HttpExecutionContext ctx, EventModel eventModel) {
    MoesifResource<?> client = ctx
      .getComponent(ResourceManager.class)
      .getResource("moesif-resource", MoesifResource.class);
    if (client != null) {
      client.sendEvent(eventModel);
      log.info("Sent event to batching queue");
    } else {
      log.error("MoesifResource not found");
    }
  }
}
