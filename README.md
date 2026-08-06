# BOSS RPA Recorder

Record browser interactions into a replayable workflow, from the right sidebar.

Attaches to an open BOSS browser tab, injects a JavaScript recorder into the page, and turns
what you click and type into a list of typed actions with generated selectors. Saved workflows
are picked up by [RPA Engine](https://github.com/risa-labs-inc/boss-plugin-rpaengine), which
replays them.

## What it does

- **Pick a target tab** from the open browser tabs, and connect to it.
- **Record** by injecting a recorder into the page, then polling it for captured interactions.
  Each becomes a typed action with a generated selector (css, xpath, text or id) and a flag for
  whether that selector is unique.
- **Edit the action list**: add actions manually through a dialog, edit or delete them,
  multi-select, select all, reorder up and down, and filter the view.
- **Pause, resume and clear** a recording in progress.
- **Save named configurations** under `~/.boss/config/rparecorder`, load and delete them, or
  export to a JSON file. This directory is exactly where RPA Engine looks.

## MCP tools

| Tool | Purpose |
|---|---|
| `rpa_record_status` | Recording state, captured action count, current URL |
| `rpa_record_toggle` | Start or stop recording |
| `rpa_record_clear` | Discard captured actions |

These act on the most recently opened panel instance. With the panel closed they return an
error rather than silently doing nothing. A tab must also be selected in the panel, or
recording captures nothing.

## Requirements

- BOSS >= 9.2.20, boss-plugin-api >= 1.0.20
- `browserService`, `activeTabsProvider` and `fileSystemDataProvider`, all optional-typed and
  null-checked.
- No external binaries.

## Notes

Recording works by injecting JavaScript into whatever page the selected tab is showing, and
nothing here is permission-gated. Be deliberate about recording on a page holding credentials
or personal data, since the captured selectors and input values are written to disk in plain
JSON.

## Build

```bash
./gradlew buildPluginJar
cp build/libs/boss-plugin-rparecorder-*.jar ~/.boss/plugins/
```

See [AGENTS.md](AGENTS.md) for architecture and conventions.

## License

Proprietary - Risa Labs Inc.
