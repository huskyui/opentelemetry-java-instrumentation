/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.auto.armeria.v1_0

import com.linecorp.armeria.client.WebClientBuilder
import com.linecorp.armeria.server.ServerBuilder
import io.opentelemetry.auto.test.AgentTestTrait
import io.opentelemetry.instrumentation.armeria.v1_0.AbstractArmeriaTest
import io.opentelemetry.instrumentation.armeria.v1_0.client.OpenTelemetryClient
import io.opentelemetry.instrumentation.armeria.v1_0.server.OpenTelemetryService

class ArmeriaNoDuplicateInstrumentationTest extends AbstractArmeriaTest implements AgentTestTrait {
  @Override
  ServerBuilder configureServer(ServerBuilder sb) {
    return sb.decorator(OpenTelemetryService.newDecorator())
  }

  @Override
  WebClientBuilder configureClient(WebClientBuilder clientBuilder) {
    return clientBuilder.decorator(OpenTelemetryClient.newDecorator())
  }

  def childSetupSpec() {
    server.before()
  }

  def childCleanupSpec() {
    server.after()
  }
}
