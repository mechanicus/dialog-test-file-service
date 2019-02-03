package im.dig.trial.messenger.services.file.cluster

import java.time.LocalDateTime

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import im.dig.trial.messenger.services.messages.{CreateFile, GetUserId}
import im.dig.trial.messenger.services.model.{FileId, Filename, SessionId, UserId}

import scala.concurrent.Future
import scala.concurrent.duration._

final class ServiceApi(
  private val authServiceClient: ActorRef,
  private val crudServiceClient: ActorRef
) {

  private implicit val timeout: Timeout = Timeout(1.second)

  def getUserId(sessionId: SessionId): Future[Option[UserId]] =
    (authServiceClient ? GetUserId(sessionId)).mapTo[Option[UserId]]

  def storeFileInfo(fileId: FileId, ownerId: UserId, originalName: Filename, uploadedOn: LocalDateTime): Future[Int] =
    (crudServiceClient ? CreateFile(fileId, ownerId, originalName, uploadedOn)).mapTo[Int]

}
