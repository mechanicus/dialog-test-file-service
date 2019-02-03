package im.dig.trial.messenger.services.file.rest.controller

import akka.http.scaladsl.server.{PathMatcher, PathMatcher1, Route}
import akka.stream.Materializer
import im.dig.trial.messenger.services.model.SHA256
import im.dig.trial.messenger.services.model.ReadSyntax._

import scala.concurrent.ExecutionContext


trait RouteGroup {

  protected val SHA256Segment: PathMatcher1[SHA256] = {
    PathMatcher("""[0-9a-fA-F]{64}""".r) flatMap { string =>
      string.readOption[SHA256]
    }
  }

  def routes(implicit mat: Materializer, ec: ExecutionContext): Route

}
