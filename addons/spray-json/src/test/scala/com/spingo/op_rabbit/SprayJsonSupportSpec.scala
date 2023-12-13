package com.spingo.op_rabbit

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import spray.json.DefaultJsonProtocol

class SprayJsonSupportSpec extends AnyFunSpec with Matchers with DefaultJsonProtocol {
  case class Thing(a: Int)
  implicit val thingFormat = jsonFormat1(Thing)
  import SprayJsonSupport._
  val u = implicitly[RabbitUnmarshaller[Thing]]
  val m = implicitly[RabbitMarshaller[Thing]]

  describe("SprayJsonSupport") {
    it("deserializes the provided content") {
      u.unmarshall("""{"a": 5}""".getBytes, Some("application/json"), Some("UTF-8")) should be (Thing(5))
    }

    it("interprets no encoding / no contentType as json / UTF8") {
      u.unmarshall("""{"a": 5}""".getBytes, None, None) should be (Thing(5))
    }

    it("rejects wrong encoding") {
      a [MismatchedContentType] should be thrownBy {
        u.unmarshall("""{"a": 5}""".getBytes, Some("text"), Some("UTF-8"))
      }
    }

    it("serializes the provided content") {
      val body = m.marshall(Thing(5))
    }

    it("provides the appropriate content headers") {
      val properties = m.setProperties().build
      properties.getContentType should be ("application/json")
      properties.getContentEncoding should be ("UTF-8")
    }
  }
}
