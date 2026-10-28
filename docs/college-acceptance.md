# College acceptance review

Review status: **Not yet recorded**. This is a review template, not college approval. Engineering verification and stakeholder acceptance are separate. The project remains local-only.

Reviewer name/role: ____________________  Review date: ____________________

Project version/commit or local snapshot: ____________________

## Confirm the property information

| Detail | Confirmed value | Source / confirmed by | Applied and checked |
| --- | --- | --- | --- |
| Guest-house public phone | 77217456 | User, 27 October 2026 | Added; browser checked and production build passed |
| Guest-house public email | cstguesthouse@gmail.com | User explicitly confirmed correction, 27 October 2026 | Added; browser checked and production build passed |
| Guest-house address / arrival instructions | Rinchendhing; detailed arrival instructions pending | User, 27 October 2026 | Address added to Contact page |
| Room 201 regular bed count | Pending | | |
| Room 201 verified guest capacity (1–3 in current system) | Pending | | |
| Room 202 regular bed count | Pending | | |
| Room 202 verified guest capacity (1–3 in current system) | Pending | | |
| Locations, descriptions and amenities by room | Pending | | |
| Actual room photographs and permission to use them | Pending | | |
| Dining periods and hall operating rules | Pending | | |
| Registration policy and administrator ownership | Pending | | |
| Guest-data retention and backup responsibility | Pending | | |

Do not infer capacity from bed count. Confirm special-room capacity separately. The current one-room workflow supports at most three guests; if confirmed requirements differ, review the implementation before applying them. Campus photographs must remain identified as references until actual room images are supplied. Only publish contact details explicitly intended for guest enquiries.

## Review the main workflows

Use synthetic test accounts and records. Record Pass, Fail or Not run against each item, with a reference/screenshot and any follow-up. Existing automated/browser evidence is described in local-handover.md; none of the blank results below is implicitly a pass.

| ID | Review scenario | Expected result | College result / evidence |
| --- | --- | --- | --- |
| UAT-01 | Review home, rooms, facilities and contact | Accurate property content; unverified content identified | Not run |
| UAT-02 | Register/sign in as a guest | Registration follows the chosen policy; guest dashboard opens | Not run |
| UAT-03 | Request a three-guest standard stay | Extra-bed request is mandatory; request begins pending | Not run |
| UAT-04 | Review and approve a request | Admin allocates an eligible room and records bed decision | Not run |
| UAT-05 | Approve overlapping requests for one room | Conflicting approval is rejected | Not run |
| UAT-06 | Check in before adding the required bed | Check-in blocked; succeeds after setup within stay dates | Not run |
| UAT-07 | Check out | Stay completes, room releases and history is retained | Not run |
| UAT-08 | Review special accommodation | Verified capacity and administrator allocation enforced | Not run |
| UAT-09 | Request convention/dining usage | Independent or linked request can be reviewed; overlaps rejected | Not run |
| UAT-10 | Cancel accommodation with pending linked usage | Pending linked usage cancels; approved usage requires separate review | Not run |
| UAT-11 | Search bookings and generate a report | Search combines with status; report date/status semantics are clear | Not run |
| UAT-12 | Deactivate/reactivate a synthetic guest | Sessions end, old tokens stay invalid, bookings remain; self-deactivation blocked | Not run |
| UAT-13 | Edit room details | Validated/audited; duplicate numbers and active allocations prevent conflicting changes | Not run |
| UAT-14 | Use the site on a phone and with keyboard | Forms understandable; menus close with Escape; tables scroll locally | Not run |
| UAT-15 | Start locally and rehearse backup restore | Local site starts; separate restore succeeds without replacing working data | Not run |

## Record defects and decisions

| Item / UAT ID | Problem or requested change | Impact | Owner | Resolution / retest |
| --- | --- | --- | --- | --- |
| | | | | |

Acceptance decision: Pending / Accepted / Accepted with conditions / Changes required

Conditions or excluded scope: _______________________________________________

Reviewer confirmation: ____________________  Date: ____________________

No online publication or GitHub push is authorized by completing this template. Public deployment remains a separate stage. Payment, invoicing and food ordering are outside the agreed system scope.

## Capture the current local configuration

Run `D:\GuestHouse\check-local-readiness.ps1` from PowerShell 7. It checks website response, protected administrator access, eight-standard/two-special inventory and the standard-room capacity rules. It saves `.logs/local-readiness.json` with unfilled room content and special-room values. It does not expose passwords/tokens or guest names and does not mark college acceptance complete. It opens and revokes a short administrator session without changing property data.
