package com.samstarling.prometheusfinagle.metrics

import com.samstarling.prometheusfinagle.UnitTest
import com.twitter.finagle.http.{Method, Request}
import com.twitter.util.Await
import io.prometheus.client.CollectorRegistry
import org.specs2.specification.Scope

class MetricsServiceSpec extends UnitTest {

  trait Context extends Scope {
    val registry = new CollectorRegistry(true)
    val telemetry = new Telemetry(registry, "unit_test")
    val service = new MetricsService(registry)
  }

  "it renders metrics correctly" in new Context {
    telemetry.counter("foo").inc()
    val request = Request(Method.Get, "/")
    val response = Await.result(service.apply(request))

    response.getContentString.trim must contain("unit_test_foo_total 1.0")
  }
}
