package com.github.pjfanning.op_rabbit.helpers

import com.rabbitmq.client.Channel
import com.github.pjfanning.op_rabbit.MessageForPublicationLike
import scala.concurrent.Promise

case class DeleteQueue(queueName: String) extends MessageForPublicationLike {
  private val _processedP = Promise[Unit]
  def processed = _processedP.future
  val dropIfNoChannel = false
  def apply(channel: Channel): Unit = {
    channel.queueDelete(queueName)
    _processedP.success()
  }
}
