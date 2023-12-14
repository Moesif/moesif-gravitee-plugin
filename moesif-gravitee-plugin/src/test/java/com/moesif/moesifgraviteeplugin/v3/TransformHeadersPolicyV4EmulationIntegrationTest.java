package com.moesif.moesifgraviteeplugin.v3;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.ExecutionMode;

@GatewayTest(v2ExecutionMode = ExecutionMode.V4_EMULATION_ENGINE)
class TransformHeadersPolicyV4EmulationIntegrationTest extends MoesifPolicyV3IntegrationTest {}
