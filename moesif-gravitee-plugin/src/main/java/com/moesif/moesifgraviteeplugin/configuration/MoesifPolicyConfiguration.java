package com.moesif.moesifgraviteeplugin.configuration;

import io.gravitee.policy.api.PolicyConfiguration;

import java.util.List;


public class MoesifPolicyConfiguration implements PolicyConfiguration {

    private PolicyScope scope = PolicyScope.REQUEST;

    private List<String> removeHeaders = null;

    private List<HttpHeader> addHeaders = null;

    private List<String> whitelistHeaders = null;

    public PolicyScope getScope() {
        return scope;
    }

    public void setScope(PolicyScope scope) {
        this.scope = scope;
    }

    public List<String> getRemoveHeaders() {
        return removeHeaders;
    }

    public void setRemoveHeaders(List<String> removeHeaders) {
        this.removeHeaders = removeHeaders;
    }

    public List<HttpHeader> getAddHeaders() {
        return addHeaders;
    }

    public void setAddHeaders(List<HttpHeader> addHeaders) {
        this.addHeaders = addHeaders;
    }

    public List<String> getWhitelistHeaders() {
        return whitelistHeaders;
    }

    public void setWhitelistHeaders(List<String> whitelistHeaders) {
        this.whitelistHeaders = whitelistHeaders;
    }
}
