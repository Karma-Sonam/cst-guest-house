# Administrator booking search

Open **Booking requests** in the administrator sidebar. The search field matches any part of a booking reference, guest name or booking email, ignoring case and leading/trailing whitespace. Combine it with the booking status selector. Changing either filter returns to page one. Clear the field to list all bookings matching the status.

Search uses the guest details stored on the booking, rather than the current account profile. The current-guests screen continues to show checked-in records only. Guest lists remain restricted to the signed-in user's own bookings.

API example: `GET /api/admin/bookings?search=GH-&status=PENDING&page=0&size=10`.

Search is limited to 120 characters and page sizes to 100 records. Percent signs and underscores are treated literally, rather than as wildcard operators. Query values are parameterized. Results use a stable newest-first order with the booking ID as a tie-breaker. No database migration is required. A literal substring search is appropriate for the local property dataset; much larger datasets may need dedicated search indexes.

Verification: all 22 backend tests (13 unit and 9 PostgreSQL integration tests) passed, including reference/name/email search, combined status filtering, pagination, literal wildcards, length validation and access restrictions. All 10 frontend tests and the production build passed.

Browser verification also passed on the running local site: a lowercase reference returned its matching booking; selecting Completed showed no matches, and selecting Pending restored the record. Preview: screenshots/admin-booking-search.png. The local backend was repackaged and restarted; rebuild Docker images before using these additions in the separate container stack.
