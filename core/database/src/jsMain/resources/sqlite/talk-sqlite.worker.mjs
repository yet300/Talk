// SQLDelight's WebWorkerDriver protocol over the official SQLite WASM build, persisted in OPFS.
// Served by the web app next to sqlite3.mjs, sqlite3.wasm and sqlite3-opfs-async-proxy.js
// (app/webApp/scripts/copy-sqlite.mjs) and started by WebSqlDriverFactory as a module worker.
import sqlite3InitModule from "./sqlite3.mjs";

const ready = openDatabase();

async function openDatabase() {
  const sqlite3 = await sqlite3InitModule();
  // The SAH pool persists without COOP/COEP headers (works on static hosting); one tab at a time.
  try {
    const pool = await sqlite3.installOpfsSAHPoolVfs({ name: "talk-sahpool", directory: "/talk" });
    return new pool.OpfsSAHPoolDb("/talk.db");
  } catch (error) {
    console.warn("Talk: OPFS SAH pool unavailable", error);
  }
  // The plain OPFS VFS needs cross-origin isolation (COOP/COEP headers).
  if (sqlite3.capi.sqlite3_vfs_find("opfs")) {
    return new sqlite3.oo1.OpfsDb("/talk.db");
  }
  console.warn("Talk: no persistent storage, the database lives in memory");
  return new sqlite3.oo1.DB(":memory:", "c");
}

// WebWorkerDriver reads query rows from `values`, and the changed-row count of any other statement
// from values[0][0]; statements without result columns therefore answer with db.changes().
function run(db, sql, params) {
  const columnNames = [];
  const rows = db.exec({
    sql,
    bind: params && params.length > 0 ? params : undefined,
    returnValue: "resultRows",
    rowMode: "array",
    columnNames,
  });
  return columnNames.length > 0 ? rows : [[db.changes()]];
}

self.onmessage = async ({ data }) => {
  try {
    const db = await ready;
    let values = [];
    switch (data && data.action) {
      case "exec":
        if (!data.sql) throw new Error("exec: missing sql");
        values = run(db, data.sql, data.params);
        break;
      case "begin_transaction":
        db.exec("BEGIN TRANSACTION;");
        break;
      case "end_transaction":
        db.exec("END TRANSACTION;");
        break;
      case "rollback_transaction":
        db.exec("ROLLBACK TRANSACTION;");
        break;
      default:
        throw new Error(`Unsupported action: ${data && data.action}`);
    }
    postMessage({ id: data.id, results: { values } });
  } catch (error) {
    postMessage({ id: data && data.id, error: String((error && error.message) || error) });
  }
};
