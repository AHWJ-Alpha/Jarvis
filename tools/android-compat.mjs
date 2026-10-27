// Android's libuv stat fallback reports ctime as birthtime. A chmod or SQLite
// write then looks like file replacement to OpenClaw's generation checks.
// Report the unavailable creation time consistently; retain inode, device,
// ctime, mtime, type and all other mutation/identity checks.
import fs from "node:fs";
import fsp from "node:fs/promises";
import cp from "node:child_process";
import { syncBuiltinESMExports } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";

if (process.platform === "android") {
  // OpenClaw 2026.9.7's Linux process identity helper also works on Android:
  // app processes can read their own /proc/<pid>/stat. Enable that branch so
  // cron's durable execution receipts can distinguish PID reuse. Keep the
  // patch pinned to the exact upstream guard and fail closed on drift.
  const pidIdentityPath = path.join(path.dirname(fileURLToPath(import.meta.url)), "dist", "pid-alive-BK4A222T.mjs");
  const androidGuard = "!isValidPid(pid) || process.platform !== \"linux\"";
  const compatibleGuard = "!isValidPid(pid) || (process.platform !== \"linux\" && process.platform !== \"android\")";
  const pidIdentitySource = fs.readFileSync(pidIdentityPath, "utf8");
  if (pidIdentitySource.includes(androidGuard)) {
    fs.writeFileSync(pidIdentityPath, pidIdentitySource.replace(androidGuard, compatibleGuard));
  } else if (!pidIdentitySource.includes(compatibleGuard)) {
    throw new Error("Unsupported OpenClaw pid-alive module; Android cron compatibility patch was not applied");
  }

  const archivePath = path.join(path.dirname(fileURLToPath(import.meta.url)), "dist", "session-history-eviction-hHpPKUWt.mjs");
  const archiveSource = fs.readFileSync(archivePath, "utf8");
  const archiveGuard = "if (failedIds.length > 0) throw new Error(`Session deletion committed, but ${failedIds.length} transcript archive file export(s) remain pending in SQLite; retry the operation to publish them.`);";
  const archiveDetail = "if (failedIds.length > 0) throw new Error(`Session deletion committed, but ${failedIds.length} transcript archive file export(s) remain pending in SQLite: ${results.filter((result) => !result.archivedPath).map((result) => result.error || result.sessionId).join('; ')}`);";
  if (archiveSource.includes(archiveGuard)) {
    fs.writeFileSync(archivePath, archiveSource.replace(archiveGuard, archiveDetail));
  } else if (!archiveSource.includes(archiveDetail)) {
    throw new Error("Unsupported OpenClaw session archive module; Android archive diagnostics were not applied");
  }

  const archiveArtifactPath = path.join(path.dirname(fileURLToPath(import.meta.url)), "dist", "session-accessor.sqlite-archive-artifact-B42YkoVn.mjs");
  const archiveArtifactSource = fs.readFileSync(archiveArtifactPath, "utf8");
  const linkPublish = "fs.linkSync(tempPath, archivePath);";
  const androidPublish = `try {
			fs.linkSync(tempPath, archivePath);
		} catch (error) {
			if (!hasErrnoCode(error, "EACCES") && !hasErrnoCode(error, "EPERM")) throw error;
			fs.copyFileSync(tempPath, archivePath, fs.constants.COPYFILE_EXCL);
			const fd = fs.openSync(archivePath, "r");
			try { fs.fsyncSync(fd); } finally { fs.closeSync(fd); }
		}`;
  if (archiveArtifactSource.includes(androidPublish)) {
    // Already patched by this process or a worker preload.
  } else if (archiveArtifactSource.includes(linkPublish)) {
    fs.writeFileSync(archiveArtifactPath, archiveArtifactSource.replace(linkPublish, androidPublish));
  } else {
    throw new Error("Unsupported OpenClaw archive artifact module; Android publication patch was not applied");
  }

  const normalize = (stat) => {
    if (stat) {
      stat.birthtimeMs = typeof stat.birthtimeMs === "bigint" ? 0n : 0;
      if ("birthtimeNs" in stat) stat.birthtimeNs = 0n;
      stat.birthtime = new Date(0);
    }
    return stat;
  };
  for (const name of ["stat", "lstat", "fstat"]) {
    const sync = fs[`${name}Sync`];
    fs[`${name}Sync`] = (...args) => normalize(sync(...args));
    const callback = fs[name];
    fs[name] = (...args) => {
      const done = args.pop();
      return callback(...args, (error, stat) => done(error, normalize(stat)));
    };
    if (fsp[name]) {
      const promise = fsp[name];
      fsp[name] = async (...args) => normalize(await promise(...args));
    }
  }
  const open = fsp.open;
  fsp.open = async (...args) => {
    const handle = await open(...args);
    const stat = handle.stat.bind(handle);
    handle.stat = async (...options) => normalize(await stat(...options));
    return handle;
  };
  // Executing a private ELF directly is blocked for apps targeting Android 10+.
  // The system linker can load it. Node's fork() and OpenClaw's SQLite workers
  // must use the same route as the initial process, including their IPC fd.
  const node = process.env.JARVIS_NODE_PATH || process.execPath;
  const linker = "/system/bin/linker64";
  process.execPath = node;
  process.argv[0] = node;
  const nodeOptions = (options = {}) => ({
    ...options,
    env: {
      ...(options.env || process.env),
      LD_LIBRARY_PATH: process.env.LD_LIBRARY_PATH,
      OPENSSL_CONF: process.env.OPENSSL_CONF,
      SSL_CERT_FILE: process.env.SSL_CERT_FILE,
      JARVIS_NODE_PATH: node,
      NODE_OPTIONS: `--import=${process.env.JARVIS_ANDROID_COMPAT || new URL(import.meta.url).pathname}`
    }
  });
  const fork = cp.fork;
  cp.fork = (modulePath, args, options) => {
    if (!Array.isArray(args)) { options = args; args = []; }
    const opts = options || {};
    return fork(modulePath, args, {
      ...nodeOptions(opts),
      execPath: linker,
      execArgv: [node, ...(opts.execArgv || process.execArgv)]
    });
  };
  for (const name of ["spawn", "spawnSync"]) {
    const original = cp[name];
    cp[name] = (command, args, options) => {
      if (!Array.isArray(args)) { options = args; args = []; }
      if (command === node || command === "node") {
        command = linker;
        args = [node, ...args];
        options = nodeOptions(options);
      } else if (command === "/bin/sh") command = "/system/bin/sh";
      if (options?.shell === true || options?.shell === "/bin/sh") {
        options = { ...options, shell: "/system/bin/sh" };
      }
      return original(command, args, options);
    };
  }
  syncBuiltinESMExports();
}
