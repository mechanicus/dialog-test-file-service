package im.dig.trial.messenger.services.file.cluster

import akka.actor.{Actor, ActorRef, RootActorPath, Terminated}
import akka.pattern.{ask, pipe}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, MemberUp}
import akka.util.Timeout
import im.dig.trial.messenger.services.messages.CrudServiceMessage
import im.dig.trial.messenger.services.model.ServiceUnavailable

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._


/**
  * Актор akka кластера, явлющийся шлюзом доступа к crud-сервису
  */
final class CrudServiceClient extends Actor {

  private val cluster = Cluster(context.system)
  private implicit val ec: ExecutionContext = context.dispatcher
  private implicit val timeout: Timeout = Timeout(1.second)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberEvent])

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = awaitingForCrudService

  private def awaitingForCrudService: Receive = {
    case MemberUp(member) =>
      if (member.hasRole("crud-service")) {
        val selection = context.actorSelection(RootActorPath(member.address) / "user" / "crud-service")
        selection.resolveOne pipeTo self
      }
    case crudService: ActorRef =>
      context.watch(crudService)
      context.become(work(crudService))
    case _: CrudServiceMessage =>
      Future.failed(ServiceUnavailable("crud-service")) pipeTo sender()
  }

  private def work(crudService: ActorRef): Receive = {
    case m: CrudServiceMessage => (crudService ? m) pipeTo sender()
    case Terminated(a) =>
      if (a == crudService) {
        context.unwatch(crudService)
        context.become(awaitingForCrudService)
      }
  }

}
