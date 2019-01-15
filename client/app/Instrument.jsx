var React = require('react');

module.exports = React.createClass({
  formatField(container, name, factor, fractionalDigits) {
    return !container || !container[name] ? '—' : (container[name] / factor).toFixed(fractionalDigits);
  },

  formatPrice(container, name) {
    return this.formatField(container, name, this.props.instrument.priceFactor, this.props.instrument.priceFractionDigits);
  },

  formatSize(container, name) {
    return this.formatField(container, name, this.props.instrument.sizeFactor, this.props.instrument.sizeFractionDigits);
  },

  render: function () {
    return (
      <tr>
        <td>{this.props.instrument.instrument}</td>
        <td className="numeric">{this.formatPrice(this.props.bbo, "bidPrice")}</td>
        <td className="numeric">{this.formatSize(this.props.bbo, "bidSize")}</td>
        <td className="numeric">{this.formatPrice(this.props.bbo, "askPrice")}</td>
        <td className="numeric">{this.formatSize(this.props.bbo, "askSize")}</td>
        <td className="numeric">{this.formatPrice(this.props.trade, "price")}</td>
        <td className="numeric">{this.formatSize(this.props.trade, "size")}</td>
      </tr>
    );
  }
});

