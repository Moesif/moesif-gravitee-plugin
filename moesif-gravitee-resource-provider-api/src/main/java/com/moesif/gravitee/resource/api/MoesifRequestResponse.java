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
package com.moesif.gravitee.resource.api;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSession;
import lombok.Data;

@Data
public class MoesifRequestResponse {

    // Request Details
    private String requestId;
    private String transactionId;
    private String clientIdentifier;
    private String uri;
    private String host;
    private String originalHost;
    private String path;
    private String pathInfo;
    private String contextPath;
    private MultiValueMap<String, String> parameters;
    private MultiValueMap<String, String> pathParameters;
    private Map<String, List<String>> requestHeaders;
    private HttpMethod method;
    private String scheme;
    private HttpVersion httpVersion;
    private long timestamp;
    private String remoteAddress;
    private String localAddress;
    private SSLSession sslSession;
    private boolean requestEnded;
    private Buffer requestBody;
    // in case of error
    private Throwable requestError;

    // Response Details
    private Integer responseStatus;
    private Map<String, List<String>> responseHeaders;
    private Buffer responseBody;
    private Throwable responseError;
}
