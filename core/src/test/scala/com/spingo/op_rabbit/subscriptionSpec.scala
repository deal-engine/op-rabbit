package com.spingo.op_rabbit

import com.spingo.scoped_fixtures.ScopedFixtures
import com.spingo.op_rabbit.helpers.RabbitTestHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Try}

class SubscriptionSpec extends AnyFunSpec with Matchers with ScopedFixtures with RabbitTestHelpers {

  implicit val executionContext = ExecutionContext.global
  val queueName = s"test-queue-rabbit-control"

  trait RabbitFixtures {
  }

  describe("Failed subscription") {
    it("propagates the exception via the initialized future") {
      implicit val recoveryStrategy = RecoveryStrategy.limitedRedeliver()
      new RabbitFixtures {
        val s = Subscription.run(rabbitControl) {
          import Directives._
          channel(qos = 1) {
            consume(pqueue("very-queue")) {
              ack
            }
          }
        }
        val Failure(ex) = Try(await(s.initialized))
        ex.getMessage() should include ("no queue 'very-queue' in vhost")
      }
    }
  }

  describe("Allocating and releasing channels") {
    // it("allocates a channel on subscription, and closes it on shutdown") {
    //   new RabbitFixtures {

    //     val channelWatcher = new com.rabbitmq.client.ShutdownListener {
    //       private val _p = Promise[com.rabbitmq.client.ShutdownSignalException]
    //       val shutdown = _p.future
    //       def shutdownCompleted(cause: com.rabbitmq.client.ShutdownSignalException): Unit = {
    //         _p.success(cause)
    //       }
    //     }

    //     val binding = new Binding {
    //       val queueName = "test"
    //       var channel: Option[Channel] = None
    //       def bind(c: Channel): Unit = {
    //         channel = Some(c)
    //       }
    //     }

    //     val consumer = new Consumer {
    //       val name = "test"
    //       var channels = Seq.empty[Channel]
    //       var unsubscribes = Seq.empty[Int]
    //       val shutdownP = Promise[Unit]
    //       val subscribedP = Promise[Unit]
    //       var counter = 0

    //       def props(queueName: String) = Props {
    //         new Actor {
    //           def receive = {
    //             case Consumer.Unsubscribe =>
    //               unsubscribes = unsubscribes :+ counter
    //               sender ! true

    //             case Consumer.Subscribe(channel) =>
    //               subscribedP.success(Unit)
    //               channels = channels :+ channel
    //               counter += 1

    //             case Consumer.Shutdown =>
    //               shutdownP.success(Unit)
    //               context stop self
    //           }
    //         }
    //       }
    //     }

    //     val subscription = Subscription(binding, consumer)

    //     rabbitControl ! subscription
    //     await(subscription.initialized)
    //     await(consumer.subscribedP.future)

    //     binding.channel.nonEmpty should be(true)
    //     val channel = binding.channel.get
    //     channel.addShutdownListener(channelWatcher)
    //     consumer.channels should be (Seq(channel))

    //     // now test shutdown
    //     subscription.close()
    //     await(subscription.closed)

    //     // It closes the consumer
    //     await(consumer.shutdownP.future)

    //     // It closes the channel
    //     val shutdownReason = await(channelWatcher.shutdown).getReason
    //     shutdownReason.protocolMethodName should be ("channel.close")
    //     channel.isOpen should be (false) // close should have closed the channel associated with this subscription

    //     // Flush out the rest of the stuff
    //     actorSystem.shutdown()
    //     actorSystem.awaitTermination(timeout.duration)

    //     consumer.channels should be (Seq(channel)) // we shouldn't have received another channel after it was closed
    //     consumer.unsubscribes should be (Seq.empty)
    //   }
    // }
  }
}
