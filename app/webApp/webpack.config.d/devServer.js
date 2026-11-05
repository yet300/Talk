// Decompose's web history uses real paths (/post/<id>, /user/<id>): serve index.html for all of them.
config.devServer = Object.assign({}, config.devServer, { historyApiFallback: true });
