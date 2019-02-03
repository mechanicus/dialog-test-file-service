package im.dig.trial.messenger.services.file.rest.view

import argonaut.Argonaut._
import argonaut._
import cats._
import cats.implicits._
import im.dig.trial.messenger.services.model.{File, Read}
import im.dig.trial.messenger.services.model.ReadSyntax._
import im.dig.trial.messenger.services.model.JavaImplicits._


object JsonCodecs extends EncodeJsons {

  implicit def encodeSeq[A : EncodeJson]: EncodeJson[Seq[A]] = EncodeJson {
    seq => seq.toVector.asJson
  }

  implicit def encodeIndexedSeq[A : EncodeJson]: EncodeJson[IndexedSeq[A]] = EncodeJson {
    indexedSeq => indexedSeq.toVector.asJson
  }

  implicit def encodeJsonResponse[A : EncodeJson]: EncodeJson[JsonResponse[A]] = EncodeJson {
    case Success(code, status, result) =>
      ("code" := code) ->:
      ("status" := status) ->:
      ("result" := result) ->:
      jEmptyObject
    case Error(code, status, messages) =>
      ("code" := code) ->:
      ("status" := status) ->:
      ("messages" := messages) ->:
      jEmptyObject
  }

  implicit def codecReadShow[A : Read : Show]: CodecJson[A] = CodecJson (
    a => a.show.asJson,
    decoder => for { string <- decoder.as[String] } yield string.read[A]
  )

  implicit val codecFile: EncodeJson[File] =
    casecodec4(File.apply, File.unapply)("fileId", "ownerId", "originalName", "uploadedOn")

}
