package me.martinrichards.prometheusfinagle.examples

import java.net.InetSocketAddress

import me.martinrichards.prometheusfinagle.PrometheusStatsReceiver
import me.martinrichards.prometheusfinagle.metrics.{MetricsService, Telemetry}
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.http._
import com.twitter.finagle.http.path._
import com.twitter.finagle.http.service.{NotFoundService, RoutingService}
import com.twitter.finagle.loadbalancer.perHostStats
import com.twitter.finagle.{Http, Service}
import io.prometheus.client.CollectorRegistry

object TestServer extends App {

  perHostStats.parse("true")

  val registry = CollectorRegistry.defaultRegistry
  val statsReceiver = new PrometheusStatsReceiver(registry)
  val telemetry = new Telemetry(registry, "namespace")

  val emojiService = new EmojiService(statsReceiver)
  val metricsService = new MetricsService(registry)
  val echoService = new EchoService
  val customTelemetryService = new CustomTelemetryService(telemetry)

  val router: Service[Request, Response] =
    RoutingService.byMethodAndPathObject {
      case (Method.Get, Root / "emoji")   => emojiService
      case (Method.Get, Root / "metrics") => metricsService
      case (Method.Get, Root / "echo")    => echoService
      case (Method.Get, Root / "custom")  => customTelemetryService
      case _                              => new NotFoundService
    }

  ServerBuilder()
    .stack(Http.server)
    .name("testserver")
    .bindTo(new InetSocketAddress(8080))
    .build(router)
}
