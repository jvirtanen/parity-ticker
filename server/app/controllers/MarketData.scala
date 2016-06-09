package controllers

import akka.actor.{Actor, ActorRef}
import com.paritytrading.foundation.ASCII
import com.paritytrading.parity.net.pmd.{PMD, PMDListener, PMDParser}
import com.paritytrading.parity.top.{Market, MarketListener, Side}
import com.paritytrading.parity.util.MoldUDP64
import com.typesafe.config.Config
import java.net.InetSocketAddress
import org.jvirtanen.config.Configs
import play.api.libs.json.Json
import scala.collection.JavaConverters._

sealed trait MarketData

case class BBO(
  instrument: String,
  bidPrice:   Double,
  bidSize:    Long,
  askPrice:   Double,
  askSize:    Long
) extends MarketData

case class Trade(
  instrument: String,
  price:      Double,
  size:       Long
) extends MarketData

case object MarketDataRequest

class MarketDataReceiver(config: Config, publisher: ActorRef) extends Runnable {

  val PriceFactor = 10000.0

  override def run {
    val multicastInterface = Configs.getNetworkInterface(config, "market-data.multicast-interface")
    val multicastGroup     = Configs.getInetAddress(config, "market-data.multicast-group")
    val multicastPort      = Configs.getPort(config, "market-data.multicast-port")
    val requestAddress     = Configs.getInetAddress(config, "market-data.request-address")
    val requestPort        = Configs.getPort(config, "market-data.request-port")

    val market = new Market(new MarketListener {

      override def bbo(instrument: Long, bidPrice: Long, bidSize: Long, askPrice: Long, askSize: Long) {
        publisher ! BBO(
          instrument = ASCII.unpackLong(instrument).trim,
          bidPrice   = bidPrice / PriceFactor,
          bidSize    = bidSize,
          askPrice   = askPrice / PriceFactor,
          askSize    = askSize
        )
      }

      override def trade(instrument: Long, side: Side, price: Long, size: Long) {
        publisher ! Trade(
          instrument = ASCII.unpackLong(instrument).trim,
          price      = price / PriceFactor,
          size       = size
        )
      }

    })

    config.getStringList("instruments").asScala.foreach { instrument => 
      market.open(ASCII.packLong(instrument))
    }

    MoldUDP64.receive(
      multicastInterface,
      new InetSocketAddress(multicastGroup, multicastPort),
      new InetSocketAddress(requestAddress, requestPort),
      new PMDParser(new PMDListener {

        override def version(message: PMD.Version) = Unit

        override def seconds(message: PMD.Seconds) = Unit

        override def orderAdded(message: PMD.OrderAdded) {
          market.add(message.instrument, message.orderNumber, side(message.side), message.price, message.quantity)
        }

        override def orderExecuted(message: PMD.OrderExecuted) {
          market.execute(message.orderNumber, message.quantity)
        }

        override def orderCanceled(message: PMD.OrderCanceled) {
          market.cancel(message.orderNumber, message.canceledQuantity)
        }

        override def orderDeleted(message: PMD.OrderDeleted) {
          market.delete(message.orderNumber)
        }

        override def brokenTrade(message: PMD.BrokenTrade) = Unit

        def side(side: Byte) = side match {
          case PMD.BUY  => Side.BUY
          case PMD.SELL => Side.SELL
        }
      })
    )
  }
}

class MarketDataPublisher extends Actor {

  var bbos   = Map[String, BBO]()
  var trades = Map[String, Trade]()

  def receive = {
    case bbo: BBO =>
      bbos = bbos.updated(bbo.instrument, bbo)
      context.system.eventStream.publish(bbo)
    case trade: Trade =>
      trades = trades.updated(trade.instrument, trade)
      context.system.eventStream.publish(trade)
    case MarketDataRequest =>
      bbos.values.foreach(sender ! _)
      trades.values.foreach(sender ! _)
  }

}

class MarketDataRelay(publisher: ActorRef, out: ActorRef) extends Actor {

  override def preStart {
    publisher ! MarketDataRequest
    context.system.eventStream.subscribe(self, classOf[MarketData])
  }

  override def postStop {
    context.system.eventStream.unsubscribe(self, classOf[MarketData])
  }

  def receive = {
    case bbo: BBO =>
      out ! Json.obj(
        "instrument" -> bbo.instrument,
        "bidPrice"   -> bbo.bidPrice,
        "bidSize"    -> bbo.bidSize,
        "askPrice"   -> bbo.askPrice,
        "askSize"    -> bbo.askSize
      )
    case trade: Trade =>
      out ! Json.obj(
        "instrument" -> trade.instrument,
        "price"      -> trade.price,
        "size"       -> trade.size
      )
    case _ =>
      Unit
  }

}
