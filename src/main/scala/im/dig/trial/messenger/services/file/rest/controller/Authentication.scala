package im.dig.trial.messenger.services.file.rest.controller

import akka.http.scaladsl.model.headers.HttpCookiePair
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import im.dig.trial.messenger.services.file.cluster.ServiceApi
import im.dig.trial.messenger.services.file.rest.controller.marshalling.Marshallers._
import im.dig.trial.messenger.services.file.rest.view.Responses
import im.dig.trial.messenger.services.model.ReadSyntax._
import im.dig.trial.messenger.services.model.{SessionId, UserId}


/**
  * Вспомогательный трейт для подмешивания в контроллер.
  * Производит аутентификацию пользователя на основе
  * cookie `sessionId` и передает во внутренний `Route`
  * значение `userId`, полученное на основе токена сессии
  */
trait Authentication {

  protected def serviceApi: ServiceApi

  protected def authenticated(route: UserId => Route): Route = {
    cookie("sessionId") {
      case HttpCookiePair(_, sessionIdString) =>
        sessionIdString.readEither[SessionId] match {
          case Right(sessionId) =>
            onSuccess(serviceApi.getUserId(sessionId)) {
              case Some(userId) => route(userId)
              case None => complete(Responses.unauthorized("unknown user"))
            }
          case Left(ex) => complete(Responses.badRequest(s"Cannot recognize sessionId cookie. ${ex.getMessage}"))
        }
    }
  }
}
