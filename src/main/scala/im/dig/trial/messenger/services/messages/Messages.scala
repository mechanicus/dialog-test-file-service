package im.dig.trial.messenger.services.messages

import java.time.LocalDateTime

import im.dig.trial.messenger.services.model.{FileId, Filename, SessionId, UserId}

// здесь хранятся классы сообщений для выполнения
// запросов к другим микросервисам

sealed abstract class AuthServiceMessage
@SerialVersionUID(1L)
final case class GetUserId(sessionId: SessionId) extends AuthServiceMessage



sealed abstract class CrudServiceMessage
sealed abstract class FileAction extends CrudServiceMessage
@SerialVersionUID(1L)
final case class CreateFile(
  fileId: FileId,
  ownerId: UserId,
  originalName: Filename,
  uploadedOn: LocalDateTime
) extends FileAction
