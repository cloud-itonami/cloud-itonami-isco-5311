# cloud-itonami-isco-5311

Open Occupation Blueprint for **ISCO-08 5311**: Child Care Workers.

This repository designs a forkable OSS business for an independent child care worker: an activity-assist robot supports supply setup and area monitoring alongside — never instead of — a supervising adult, under a governor-gated actor, so the practice keeps its own care and safety records instead of renting a closed childcare-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a supervised activity-assist robot performs supply setup, activity timing and area monitoring — always alongside, never instead of, an adult caregiver under an actor that proposes
actions and an independent **Child Care Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
any unsupervised child interaction, or activity involving food, medication or physical contact) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
guardian consent + care plan + supervision ratio requirement
        |
        v
Care Advisor -> Child Care Governor -> supervise/log, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `5311`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/child_care/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `child-care.store` — `Store` protocol + `MemStore`: registered
  (guardian-consented) children (with an `allergies` set), activities,
  incident reports. An activity/incident-report can only be recorded
  against a registered child (child provenance).
- `child-care.governor` — `ChildCareGovernor`: `assess` gates a proposal
  against the child env. Hard invariants force `:hold` (no child,
  direct-write instead of `:propose`, or a `:meal` activity for a child
  with known `allergies` below `:high` safety-class); a meal activity
  with allergy risk always requires `:high`+ safety-class and thus
  `:human-approval`; **every** incident-report always escalates to
  `:human-approval` regardless of safety-class or confidence (no
  autonomous incident clearance); low-confidence proposals also
  escalate.

```bash
clojure -M:test   # 8 tests, 14 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 23rd `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321`, `-3253`, `-6210`, `-5223`, `-7231`, `-8121`, `-9111`,
`-2512`, `-1120`, `-4110`, `-3213`, `-5153`, `-7411`, `-2262` and
`-4222` (ADR-2607012000).

## License

AGPL-3.0-or-later.
