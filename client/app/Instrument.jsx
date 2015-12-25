var React = require('react');

module.exports = React.createClass({
  render: function () {
    return (
      <tr>
        <td>{this.props.bbo.instrument}</td>
        <td className="numeric">{formatPrice(this.props.bbo, "bidPrice")}</td>
        <td className="numeric">{formatSize(this.props.bbo, "bidSize")}</td>
        <td className="numeric">{formatPrice(this.props.bbo, "askPrice")}</td>
        <td className="numeric">{formatSize(this.props.bbo, "askSize")}</td>
        <td className="numeric">{formatPrice(this.props.trade, "price")}</td>
        <td className="numeric">{formatSize(this.props.trade, "size")}</td>
      </tr>
    );
  }
});

function formatPrice(container, name) {
  return !container || !container[name] ? '—' : container[name].toFixed(2);
}

function formatSize(container, name) {
  return !container || !container[name] ? '—' : container[name];
}
