require('./index.css');

var $        = require('jquery');
var React    = require('react');
var ReactDOM = require('react-dom');

var Application = require('./Application.jsx');

$(document).ready(function () {
  ReactDOM.render(
    <Application />,
    document.getElementById("container")
  );
});
