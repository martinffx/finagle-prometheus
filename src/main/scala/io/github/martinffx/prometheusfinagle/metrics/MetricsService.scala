package io.github.martinffx.prometheusfinagle.metrics

import java.io.StringWriter

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.Future
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

class MetricsService(registry: CollectorRegistry)
    extends Service[Request, Response] {

  override def apply(request: Request): Future[Response] = {
    val writer = new StringWriter
    TextFormat.write004(writer, registry.metricFamilySamples())
    val response = Response(request.version, Status.Ok)
    response.setContentString(writer.toString)
    Future(response)
  }
}
