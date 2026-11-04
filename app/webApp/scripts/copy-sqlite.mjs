// Copies the SQLite WASM build and Talk's SQLDelight worker into public/sqlite, where
// WebSqlDriverFactory starts the worker from. Runs before `npm start` and `npm run build`.
import { copyFileSync, mkdirSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const target = join(here, "..", "public", "sqlite");
const sqliteDist = join(dirname(createRequire(import.meta.url).resolve("@sqlite.org/sqlite-wasm/package.json")), "dist");
const worker = join(here, "..", "..", "..", "core", "database", "src", "jsMain", "resources", "sqlite", "talk-sqlite.worker.mjs");

mkdirSync(target, { recursive: true });
copyFileSync(join(sqliteDist, "index.mjs"), join(target, "sqlite3.mjs"));
copyFileSync(join(sqliteDist, "sqlite3.wasm"), join(target, "sqlite3.wasm"));
copyFileSync(join(sqliteDist, "sqlite3-opfs-async-proxy.js"), join(target, "sqlite3-opfs-async-proxy.js"));
copyFileSync(worker, join(target, "talk-sqlite.worker.mjs"));
console.log(`SQLite WASM and the Talk worker copied to ${target}`);
