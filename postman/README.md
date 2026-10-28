# CST Guest House API collection

Import both JSON files into Postman, select the local environment, and set your administrator credentials privately. No real credentials are committed.

Run the **Happy path - local test data** folder against a local development database only. It creates a synthetic guest and audited test bookings. The additional-actions folder is a reference for manual, state-dependent operations. Reset the guest email and dates before a new full run.

The collection now contains 55 requests. **Admin management - manual reference** adds user search, account activation/deactivation, reports, booking search and room details. Obtain adminToken using the existing administrator login request. Set targetUserId only to a synthetic test account; do not run this folder as a sequence. Set reportFrom/reportTo explicitly (end date excluded), and URL-encode userSearch/bookingSearch when needed. bookingStatus may be blank or an existing status such as PENDING.

For room editing, set roomId and its current roomNumber deliberately, inspect the existing room values, and review the JSON body before sending. The sample body assumes two regular beds and clears location, description and amenities; retain verified values if you intend to keep them. Special-room bed counts may instead be null or a verified 1–3. Active allocations block edits. These manual operations are not a Postman runner test, and local JSON validation does not imply a real Postman run.

See [delivery and test instructions](../docs/delivery.md) for configuration, expected behavior and limitations.
