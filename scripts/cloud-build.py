#!/usr/bin/env python3
"""Use the cloud's existing HTTPS proxy and CA trust for Gradle and Java subprocesses."""
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlsplit

root = Path(__file__).resolve().parents[1]
toolchains = Path(os.environ.get("ADVENTURERS_TOOLS", "/workspace/toolchains"))
env = os.environ.copy()
env["JAVA_HOME"] = str(toolchains / "jdk-25.0.4.1+1")
env["GRADLE_USER_HOME"] = os.environ.get("GRADLE_USER_HOME", "/workspace/.gradle")
java_paths = ",".join(str(p) for p in sorted(toolchains.glob("jdk*")) if (p / "bin/javac").exists())
options = [env.get("JAVA_TOOL_OPTIONS", ""), f"-Dorg.gradle.java.installations.paths={java_paths}"]
proxy = urlsplit(env.get("HTTPS_PROXY", env.get("https_proxy", "")))
if proxy.hostname:
    if proxy.username or proxy.password:
        raise SystemExit("Authenticated proxies must use the platform's supported credential binding; no secrets are copied into JVM options.")
    options.extend([
        f"-Dhttps.proxyHost={proxy.hostname}", f"-Dhttps.proxyPort={proxy.port or 80}",
        f"-Dhttp.proxyHost={proxy.hostname}", f"-Dhttp.proxyPort={proxy.port or 80}",
        "-Dhttp.nonProxyHosts=localhost|127.*",
    ])
    trust = Path("/etc/ssl/certs/java/cacerts")
    if trust.exists():
        options.append(f"-Djavax.net.ssl.trustStore={trust}")
env["JAVA_TOOL_OPTIONS"] = " ".join(options)
args = [str(root / "gradlew"), f"-Porg.gradle.java.installations.paths={java_paths}", *sys.argv[1:]]
raise SystemExit(subprocess.call(args, cwd=root, env=env))
