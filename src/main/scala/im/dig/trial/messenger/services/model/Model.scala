package im.dig.trial.messenger.services.model

import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats._
import cats.implicits._
import org.apache.commons.codec.binary.Hex


// здесь собраны классы общей для всех микросервисов модели
// подробное описание в CRUD-сервисе


@SerialVersionUID(1L)
final case class SHA256(bytes: Array[Byte])

object SHA256 {
  private val random = new SecureRandom()
  def generate(): SHA256 = {
    val bytes = Array.fill[Byte](32)(0)
    random.nextBytes(bytes)
    SHA256(bytes)
  }
  implicit val showSHA256: Show[SHA256] =
    sha256 => Hex.encodeHexString(sha256.bytes)
  implicit val readSHA256: Read[SHA256] = string => {
    if (string.length =!= 64)
      throw new IllegalArgumentException(s"String '$string' is not a SHA256 hash")
    SHA256(Hex.decodeHex(string))
  }
}


@SerialVersionUID(1L)
final case class Filename(value: String)

object Filename {
  private val invalidFilenameCharacters = """\/:*?"<>|""".toSet + '\u0000'
  implicit val readFilename: Read[Filename] = { string =>
    if (string.exists(char => invalidFilenameCharacters.contains(char)))
      throw new IllegalArgumentException("The filename contains invalid characters")
    Filename(string)
  }
  implicit val showFilename: Show[Filename] = _.value
}


@SerialVersionUID(1L)
final case class File (
  fileId: FileId,
  ownerId: UserId,
  originalName: Filename,
  uploadedOn: LocalDateTime
)



final case class ServiceUnavailable(serviceName: String)
  extends RuntimeException(s"Service '$serviceName' is unavailable.")



object JavaImplicits {

  private val localDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  implicit val readLocalDateTime: Read[LocalDateTime] = string =>
    LocalDateTime.parse(string, localDateTimeFormatter)

  implicit val showLocalDateTime: Show[LocalDateTime] = ldt =>
    ldt.format(localDateTimeFormatter)

}
