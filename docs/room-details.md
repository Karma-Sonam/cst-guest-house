# Editing room details

Sign in as an administrator and choose **Room management → Edit details**. The same editor is available under **Special rooms**. Save a room number, verified regular bed count, location, description and amenities. Guest-facing details appear on the room information page reached from availability results.

Room numbers accept letters, digits, spaces and hyphens, up to 20 characters; they must start with a letter or digit. Numbers are unique ignoring case. Surrounding spaces are removed. Location is limited to 120 characters, description to 1,000 and amenities to 500. Empty optional text is stored as unset. Text is rendered as plain text, not HTML. No descriptive claims are prefilled for the property.

Standard rooms retain two regular beds and capacity for two guests, or three with an approved extra bed. For special rooms, the regular bed count can be left unverified or set to 1–3. Bed count does not change guest capacity; use **Set capacity** separately. Room category and the eight-standard/two-special inventory remain fixed.

Editing is blocked while a room has an APPROVED or CHECKED_IN allocation. Resolve those allocations first. Completed booking history keeps the same room ID if its number is changed; historical displays may therefore show its current number. Changes create ROOM_DETAILS audit records. Audit records identify the actor, room and change time; they are not a complete revision history of descriptive text.

API: `PUT /api/admin/rooms/{id}` with `roomNumber`, `beds`, `location`, `description` and `amenities`. The API requires an active administrator session. Database migration V5 adds nullable descriptive fields and a case-insensitive room-number uniqueness index, retaining existing room records.

The local development backend must be repackaged/restarted for the API and migration. Rebuild Docker images before using the new fields in the separate container stack. No public hosting is configured.

Verification: all 23 backend tests (13 unit and 10 real PostgreSQL integration tests), 11 frontend tests and the production frontend build passed. Coverage includes role restrictions, guest-facing values, duplicate room numbers ignoring case, field bounds, standard-bed invariants, active-allocation conflicts and audit entries.

The running local site also passed administrator sign-in, opening the room editor, saving existing room details and receiving the success message. V5 was applied on local startup. No unverified room descriptions or amenities were added. Preview: screenshots/admin-room-editor.png.
