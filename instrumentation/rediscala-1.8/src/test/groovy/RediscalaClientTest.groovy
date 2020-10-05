/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.trace.Span.Kind.CLIENT

import akka.actor.ActorSystem
import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.auto.test.utils.PortUtils
import io.opentelemetry.trace.attributes.SemanticAttributes
import redis.ByteStringDeserializerDefault
import redis.ByteStringSerializerLowPriority
import redis.RedisClient
import redis.RedisDispatcher
import redis.embedded.RedisServer
import scala.Option
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import spock.lang.Shared

class RediscalaClientTest extends AgentTestRunner {

  @Shared
  int port = PortUtils.randomOpenPort()

  @Shared
  RedisServer redisServer = RedisServer.builder()
  // bind to localhost to avoid firewall popup
    .setting("bind 127.0.0.1")
  // set max memory to avoid problems in CI
    .setting("maxmemory 128M")
    .port(port).build()

  @Shared
  ActorSystem system

  @Shared
  RedisClient redisClient

  def setupSpec() {
    system = ActorSystem.create()
    redisClient = new RedisClient("localhost",
      port,
      Option.apply(null),
      Option.apply(null),
      "RedisClient",
      Option.apply(null),
      system,
      new RedisDispatcher("rediscala.rediscala-client-worker-dispatcher"))

    println "Using redis: $redisServer.args"
    redisServer.start()
  }

  def cleanupSpec() {
    redisServer.stop()
    system?.terminate()
  }

  def "set command"() {
    when:
    def value = redisClient.set("foo",
      "bar",
      Option.apply(null),
      Option.apply(null),
      false,
      false,
      new ByteStringSerializerLowPriority.String$())


    then:
    Await.result(value, Duration.apply("3 second")) == true
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "Set"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "Set"
          }
        }
      }
    }
  }

  def "get command"() {
    when:
    def write = redisClient.set("bar",
      "baz",
      Option.apply(null),
      Option.apply(null),
      false,
      false,
      new ByteStringSerializerLowPriority.String$())
    def value = redisClient.get("bar", new ByteStringDeserializerDefault.String$())

    then:
    Await.result(write, Duration.apply("3 second")) == true
    Await.result(value, Duration.apply("3 second")) == Option.apply("baz")
    assertTraces(2) {
      trace(0, 1) {
        span(0) {
          operationName "Set"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "Set"
          }
        }
      }
      trace(1, 1) {
        span(0) {
          operationName "Get"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "Get"
          }
        }
      }
    }
  }
}
