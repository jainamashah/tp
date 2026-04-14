# Jainam Shah - Project Portfolio Page

## Overview
Crypto1010 is a CLI blockchain wallet simulator for students to learn blockchain fundamentals such as wallet-based transfers, block linkage, and chain validation through hands-on command usage.

My focus was improving the transfer flow (`send`), including stronger argument handling, clearer behavior documentation, and UML artifacts that explain the design and data flow to future contributors.

## Summary of Contributions

### Code contributed
- [Code Dashboard link: [View my code contributions](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=jainamashah&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-02-20T00%3A00%3A00&filteredFileName=)]

### Enhancements implemented
- Improved reliability of command handling through stronger validation and clearer parsing behavior across `send`, `balance`, `help`, and key-related flows.
- Refactored command internals to reduce duplication and separate parsing, validation, and output responsibilities.
- Improved wallet safety and user-facing behavior (for example, key/address handling and balance-related edge cases).
- Enhanced transfer flow behavior, including fee-policy support (speed tiers and manual override) and clearer execution summaries.
- Added/updated comprehensive JUnit tests for `SendCommand`, `BalanceCommand`, and related flows to ensure robust behavior and edge case coverage.

### Storage persistence contributions
- Fixed persistence bugs where blockchain and wallet data were not reliably retained across sessions.
- Improved save-to-disk and autosave behavior for successful transfers and wallet history updates.
- Contributed to making storage behavior more stable and predictable during load/save cycles.
- Helped document and clarify persistence logic in the Developer Guide.

### Issue contributions on GitHub (summary)
- Resolved multiple v1.0 and v2.0 issues focused on command quality, wallet robustness, persistence reliability, and send-flow maintainability.
- Representative completed issues: [#56](https://github.com/AY2526S2-CS2113-F14-4/tp/issues/56), [#66](https://github.com/AY2526S2-CS2113-F14-4/tp/issues/66), [#67](https://github.com/AY2526S2-CS2113-F14-4/tp/issues/67), [#68](https://github.com/AY2526S2-CS2113-F14-4/tp/issues/68).
- Reviewed and commented on PRs and issues to help maintain code quality and project direction.

### Contributions to the User Guide
- Updated `send` command documentation to keep command format and behavior clear.
- Added a `Coming Soon` section describing planned account-switching capability and persistence scope.
- Clarified error messages and command usage in the documentation for new users.

### Contributions to the Developer Guide
- Added a dedicated `SendCommand` class diagram source:
  - `docs/diagrams/SendCommandClassDiagram.puml`
- Added/updated DG sections describing:
  - `send` command implementation details
  - design rationale and dependencies around `SendCommand`
  - UML diagram references for maintainers
  - Persistence and storage design for blockchain and wallet data

### Contributions to team-based tasks
- Updated project documentation structure and cross-references between DG and diagram sources.
- Maintained branch hygiene by creating focused branches and commits for separate concerns (code quality vs documentation).
- Participated in team meetings and discussions to plan features and resolve blockers.

## Optional: Developer Guide extracts
### SendCommand implementation and structure
I documented the send flow and design responsibilities in the DG, emphasizing:
- command-level validation responsibilities,
- delegation to `TransactionRecordingService`, and
- the UML view of static dependencies used in transfer execution.

### UML diagrams contributed
- `docs/diagrams/SendCommandClassDiagram.puml`
