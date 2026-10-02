#!/usr/bin/env bash
set -euo pipefail

# Run by the tag workflow after the build and all server tests pass.
release_tag="${ADVENTURERS_RELEASE_TAG:?Missing release tag}"
release_repo="${GITHUB_REPOSITORY:?Missing repository}"
release_commit="${GITHUB_SHA:?Missing source commit}"
if [[ ! "$release_tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
    echo "Unsupported release tag: $release_tag" >&2
    exit 1
fi
release_version="${release_tag#v}"
release_assets="artifacts/forge/build/libs"
release_jar="$release_assets/adventurers-$release_version.jar"
release_checksums="$release_assets/SHA256SUMS"
release_notes="docs/releases/$release_version.md"
test -f "$release_jar"
test -f "$release_checksums"
test -f "$release_notes"
(cd "$release_assets" && sha256sum -c SHA256SUMS)

release_options=(--repo "$release_repo" --target "$release_commit"
    --title "Adventurers $release_tag · 开发预发布"
    --notes-file "$release_notes" --prerelease --latest=false)
if release_draft=$(gh release view "$release_tag" --repo "$release_repo" --json isDraft --jq '.isDraft'); then
    if [[ "$release_draft" != true ]]; then
        echo "Release is already published; refusing to replace its assets or tag." >&2
        exit 1
    fi
    gh release edit "$release_tag" "${release_options[@]}" --draft=true
else
    gh release create "$release_tag" "${release_options[@]}" --verify-tag --draft
fi
# No --clobber: a previous upload must never be silently replaced.
gh release upload "$release_tag" "$release_jar" "$release_checksums" --repo "$release_repo"
gh release edit "$release_tag" --repo "$release_repo" --draft=false --prerelease --latest=false
