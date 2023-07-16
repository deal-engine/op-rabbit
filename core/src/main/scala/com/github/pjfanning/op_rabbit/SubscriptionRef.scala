package com.github.pjfanning.op_rabbit

import org.apache.pekko.actor.ActorRef
import scala.concurrent.Future
import scala.concurrent.duration._

trait SubscriptionRef {
  /**
    Future is completed the moment the subscription closes.
    */
  val closed: Future[Unit]

  /**
    Future is completed once the message queue and associated bindings are configured.
    */
  val initialized: Future[Unit]

  /**
    Causes consumer to immediately stop receiving new messages; once pending messages are complete / acknowledged, shut down all associated actors, channels, etc.

    If pending messages aren't complete after the provided timeout (default 5 minutes), the channel is closed and the unacknowledged messages will be scheduled for redelivery.
    */
  def close(timeout: FiniteDuration = SubscriptionActor.Stop.defaultTimeout): Unit

  /**
    Like close, but don't wait for pending messages to finish processing.
    */
  def abort(): Unit

  /**
    Like close, but cause the subscription closed future to fail

    Note, if the subscription is closed for some other reason before this method is called, then this is a no-op
    */
  def abort(exception: Throwable): Unit
}

private [op_rabbit] case class SubscriptionRefDirect(subscriptionActor: ActorRef, initialized: Future[Unit], closed: Future[Unit]) extends SubscriptionRef {
  def close(timeout: FiniteDuration = SubscriptionActor.Stop.defaultTimeout): Unit =
    subscriptionActor ! SubscriptionActor.Stop(None, timeout)

  def abort(): Unit =
    subscriptionActor ! SubscriptionActor.Abort(None)

  def abort(exception: Throwable): Unit =
    subscriptionActor ! SubscriptionActor.Abort(Some(exception))
}

private [op_rabbit] case class SubscriptionRefProxy(subscriptionRef: Future[SubscriptionRef]) extends SubscriptionRef {
  implicit val ec = SameThreadExecutionContext
  val closed = subscriptionRef.flatMap(_.closed)
  val initialized = subscriptionRef.flatMap(_.initialized)
  def close(timeout: FiniteDuration = 5.minutes): Unit =
    subscriptionRef.foreach(_.close(timeout))

  def abort(): Unit =
    subscriptionRef.foreach(_.abort())

  def abort(exception: Throwable): Unit =
    subscriptionRef.foreach(_.abort(exception))
}
