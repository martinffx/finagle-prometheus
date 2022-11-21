package io.github.martinffx.prometheusfinagle.filter

import io.github.martinffx.prometheusfinagle.UnitTest
import io.github.martinffx.prometheusfinagle.helper.{CollectorHelper, CollectorRegistryHelper}
import io.github.martinffx.prometheusfinagle.metrics.Telemetry
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Await, Duration, Future, Timer}
import io.prometheus.client.CollectorRegistry
import org.specs2.specification.Scope

class HttpLatencyMonitoringFilterSpec extends UnitTest {

  class SlowService extends Service[Request, Response] {
    implicit val timer = DefaultTimer.twitter

    override def apply(request: Request): Future[Response] = {
      Future
        .value(Response(request.version, Status.Ok))
        .delayed(Duration.fromMilliseconds(1500))
    }
  }

  trait Context extends Scope {
    val registry = new CollectorRegistry(true)
    val registryHelper = CollectorRegistryHelper(registry)
    val telemetry = new Telemetry(registry, "test")
    val buckets = Seq(1.0, 2.0)
    val labeller = new TestLabeller
    val filter = new HttpLatencyMonitoringFilter(telemetry, buckets, labeller)
    val service = mock[Service[Request, Response]]
    val slowService = new SlowService
    val request = Request(Method.Get, "/foo/bar")
    val serviceResponse = Response(Status.Created)
    val histogram =
      telemetry.histogram(name = "incoming_http_request_latency_seconds")
    service.apply(request) returns Future.value(serviceResponse)
  }

  "HttpLatencyMonitoringFilter" >> {
    "passes requests on to the next service" in new Context {
      Await.result(filter.apply(request, service))
      there was one(service).apply(request)
    }

    "returns the Response from the next service" in new Context {
      val actualResponse = Await.result(filter.apply(request, service))
      actualResponse ==== serviceResponse
    }

    "counts the request" in new Context {
      Await.result(filter.apply(request, slowService))

      registryHelper.samples
        .get("test_incoming_http_request_latency_seconds_count")
        .map(_.map(_.value).sum) must beSome(1.0).eventually
    }

    "increments the counter with the labels from the labeller" in new Context {
      Await.result(filter.apply(request, service))

      registryHelper.samples
        .get("test_incoming_http_request_latency_seconds_count")
        .map(_.head.dimensions.get("foo").get) must beSome("bar").eventually
    }

    "categorises requests into the correct bucket" in new Context {
      Await.result(filter.apply(request, slowService))

      // Our request takes ~1500ms, so it should NOT fall into the "less than or equal to 1 second" bucket (le=0.5)
      registryHelper.samples
        .get("test_incoming_http_request_latency_seconds_bucket")
        .flatMap(_.find(_.dimensions.get("le").contains("1.0")))
        .map(_.value) must beSome(0.0).eventually

      // However, it should fall into the "less than or equal to 2 seconds" bucket (le=0.5)
      registryHelper.samples
        .get("test_incoming_http_request_latency_seconds_bucket")
        .flatMap(_.find(_.dimensions.get("le").contains("2.0")))
        .map(_.value) must beSome(1.0).eventually

      // It should also fall into the "+Inf" bucket
      registryHelper.samples
        .get("test_incoming_http_request_latency_seconds_bucket")
        .flatMap(_.find(_.dimensions.get("le").contains("+Inf")))
        .map(_.value) must beSome(1.0).eventually
    }
  }
}
