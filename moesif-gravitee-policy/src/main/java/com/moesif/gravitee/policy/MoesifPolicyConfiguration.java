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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.policy.api.PolicyConfiguration;
import lombok.Data;

@Data
public class MoesifPolicyConfiguration implements PolicyConfiguration {

  private String userIdHeader;
  private String companyIdHeader;
  private boolean debug = false;
}
