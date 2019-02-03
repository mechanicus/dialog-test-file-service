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
        path("upload") { post { withoutSizeLimit { fileUpload("file") {
          case (metadata, byteSource) =>
            import ReadSyntax._
            metadata.fileName.readEither[Filename] match {
              case Right(originalName) =>
                val fileId = SHA256.generate()
                val stage1 = byteSource.runWith(
                  FileIO.toPath(Paths.get(storageDirectoryPath, fileId.show))
                )
                val stage2 = stage1.flatMap { _ =>
                  val uploadedOn = LocalDateTime.now()
                  serviceApi.storeFileInfo(fileId, userId, originalName, uploadedOn).map { _ =>
                    uploadedOn
                  }
                }
                onSuccess(stage2) { uploadedOn =>
                  val file = File(fileId, userId, originalName, uploadedOn)
                  complete(Responses.ok(file))
                }
              case Left(ex) => complete(Responses.badRequest(ex.getMessage))
            }
        }}}} ~
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
