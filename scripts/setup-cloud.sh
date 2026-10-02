#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ADVENTURERS_TOOLS="${ADVENTURERS_TOOLS:-/workspace/toolchains}"
mkdir -p "$ADVENTURERS_TOOLS"
python3 - <<'PY'
import hashlib
import os
from pathlib import Path
import tarfile
import urllib.request

root = Path(os.environ['ADVENTURERS_TOOLS'])
downloads = root / 'downloads'
downloads.mkdir(exist_ok=True)
builds = [
    ('jdk-25.0.4.1+1', 'https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4.1_1.tar.gz',
     'dbb698396d478e7fa2b1e50f4103324b2a99b90569ee27c33f2261f9215cf41e'),
    ('jdk8u504-b01', 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u504-b01/OpenJDK8U-jdk_x64_linux_hotspot_8u504b01.tar.gz',
     '9c70e102f527ac674ac2fe9c7d47b9a04e2d19842ba5ab8e9b33f368bbadfaea'),
]
if os.uname().machine != 'x86_64':
    raise SystemExit('This cloud bootstrap pins Linux x86_64 JDKs; use platform-specific JDKs and ./gradlew on other systems.')
for directory, url, expected in builds:
    if (root / directory / 'bin/javac').exists():
        print('Using installed toolchain:', directory)
        continue
    archive = downloads / (directory + '.tar.gz')
    if not archive.exists():
        temp = archive.with_suffix('.part')
        try:
            with urllib.request.urlopen(url, timeout=60) as response, temp.open('wb') as output:
                while chunk := response.read(1024 * 1024):
                    output.write(chunk)
            temp.replace(archive)
        finally:
            temp.unlink(missing_ok=True)
    with archive.open('rb') as source:
        actual = hashlib.file_digest(source, 'sha256').hexdigest()
    if actual != expected:
        raise SystemExit('JDK checksum mismatch: ' + str(archive))
    with tarfile.open(archive) as source:
        source.extractall(root, filter='data')
    print('Installed with verified SHA-256:', directory)
PY
python3 scripts/cloud-build.py :core:check :forge:build --no-daemon
