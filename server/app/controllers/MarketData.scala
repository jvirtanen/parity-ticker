package controllers

import akka.actor.{Actor, ActorRef}
import com.paritytrading.foundation.ASCII
import com.paritytrading.parity.net.pmd.{PMD, PMDListener, PMDParser}
import com.paritytrading.parity.book.{Market, MarketListener, OrderBook, Side}
import com.paritytrading.parity.util.{Instrument, Instruments}
import com.paritytrading.nassau.util.MoldUDP64
import com.typesafe.config.Config
import java.net.InetSocketAddress
import org.jvirtanen.config.Configs
import play.api.libs.json.Json
import scala.collection.JavaConverters._

sealed trait MarketData

case class BBO(
  instrument: String,
  bidPrice:   Long,
  bidSize:    Long,
  askPrice:   Long,
  askSize:    Long
) extends MarketData

case class Trade(
  instrument: String,
  price:      Long,
  size:       Long
) extends MarketData

case object InstrumentsRequest

case object MarketDataRequest

class MarketDataReceiver(config: Config, publisher: ActorRef) extends Runnable {

  override def run {
    val multicastInterface = Configs.getNetworkInterface(config, "market-data.multicast-interface")
    val multicastGroup     = Configs.getInetAddress(config, "market-data.multicast-group")
    val multicastPort      = Configs.getPort(config, "market-data.multicast-port")
    val requestAddress     = Configs.getInetAddress(config, "market-data.request-address")
    val requestPort        = Configs.getPort(config, "market-data.request-port")

    var instruments = Instruments.fromConfig(config, "instruments")

    val market = new Market(new MarketListener {

      override def update(book: OrderBook, bbo: Boolean) {
        if (!bbo)
          return
        val bidPrice = book.getBestBidPrice()
        val askPrice = book.getBestAskPrice()
        publisher ! BBO(
          instrument = ASCII.unpackLong(book.getInstrument()).trim,
          bidPrice   = bidPrice,
          bidSize    = book.getBidSize(bidPrice),
          askPrice   = askPrice,
          askSize    = book.getAskSize(askPrice)
        )
      }

      override def trade(book: OrderBook, side: Side, price: Long, size: Long) {
        publisher ! Trade(
          instrument = ASCII.unpackLong(book.getInstrument()).trim,
          price      = price,
          size       = size
        )
      }

    })

    instruments.asScala.foreach { instrument => 
      market.open(instrument.asLong())
    }
    publisher ! instruments

    MoldUDP64.receive(
      multicastInterface,
      new InetSocketAddress(multicastGroup, multicastPort),
      new InetSocketAddress(requestAddress, requestPort),
      new PMDParser(new PMDListener {

        override def version(message: PMD.Version) = Unit

        override def orderAdded(message: PMD.OrderAdded) {
          market.add(message.instrument, message.orderNumber, side(message.side), message.price, message.quantity)
        }

        override def orderExecuted(message: PMD.OrderExecuted) {
          market.execute(message.orderNumber, message.quantity)
        }

        override def orderCanceled(message: PMD.OrderCanceled) {
          market.cancel(message.orderNumber, message.canceledQuantity)
        }

        def side(side: Byte) = side match {
          case PMD.BUY  => Side.BUY
          case PMD.SELL => Side.SELL
        }
      })
    )
  }
}

class MarketDataPublisher extends Actor {

  var instruments:Instruments = null
  var bbos   = Map[String, BBO]()
  var trades = Map[String, Trade]()

  def receive = {
    case bbo: BBO =>
      bbos = bbos.updated(bbo.instrument, bbo)
      context.system.eventStream.publish(bbo)
    case trade: Trade =>
      trades = trades.updated(trade.instrument, trade)
      context.system.eventStream.publish(trade)
    case instruments: Instruments =>
      this.instruments = instruments
    case InstrumentsRequest =>
      sender ! instruments
    case MarketDataRequest =>
      bbos.values.foreach(sender ! _)
      trades.values.foreach(sender ! _)
  }

}

class MarketDataRelay(publisher: ActorRef, out: ActorRef) extends Actor {

  override def preStart {
    publisher ! InstrumentsRequest
  }

  override def postStop {
    context.system.eventStream.unsubscribe(self, classOf[MarketData])
  }

  def receive = {
    case bbo: BBO =>
      out ! Json.obj(
        "type"       -> "BBO",
        "instrument" -> bbo.instrument,
        "bidPrice"   -> bbo.bidPrice,
        "bidSize"    -> bbo.bidSize,
        "askPrice"   -> bbo.askPrice,
        "askSize"    -> bbo.askSize
      )
    case trade: Trade =>
      out ! Json.obj(
        "type"       -> "Trade",
        "instrument" -> trade.instrument,
        "price"      -> trade.price,
        "size"       -> trade.size
      )
    case instruments: Instruments =>
      instruments.asScala.foreach { instrument => 
        out ! Json.obj(
          "type"                -> "Instrument",
          "instrument"          -> instrument.asString(),
          "priceFactor"         -> instrument.getPriceFactor(),
          "sizeFactor"          -> instrument.getSizeFactor(),
          "priceFractionDigits" -> instrument.getPriceFractionDigits(),
          "sizeFractionDigits"  -> instrument.getSizeFractionDigits()
        )
      }
      publisher ! MarketDataRequest
      context.system.eventStream.subscribe(self, classOf[MarketData])
    case _ =>
      Unit
  }

}
