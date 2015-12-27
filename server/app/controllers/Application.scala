package controllers

import akka.actor.Props
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}

object Application extends Controller {

  val publisher = Akka.system.actorOf(Props[MarketDataPublisher], "market-data-publisher")

  new Thread(new MarketDataReceiver(current.configuration.underlying, publisher)).start

  def data = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
      Props(new MarketDataRelay(publisher, out))
  }

}
