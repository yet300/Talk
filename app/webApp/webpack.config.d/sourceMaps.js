// Development builds wrap every module in eval (Kotlin adds an EvalSourceMapDevToolPlugin), which the page's
// Content-Security-Policy (script-src 'self', no 'unsafe-eval') blocks. Plain source maps keep dev and
// production under one policy.
if (config.mode === "development") {
    config.plugins = config.plugins.filter((plugin) => plugin.constructor.name !== "EvalSourceMapDevToolPlugin");
    config.devtool = "source-map";
}
