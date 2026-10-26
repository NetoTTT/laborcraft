# Releasing

All GitHub release notes are written in **English**, including when the changelog and in-game
strings are localized.

## Required structure

1. `# LaborCraft <version>`
2. A short intro paragraph describing the release.
3. Topic sections (`## ...`) for the meaningful changes.
4. `## Links` with source, issues, changelog and relevant companion projects.
5. `## Target` with the Minecraft, Fabric Loader, Fabric API and Java versions.

Use short bullets, backticks for commands and file names, and no internal API-request notes. Keep the
release body focused on what users and pack authors need to know.

## Template

```markdown
# LaborCraft <version>

One or two sentences describing the release.

## Added / Changed / Fixed

- User-facing change.

## Notes

- Installation or compatibility note.

## Links

- Source: https://github.com/NetoTTT/laborcraft
- Issues: https://github.com/NetoTTT/laborcraft/issues
- Changelog: https://github.com/NetoTTT/laborcraft/blob/master/CHANGELOG.md

## Target

Minecraft <version> / Fabric Loader <version> / Fabric API <version>. Java <version>.
```
