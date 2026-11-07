// SQLDelight's WebWorkerDriver protocol over the official SQLite WASM build, persisted in OPFS.
// Served by the web app next to sqlite3.mjs, sqlite3.wasm and sqlite3-opfs-async-proxy.js
// (app/webApp/scripts/copy-sqlite.mjs) and started by WebSqlDriverFactory as a module worker, one per tab.
//
// The OPFS SAH pool can be open in one worker only, so the tabs elect an owner with a Web Lock: the owner's worker
// opens the database and runs every tab's statements; the other workers forward theirs over a BroadcastChannel
// and pass the answers back. When the owner's tab closes, its lock is released and a waiting worker takes over.
// The owner runs one transaction at a time: while a tab is inside one, the other tabs' statements wait.
import sqlite3InitModule from "./sqlite3.mjs";

const LOCK = "talk-sqlite-owner";
const channel = new BroadcastChannel("talk-sqlite");
const me = crypto.randomUUID();

let db = null; // set once this worker owns the database
let sqlite3 = null;
const forwarded = new Map(); // this tab's requests waiting for the owner: id -> message
const queue = []; // the owner's work: { from, data }
let transactionOwner = null; // the tab inside a transaction
let transactionSeen = 0;

async function openDatabase() {
  sqlite3 = await sqlite3InitModule();
  // The SAH pool persists without COOP/COEP headers (works on static hosting). A previous owner's handles are
  // released when its worker ends, which can take a moment: try a few times.
  for (let attempt = 1; attempt <= 5; attempt++) {
    try {
      const pool = await sqlite3.installOpfsSAHPoolVfs({ name: "talk-sahpool", directory: "/talk" });
      return new pool.OpfsSAHPoolDb("/talk.db");
    } catch (error) {
      console.warn(`Talk: OPFS SAH pool unavailable (attempt ${attempt})`, error);
      await new Promise((resolve) => setTimeout(resolve, 200 * attempt));
    }
  }
  // The plain OPFS VFS needs cross-origin isolation (COOP/COEP headers).
  if (sqlite3.capi.sqlite3_vfs_find("opfs")) {
    return new sqlite3.oo1.OpfsDb("/talk.db");
  }
  console.warn("Talk: no persistent storage, the database lives in memory");
  return new sqlite3.oo1.DB(":memory:", "c");
}

async function becomeOwner() {
  db = await openDatabase();
  channel.postMessage({ type: "owner" });
  // This tab's requests that waited for an owner run here now.
  for (const data of forwarded.values()) queue.push({ from: me, data });
  forwarded.clear();
  pump();
}

if (navigator.locks) {
  // Held until this worker ends (its tab closes).
  navigator.locks.request(LOCK, () => becomeOwner().then(() => new Promise(() => {})));
} else {
  becomeOwner(); // No Web Locks: one tab, as before.
}

function reply(to, message) {
  if (to === me) postMessage(message);
  else channel.postMessage({ type: "answer", to, message });
}

// WebWorkerDriver reads query rows from `values`, and the changed-row count of any other statement
// from values[0][0]; statements without result columns therefore answer with db.changes().
function run(sql, params) {
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

function handle(from, data) {
  try {
    let values = [];
    switch (data && data.action) {
      case "exec":
        if (!data.sql) throw new Error("exec: missing sql");
        values = run(data.sql, data.params);
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
    reply(from, { id: data.id, results: { values } });
  } catch (error) {
    reply(from, { id: data && data.id, error: String((error && error.message) || error) });
  } finally {
    // Whoever left the database inside a transaction owns it until it ends.
    const inTransaction = sqlite3.capi.sqlite3_get_autocommit(db.pointer) === 0;
    transactionOwner = inTransaction ? from : null;
    transactionSeen = Date.now();
  }
}

function pump() {
  if (!db) return;
  for (;;) {
    const next = queue.findIndex((job) => transactionOwner === null || job.from === transactionOwner);
    if (next < 0) return;
    const { from, data } = queue.splice(next, 1)[0];
    handle(from, data);
  }
}

// A tab that closed inside a transaction would hold everyone up: give up on it after a while.
setInterval(() => {
  if (db && transactionOwner !== null && Date.now() - transactionSeen > 15000) {
    console.warn("Talk: a tab left a transaction open; rolling it back");
    try {
      db.exec("ROLLBACK TRANSACTION;");
    } catch (_) {}
    transactionOwner = null;
    pump();
  }
}, 5000);

self.onmessage = ({ data }) => {
  if (db) {
    queue.push({ from: me, data });
    pump();
  } else {
    forwarded.set(data.id, data);
    channel.postMessage({ type: "request", from: me, data });
  }
};

channel.onmessage = ({ data: message }) => {
  switch (message && message.type) {
    case "request":
      if (db) {
        queue.push({ from: message.from, data: message.data });
        pump();
      }
      break;
    case "answer":
      if (message.to === me && forwarded.delete(message.message.id)) postMessage(message.message);
      break;
    case "owner":
      // A new owner: ask it what the old one did not answer.
      if (!db) for (const data of forwarded.values()) channel.postMessage({ type: "request", from: me, data });
      break;
  }
};
