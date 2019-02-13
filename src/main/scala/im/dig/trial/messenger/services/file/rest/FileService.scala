package im.dig.trial.messenger.services.file.rest

import java.io.IOException
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.FileIO
import akka.stream.{ActorMaterializer, Materializer}
import cats.implicits._
import im.dig.trial.messenger.services.file.cluster.ServiceApi
import im.dig.trial.messenger.services.file.rest.controller.marshalling.Marshallers
import im.dig.trial.messenger.services.file.rest.controller.{Authentication, CustomExceptionHandler, CustomRejectionHandler, RouteGroup}
import im.dig.trial.messenger.services.file.rest.view.{JsonCodecs, Responses}
import im.dig.trial.messenger.services.model.{File, Filename, ReadSyntax, SHA256}

import scala.concurrent.ExecutionContext

/**
  * Класс контроллера, предоставляющий rest api для сохранения
  * и загрузки файлов
  * @param storageDirectoryPath - путь к каталогу, где будут храниться файлы
  * @param serviceApi - scala api для crud- и auth- сервисов
  */
final class FileService(
  private val storageDirectoryPath: String,
  override protected val serviceApi: ServiceApi
) extends RouteGroup
     with Authentication
     with CustomRejectionHandler
     with CustomExceptionHandler
{

  import JsonCodecs._
  import Marshallers._

  prepareStorageDirectory()

  override def routes(implicit mat: Materializer, ec: ExecutionContext): Route = {
    authenticated { userId =>
      pathPrefix("files") {
        // загружаем файл на сервер
        path("upload") { post { withoutSizeLimit { fileUpload("file") {
          case (metadata, byteSource) =>
            import ReadSyntax._
            // файл должен иметь кроссплатформенно допустимое имя
            metadata.fileName.readEither[Filename] match {
              case Right(originalName) =>
                // генерируем идентификатор файла
                val fileId = SHA256.generate()
                // сохраняем файл с именем его идентификатора в каталоге хранилища
                val stage1 = byteSource.runWith(
                  FileIO.toPath(Paths.get(storageDirectoryPath, fileId.show))
                )
                // отправляем метаинформацию о загруженном файле в crud-сервис
                val stage2 = stage1.flatMap { _ =>
                  val uploadedOn = LocalDateTime.now()
                  serviceApi.storeFileInfo(fileId, userId, originalName, uploadedOn).map { _ =>
                    uploadedOn
                  }
                }
                // возвращаем клиенту метаинформацию о загруженном файле
                onSuccess(stage2) { uploadedOn =>
                  val file = File(fileId, userId, originalName, uploadedOn)
                  complete(Responses.ok(file))
                }
              case Left(ex) => complete(Responses.badRequest(ex.getMessage))
            }
        }}}} ~
        // скачиваем файл с сервера по `fileId`
        pathPrefix("download") { get {
          getFromDirectory(storageDirectoryPath)
        }}
      }
    }
  }

  def run()(implicit system: ActorSystem): Unit = {
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher
    Http().bindAndHandle(routes, "localhost", 8003)
  }

  private def prepareStorageDirectory(): Unit = {
    val directory = Paths.get(storageDirectoryPath)
    if (!Files.exists(directory)) {
      Files.createDirectory(directory)
    } else if (!Files.isDirectory(directory)) {
      throw new IOException(
        "Cannot prepare the file storage directory. File with specified name already exists as regular file."
      )
    }
  }

}
