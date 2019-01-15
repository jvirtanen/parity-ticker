var $     = require('jquery');
var React = require('react');

var Instrument = require('./Instrument.jsx');

module.exports = React.createClass({
  getInitialState: function () {
    return { instruments: {}, bbos: {}, trades: {} };
  },
  componentDidMount: function () {
    var socket = new WebSocket("ws://" + location.host + "/data");

    socket.onmessage = function (event) {
      var message = JSON.parse(event.data);
      var messageType = message.type;
      delete message.type;

      if (messageType === "Trade") {
        var trades = $.extend({}, this.state.trades);

        trades[message.instrument] = message;

        this.setState({ trades: trades });
      } else if (messageType === "BBO") {
        var bbos = $.extend({}, this.state.bbos);

        bbos[message.instrument] = message;

        this.setState({ bbos: bbos });
      } else if (messageType === "Instrument") {
        var instruments = $.extend({}, this.state.instruments);

        instruments[message.instrument] = message;

        this.setState({ instruments: instruments });
      }
    }.bind(this);
  },
  render: function () {
    var instruments = Object.keys(this.state.instruments).sort();
    var instrumentNodes = instruments.map(function (instrument) {
      return (
        <Instrument key={instrument}
                    instrument={this.state.instruments[instrument]}
                    bbo={this.state.bbos[instrument]}
                    trade={this.state.trades[instrument]} />
      );
    }.bind(this));
    return (
      <table className="instruments">
        <thead>
          <tr>
            <th>Instrument</th>
            <th>Bid Price</th>
            <th>Bid Quantity</th>
            <th>Ask Price</th>
            <th>Ask Quantity</th>
            <th>Last Price</th>
            <th>Last Quantity</th>
          </tr>
        </thead>
        <tbody>
          {instrumentNodes}
        </tbody>
      </table>
    );
  }
});
