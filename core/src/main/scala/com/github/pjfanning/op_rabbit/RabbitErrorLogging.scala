package com.github.pjfanning.op_rabbit

import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope
import scala.util.Try

// object RabbitErrorLogging {}

/**
  Basic trait for reporting error messages; implement to build your own error reporting strategies to be used with consumers
  */
trait RabbitErrorLogging {
  /**
    Tries to convert the body to a human-readable string; tries to use charset in contentEncoding
    */
  protected def bodyAsString(body: Array[Byte], properties: BasicProperties): String =
    RabbitErrorLogging.StringHelpers.byteArrayToString(body, properties)

  /**
    Called by consumer to report an exception processing a message

    @param name The name of the message queue which threw the exception
    @param context A string describing the context in which the message was throw (IE: error while deserializing message)
    @param exception The exception thrown by the consumer while attempting to unmarshall the message, or process the message
    @param consumerTag RabbitMQ specific unique identifier for the consumer
    @param envelope envelope Contains delivery attributes
    @param properties Message headers, et. al
    @param body The binary, marshalled message

    @see [[RabbitErrorLogging.bodyAsString]]
    */
  def apply(name: String, context: String, exception: Throwable, consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit

  /**
    Compose this RabbitErrorLogging with another, such that both error logging strategies are used on exception
    */
  final def +(other: RabbitErrorLogging): RabbitErrorLogging = new CombinedLogger(this, other)
}

/**
  Composes two [[RabbitErrorLogging]] strategies, such that both are used when reporting an exception.
  */
private class CombinedLogger(a: RabbitErrorLogging, b: RabbitErrorLogging) extends RabbitErrorLogging {
  def apply(name: String, context: String, exception: Throwable, consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
    a(name, context, exception, consumerTag, envelope, properties, body)
    b(name, context, exception, consumerTag, envelope, properties, body)
  }
}

/**
  Reports consumer errors to Slf4j.
  */
object Slf4jLogger extends RabbitErrorLogging {
  def apply(name: String, context: String, exception: Throwable, consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
    val logger = LoggerFactory.getLogger(name)
    logger.error(s"${context}. Body=${bodyAsString(body, properties)}. Envelope=${envelope}", exception)
  }
}

@deprecated("LogbackLogger has been renamed to Slf4jLogger", "1.0.2")
object LogbackLogger extends RabbitErrorLogging {
  def apply(name: String, context: String, exception: Throwable, consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
    val logger = LoggerFactory.getLogger(name)
    logger.error(s"${context}. Body=${bodyAsString(body, properties)}. Envelope=${envelope}", exception)
  }
}

object RabbitErrorLogging {
  object StringHelpers {
    private val utf8 = Charset.forName("UTF-8")
    /** This method is kind of silly; basically, if the first 20 bytes look like ascii then it treats it as UTF-8.
      * You should always read the content from an envelope. */
    def guessCharset(body: Array[Byte]): Option[Charset] = {
      val firstChars: List[Byte] = (0 until Math.min(20, body.length)).map(body(_)).toList

      if (firstChars.exists { b => b < 0 })
        None
      else
        Some(utf8)
    }

    def byteArrayToString(body: Array[Byte], charset: Option[Charset]): String = {
      (charset orElse guessCharset(body)) match {
        case Some(c) => Try { new String(body, c) } getOrElse { "<Malencoded-String>" }
        case None => "<Binary-Data>"
      }
    }

    def byteArrayToString(body: Array[Byte], properties: BasicProperties): String = {
      byteArrayToString(body, Try { Charset.forName(properties.getContentEncoding) }.toOption)
    }

  }

  implicit val defaultLogger: RabbitErrorLogging = Slf4jLogger
}
