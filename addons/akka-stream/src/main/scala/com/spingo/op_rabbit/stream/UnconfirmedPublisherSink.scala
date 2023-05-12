package com.spingo.op_rabbit
package stream

import akka.actor.ActorRef
import akka.Done
import akka.stream.scaladsl.Sink
import scala.concurrent.Future
import scala.concurrent.duration._

object UnconfirmedPublisherSink {
  // Publishes messages to RabbitMQ; does not wait for receive confirmation; does not backpressure. Just sends and forgets.
  def apply[T](rabbitControl: ActorRef, messageFactory: MessageForPublicationLike.Factory[T, UnconfirmedMessage], timeoutAfter: FiniteDuration = 30.seconds, qos: Int = 8): Sink[T, Future[Done]] = {
    Sink.foreach[T] { payload =>
      rabbitControl ! messageFactory(payload)
    }
  }
}
