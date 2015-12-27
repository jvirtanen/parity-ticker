module.exports = {
  entry: __dirname + '/app/index.jsx',
  output: {
    path: __dirname + '/../server/public',
    filename: 'javascripts/bundle.js'
  },
  module: {
    loaders: [
      {
        test: /\.css$/,
        loader: 'style-loader!css-loader'
      },
      {
        test: /\.jsx$/,
        loader: 'babel-loader',
        query: {
          plugins: [
            'syntax-jsx',
            'transform-react-jsx'
          ]
        }
      },
      {
        test: /\.png$/,
        loader: 'file-loader',
        query: {
          name: 'images/[name].[ext]'
        }
      },
      {
        test: /\.woff$/,
        loader: 'file-loader',
        query: {
          name: 'fonts/[name].[ext]'
        }
      }
    ]
  }
};
