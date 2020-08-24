package io.github.rpiotrow.ptt.gateway.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult}

import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}
import akka.event.Logging

trait ServiceProxy {
  def queueRequest(request: HttpRequest): Future[HttpResponse]
}

class ServiceProxyLive(private val host: String, private val port: Int)(implicit
  private val system: ActorSystem,
  private val ec: ExecutionContextExecutor
) extends ServiceProxy {

  private val log = Logging.getLogger(system, this.getClass)

  private val queueSize = 10

  private val pool  = Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port)
  private val queue = Source
    .queue[(HttpRequest, Promise[HttpResponse])](queueSize, OverflowStrategy.dropNew)
    .via(pool)
    .toMat(Sink.foreach({
      case (Success(resp), p) => p.success(resp)
      case (Failure(e), p)    =>
        log.error(e, s"Error proxying request to $host:$port")
        p.success(
          HttpResponse(
            status = StatusCodes.ServiceUnavailable,
            entity = HttpEntity(ContentTypes.`application/json`, s"""{"error": "Service Unavailable"}""")
          )
        )
    }))(Keep.left)
    .run

  def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued    => responsePromise.future
      case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed =>
        Future.failed(
          new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later.")
        )
    }
  }

}
