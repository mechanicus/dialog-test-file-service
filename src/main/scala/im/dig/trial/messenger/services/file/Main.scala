package im.dig.trial.messenger.services.file

import akka.actor.{ActorSystem, Props}
import im.dig.trial.messenger.services.file.cluster.{AuthServiceClient, CrudServiceClient, ServiceApi}
import im.dig.trial.messenger.services.file.rest.FileService

object Main {
  def main(args: Array[String]): Unit = {
    val path = "storage"
    implicit val system: ActorSystem = ActorSystem("MessengerBackend")
    val authServiceClient = system.actorOf(Props[AuthServiceClient], "auth-service-client")
    val crudServiceClient = system.actorOf(Props[CrudServiceClient], "crud-service-client")
    val serviceApi = new ServiceApi(authServiceClient, crudServiceClient)
    new FileService(path, serviceApi).run()
  }
}
