var React = require('react');

var Instruments = require('./Instruments.jsx');

module.exports = React.createClass({
  render: function () {
    return (
      <div>
        <h1>Parity Stock Ticker</h1>
        <Instruments />
      </div>
    );
  }
});
