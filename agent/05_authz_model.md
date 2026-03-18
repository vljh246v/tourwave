# Authorization Model (MVP)

## Goal
Define a simple and predictable authorization model for the Tour Booking Platform.

This document explains:

- role model
- resource ownership model
- access rules by actor
- recommended backend authorization checks
- API-level authorization mapping
- denial behavior and security notes

---

## Principles

### 1. Deny by default
If access is not explicitly allowed, deny it.

### 2. Resource ownership first
Authorization should be based on the actual resource owner (organization / booking / inquiry / review), not on client-provided IDs.

### 3. Role + relationship based access
Authorization is not only role-based.
It depends on both:
- role
- relationship to resource

Examples:
- a USER can access their own booking
- an ORG_ADMIN can access bookings belonging to their organization
- an invited participant may access invitation endpoints, but not all booking details before acceptance

### 4. Server resolves authority
The server must derive organization ownership from stored data.

Examples:
- booking.organization_id
- occurrence.organization_id
- inquiry.organization_id
- review.organization_id

Never trust client input for authorization.

### 5. Minimize authorization joins
Because the schema is FK-minimized, denormalized columns such as `bookings.organization_id` are intentionally used to simplify and speed up auth checks.

---

## Role Model

A user may hold multiple roles at the same time.

### Global role-like identities
These are logical capabilities, not necessarily stored in a single enum column:

- USER
- INSTRUCTOR

### Organization-scoped roles
These are granted through `organization_members.role`:

- ORG_MEMBER
- ORG_ADMIN
- ORG_OWNER

### Notes
- A user may be `USER + INSTRUCTOR`
- A user may belong to multiple organizations
- A user may have different org roles in different organizations
- A user may hold multiple org roles simultaneously if desired, though in practice ORG_OWNER usually supersedes lower roles

---

## Actor Types

For authorization reasoning, the system primarily recognizes these actor categories:

### 1. Anonymous
Not logged in.

### 2. Authenticated User
Logged in, but no organization privilege for the target resource.

### 3. Booking Leader
The user who created the booking (`bookings.leader_user_id`).

### 4. Accepted Participant
A user included in `booking_participants` with `status = ACCEPTED`.

### 5. Invited Participant
A user included in `booking_participants` with `status = INVITED`.

### 6. Instructor
A user with an instructor profile.
This alone does **not** grant organization operator permissions.

### 7. Organization Member
A user with org membership but no admin ownership privilege.

### 8. Organization Admin
A user with `ORG_ADMIN` for the organization.

### 9. Organization Owner
A user with `ORG_OWNER` for the organization.

### 10. Platform Admin (optional)
Not part of MVP by default.
Only applies if moderation/admin-only APIs are actually enabled in implementation.

---

## Ownership Model

### Tour
A Tour belongs to exactly one organization.

Ownership source:
- `tours.organization_id`

### TourOccurrence
An occurrence belongs to exactly one tour and one organization.

Ownership source:
- `tour_occurrences.organization_id`

### Booking
A booking belongs to exactly one occurrence and one organization.

Ownership source:
- `bookings.organization_id` (denormalized, source of truth for authz)

### Inquiry
An inquiry belongs to exactly one organization, occurrence, and booking.

Ownership source:
- `inquiries.organization_id`
- `inquiries.booking_id`

### Review
A review belongs to exactly one organization and occurrence.

Ownership source:
- `reviews.organization_id`

### Announcement
Belongs to one organization.

Ownership source:
- `announcements.organization_id`

### Admin Note
Belongs to one organization.

Ownership source:
- `admin_notes.organization_id`

### Asset
Ownership is slightly different:
- primary owner is `assets.owner_user_id`
- second-level authorization may be granted if the asset is attached to a resource belonging to an organization the actor operates

MVP recommendation:
- simplest rule: asset owner only
- optional extension: org operator can access assets attached to org-owned resources

---

## Organization Membership Resolution

A user is considered an active member of an organization if:

- `organization_members.organization_id = targetOrgId`
- `organization_members.user_id = actorUserId`
- `organization_members.status = ACTIVE`

### Role checks
Roles are stored as a single effective role in `organization_members.role`.

Helper checks:

- `hasOrgRole(userId, orgId, ORG_MEMBER)`
- `hasOrgRole(userId, orgId, ORG_ADMIN)`
- `hasOrgRole(userId, orgId, ORG_OWNER)`

### Role hierarchy (recommended logical model)
For permission evaluation:

- ORG_OWNER includes ORG_ADMIN capabilities
- ORG_ADMIN includes ORG_MEMBER capabilities
- ORG_MEMBER does not imply ORG_ADMIN/ORG_OWNER

Current implementation normalizes this in code. `OWNER` is treated as stronger than `ADMIN`, and `ADMIN` is treated as stronger than `MEMBER`.

### Membership lifecycle currently implemented

- `INVITED`: operator created invitation, not yet accepted
- `ACTIVE`: usable membership for operator/member checks
- `INACTIVE`: disabled membership, no access

Current operator endpoints:

- `POST /operator/organizations`
- `GET /operator/organizations/{orgId}`
- `PATCH /operator/organizations/{orgId}`
- `GET /operator/organizations/{orgId}/members`
- `POST /operator/organizations/{orgId}/members/invitations`
- `PATCH /operator/organizations/{orgId}/members/{userId}/role`
- `PATCH /operator/organizations/{orgId}/members/{userId}/deactivate`
- `POST /organizations/{orgId}/memberships/accept`

Current instructor/tour endpoints:

- `POST /instructor-registrations`
- `GET /organizations/{orgId}/instructor-registrations`
- `POST /instructor-registrations/{registrationId}/approve`
- `POST /instructor-registrations/{registrationId}/reject`
- `GET /me/instructor-profile?organizationId={orgId}`
- `POST /me/instructor-profile`
- `PATCH /me/instructor-profile`
- `GET /instructors/{instructorProfileId}`
- `POST /organizations/{orgId}/tours`
- `GET /organizations/{orgId}/tours`
- `PATCH /tours/{tourId}`
- `POST /tours/{tourId}/publish`
- `PUT /tours/{tourId}/content`
- `GET /tours/{tourId}/content`

Authorization notes for current implementation:

- org profile read for operators requires `ACTIVE` membership
- member invitation and deactivation require `ADMIN` or `OWNER`
- assigning `OWNER` or modifying an existing owner membership requires `OWNER`
- operator self-deactivation is rejected
- instructor registration apply is open to authenticated users targeting an active organization
- instructor registration approve/reject requires org operator membership
- me instructor profile read/write is limited to the profile owner
- operator tour create/update/publish/content write requires org operator membership
- public tour content is visible only after publish

---

## Booking Access Model

Booking access is relationship-based.

### A booking may be accessed by:
- booking leader
- accepted booking participant
- org admin/operator of booking.organization_id

### Invited participant
An invited participant with `status = INVITED` may access:
- invitation accept endpoint
- invitation decline endpoint
- invitation summary endpoint if provided

But should **not** get full booking/inquiry visibility until accepted, unless product explicitly allows it.

### Booking leader special rights
Booking leader can:
- cancel their booking
- decrease party size
- invite participants
- see their own booking
- accept/decline waitlist offer for their booking

### Accepted participant rights
Accepted participant can:
- see booking detail
- access booking inquiry
- write eligible reviews
- access booking calendar
- read occurrence changes relevant to their booking

Accepted participant cannot:
- cancel the whole booking
- decrease party size
- approve/reject booking
- manage waitlist offer for the booking unless they are also the leader

---

## Inquiry Access Model

Inquiry is tied to booking + occurrence + organization.

### Inquiry can be viewed by:
- booking leader
- accepted participants of the linked booking
- org admin/operator of inquiry.organization_id

### Inquiry can be written by:
- booking leader
- accepted participants
- org operators

### Inquiry can be closed by:
- booking leader
- accepted participants
- org operators

### Invited-but-not-accepted participant
Should not access inquiry by default.

---

## Review Access Model

### Public review access
Public users can only see:
- rating average
- review count

Public users cannot see:
- review body/content
- reviewer identity
- internal moderation status

### Review content access
Review body is visible only to:
- author
- org admin/operator of review.organization_id
- related instructor for instructor-type reviews

### Author rights
Author may:
- create review if eligible
- view their own review content

MVP recommendation:
- do not allow edit after submission
- do not allow delete after submission unless explicit policy added

### Instructor access
For instructor review:
- reviewed instructor may view content
- other instructors may not

### Organization operator access
ORG_ADMIN / ORG_OWNER of the review’s organization may:
- list reviews
- read private review content
- moderate/hide review

---

## Tour / Occurrence Access Model

### Public reads
Anonymous users may access:
- published tours
- published tour occurrences
- occurrence search
- rating summary
- public announcements

### Non-public tour access
Draft/archived or unpublished tour data may be accessed only by:
- org admin/operator of that organization

### Operator-only occurrence actions
Only ORG_ADMIN / ORG_OWNER may:
- create occurrence
- patch occurrence
- cancel occurrence
- finish occurrence
- reschedule occurrence
- notify participants
- manage waitlist manually
- export participant roster

### Instructor access
Instructors assigned to a tour/occurrence may view public occurrence/tour info, but assignment alone should **not** grant operator permissions unless the user is also org admin/operator.

---

## Assets Access Model

### Upload
Authenticated users may upload assets.

### Read
MVP simplest rule:
- only owner can read the asset directly

Optional rule:
- org operator may read the asset if the asset is attached to a resource owned by the operator’s organization

### Delete
MVP simplest rule:
- owner may delete
- org operator may delete if attached to organization-managed resource and business rules allow

### Important
Asset authorization must never rely only on `assetId`.
Always check:
- owner
- or linked resource ownership if extended rule is enabled

---

## Policy / Notes / Reports Access Model

### Cancellation policy
Can be read/updated by:
- ORG_ADMIN
- ORG_OWNER

### Admin notes
Can be created/read by:
- ORG_ADMIN
- ORG_OWNER

Not visible to:
- booking leader
- participants
- instructors without operator role

### Reports
Can be read by:
- ORG_ADMIN
- ORG_OWNER

Export endpoints follow the same rule:
- GET `/organizations/{orgId}/reports/bookings/export`
- GET `/organizations/{orgId}/reports/occurrences/export`

### Announcements
Can be created/updated/deleted/read in operator namespace by:
- ORG_ADMIN
- ORG_OWNER

Public read:
- GET `/public/announcements`
- only announcements with `visibility=PUBLIC`
- current time must be within `publishStartsAtUtc <= now < publishEndsAtUtc` when `publishEndsAtUtc` is set

Not allowed for:
- ORG_MEMBER
- instructors without operator role

---

## Favorites / Notifications / Me Access Model

### Favorites
Only the authenticated user may manage their own favorites.

### Notifications
Only the notification owner may:
- list notifications
- mark as read
- mark all as read

### Me endpoints
Only the authenticated user may:
- read their own `/me`
- update their own `/me`
- deactivate their own account
- soft-delete their own account

---

## Authorization Rules by Endpoint Group

## 1. Auth / Account

### Public
- POST `/auth/signup`
- POST `/auth/login`
- POST `/auth/refresh`
- POST `/auth/password/reset-request`
  - always returns 204 whether the account exists or not to reduce enumeration risk
- POST `/auth/password/reset-confirm`
- POST `/auth/email/verify-confirm`

### Authenticated user only
- POST `/auth/logout`
- POST `/auth/email/verify-request`
- POST `/me/deactivate`
- DELETE `/me`

Runtime security note:
- deactivated accounts are denied at the security perimeter even if they still hold an older access token

---

## 2. Me / Notifications / Favorites

### Authenticated user only
- GET `/me`
- PATCH `/me`
- GET `/me/bookings`
- GET `/me/inquiries`
- GET `/me/favorites`
- GET `/me/notifications`
- POST `/me/notifications/{notificationId}/read`
- POST `/me/notifications/read-all`

Owner rule:
- actor must match current authenticated principal

---

## 3. Organizations / Members

### Public
- GET `/organizations/{orgId}`

### Org member and above
- GET `/operator/organizations/{orgId}/members`

### ORG_ADMIN / ORG_OWNER
- POST `/operator/organizations/{orgId}/members/invitations`
- POST `/organizations/{orgId}/memberships/accept`
- PATCH `/operator/organizations/{orgId}/members/{userId}/role`
- PATCH `/operator/organizations/{orgId}/members/{userId}/deactivate`

### Create org
- POST `/operator/organizations`
- authenticated user only
- creator becomes ORG_OWNER

---

## 4. Instructor Registration / Instructor Profile

### Authenticated user
- POST `/instructor-registrations`
- POST `/me/instructor-profile`
- PATCH `/me/instructor-profile`

### ORG_ADMIN / ORG_OWNER
- GET `/organizations/{orgId}/instructor-registrations`
- POST `/instructor-registrations/{registrationId}/approve`
- POST `/instructor-registrations/{registrationId}/reject`

### Public
- GET `/instructors/{instructorProfileId}`
- GET `/instructors/{instructorProfileId}/rating-summary`

---

## 5. Tours / Content / Assets / Favorites

### Public
- GET `/tours`
- GET `/tours/{tourId}` (published only)
- GET `/tours/{tourId}/instructors`
- GET `/tours/{tourId}/content` (published only)
- GET `/tours/{tourId}/rating-summary`

### ORG_ADMIN / ORG_OWNER
- GET `/organizations/{orgId}/tours`
- POST `/organizations/{orgId}/tours`
- PATCH `/tours/{tourId}`
- POST `/tours/{tourId}/publish`
- POST `/tours/{tourId}/archive`
- PUT `/tours/{tourId}/instructors`
- PUT `/tours/{tourId}/content`
- POST `/tours/{tourId}/assets`
- PATCH `/tours/{tourId}/assets/reorder`
- DELETE `/tours/{tourId}/assets/{tourAssetId}`

### Authenticated user
- POST `/tours/{tourId}/favorite`
- DELETE `/tours/{tourId}/favorite`

---

## 6. Occurrences / Search / Announcements / Calendar

### Public
- GET `/tours/{tourId}/occurrences` (if tour published)
- GET `/occurrences/{occurrenceId}` (if visible)
- GET `/occurrences/{occurrenceId}/availability`
- GET `/occurrences/{occurrenceId}/quote`
- GET `/search/occurrences`
- GET `/public/announcements`

Current runtime note:
- public catalog는 published tour와 scheduled occurrence만 노출한다.
- search는 auth 없이 호출 가능하지만, 현재 구현 필터는 `locationText`, `dateFrom/dateTo`, `timezone`, `partySize`, `onlyAvailable`, `sort`, `cursor`, `limit` 범위다.

### ORG_ADMIN / ORG_OWNER
- POST `/tours/{tourId}/occurrences`
- PATCH `/occurrences/{occurrenceId}`
- POST `/occurrences/{occurrenceId}/cancel`
- POST `/occurrences/{occurrenceId}/finish`
- POST `/occurrences/{occurrenceId}/reschedule`
- POST `/occurrences/{occurrenceId}/notify`
- POST `/organizations/{orgId}/announcements`
- PATCH `/announcements/{announcementId}`
- DELETE `/announcements/{announcementId}`

### Calendar ICS
- GET `/occurrences/{occurrenceId}/calendar.ics`
  - recommended rule: accepted participant, booking leader, or org operator
  - optional future extension: public for published occurrence

---

## 7. Bookings / Waitlist / Participants / Policies / Notes

### Authenticated user
- POST `/occurrences/{occurrenceId}/bookings`

### Booking participant or operator
- GET `/bookings/{bookingId}`
- GET `/bookings/{bookingId}/explain`

### Leader only
- POST `/bookings/{bookingId}/cancel`
- PATCH `/bookings/{bookingId}/party-size`
- POST `/bookings/{bookingId}/offer/accept`
- POST `/bookings/{bookingId}/offer/decline`
- POST `/bookings/{bookingId}/participants`
- GET `/bookings/{bookingId}/participants`

### Invited participant only
- POST `/participants/{participantId}/accept`
- POST `/participants/{participantId}/decline`

### Accepted participant or leader
- GET `/bookings/{bookingId}/calendar.ics`

### ORG_ADMIN / ORG_OWNER
- GET `/organizations/{orgId}/bookings`
- GET `/organizations/{orgId}/occurrences/{occurrenceId}/bookings`
- POST `/bookings/{bookingId}/approve`
- POST `/bookings/{bookingId}/reject`
- POST `/bookings/{bookingId}/cancel`
- POST `/bookings/{bookingId}/complete`
- PUT `/bookings/{bookingId}/attendance`
- POST `/bookings/{bookingId}/extend-offer`
- POST `/bookings/{bookingId}/force-expire`
- GET `/occurrences/{occurrenceId}/waitlist`
- POST `/waitlist/{waitlistId}/promote`
- GET `/occurrences/{occurrenceId}/policies`
- PUT `/occurrences/{occurrenceId}/policies`
- POST `/occurrences/{occurrenceId}/notes`

### Policies visibility
Policy may be public-read if product wants it.
MVP recommendation:
- policy read may be public or authenticated depending on UX
- policy write is operator-only

### Notes visibility
Notes are internal only:
- ORG_ADMIN
- ORG_OWNER

---

## 8. Inquiries

### Create inquiry
Recommended rule:
- authenticated user with linked booking access
- in practice: booking leader or accepted participant
- request must include a `bookingId` the caller can access

### Read inquiry
Allowed for:
- booking leader
- accepted participants
- org operators

### Post inquiry message
Allowed for:
- booking leader
- accepted participants
- org operators

### Close inquiry
Allowed for:
- booking leader
- accepted participants
- org operators

Endpoints:
- POST `/occurrences/{occurrenceId}/inquiries`
- GET `/inquiries/{inquiryId}`
- GET `/inquiries/{inquiryId}/messages`
- POST `/inquiries/{inquiryId}/messages`
- POST `/inquiries/{inquiryId}/close`

---

## 9. Reviews

### Public
- GET `/instructors/{instructorProfileId}/reviews/summary`
- GET `/tours/{tourId}/reviews/summary`
- GET `/organizations/{orgId}/reviews/summary`

### Eligible participant only
Review creation requires:
- accepted booking participation or booking leader
- attendance status ATTENDED
- review eligibility rule satisfied

Endpoints:
- POST `/occurrences/{occurrenceId}/reviews/tour`
- POST `/occurrences/{occurrenceId}/reviews/instructor`

### Restricted review content access
- GET `/reviews/{reviewId}`
- GET `/organizations/{orgId}/reviews`

Allowed for:
- review author
- org operators
- reviewed instructor (only for instructor review)

### Organization operator aggregation
- GET `/operator/organizations/{orgId}/reviews/summary`

Allowed for:
- ORG_ADMIN
- ORG_OWNER

Current rule:
- public summary는 published tour 하위 occurrence review만 집계한다.
- operator summary는 same-org 모든 occurrence review를 집계한다.

---

## 10. Reports

### ORG_ADMIN / ORG_OWNER
- GET `/organizations/{orgId}/reports/occurrences`
- GET `/organizations/{orgId}/reports/bookings`

---

## 11. Moderation

MVP decision as of 2026-03-19:
- keep platform moderation disabled
- runtime and OpenAPI do not expose moderation endpoints
- emergency handling stays in manual operational process, not public API contract

---

## Authorization Guard Design

Backend should implement reusable authorization guards.

Recommended guards:

- `requireAuth()`
- `requireOrgRole(orgId, roles)`
- `requireBookingLeader(bookingId)`
- `requireBookingParticipant(bookingId)`
- `requireAcceptedParticipant(bookingId)`
- `requireInvitee(participantId)`
- `requireInquiryAccess(inquiryId)`
- `requireReviewAccess(reviewId)`
- `requireAssetOwner(assetId)`

### Guard priority
Preferred order:

1. authenticate principal
2. resolve resource ownership
3. evaluate relationship (leader / participant / invitee / instructor / org operator)
4. allow or deny

---

## Recommended Authorization Resolution by Resource

### Booking
Resolve:
- `booking.organization_id`
- `booking.leader_user_id`

Then check:
- leader?
- accepted participant?
- org admin/operator?

### Inquiry
Resolve:
- `inquiry.organization_id`
- linked booking participants

Then check:
- leader?
- accepted participant?
- org operator?

### Review
Resolve:
- `review.organization_id`
- `review.author_user_id`
- `review.instructor_profile_id`

Then check:
- author?
- reviewed instructor?
- org operator?

### Tour / Occurrence
Resolve:
- `organization_id`

Then check:
- public visibility?
- org operator?

### Asset
Resolve:
- `owner_user_id`
- optionally linked resource ownership

---

## Denial Behavior

### 401 Unauthorized
Return when:
- token missing
- token invalid
- token expired

### 403 Forbidden
Return when:
- token valid
- principal authenticated
- but principal lacks permission

### 404 Not Found
Recommended when:
- resource does not exist
- or access should not reveal resource existence

MVP recommendation:
- use 404 for hidden resources where enumeration risk matters
- use 403 when the resource is expected to be known to the caller and explicit denial is acceptable

---

## Security Notes

### 1. Never trust client role claims alone
Even if JWT contains roles, sensitive resource checks must verify current organization membership or ownership from database/cache.

### 2. Denormalized organization_id must be trusted carefully
For `bookings.organization_id` and similar denormalized fields:
- set only on server
- validate on creation/update
- reconcile with background jobs

### 3. Invitation endpoints are sensitive
Invitation accept/decline must validate:
- invite target identity
- invitation status
- expiration time

### 4. Review body is private
Public APIs must never expose review body accidentally.

### 5. Asset URLs should not be blindly public
Prefer signed URLs or ownership checks for asset delivery.

---

## MVP Summary

### Public read
- published tours
- visible occurrences
- search
- rating summary
- public announcements

### User-owned access
- own profile
- own notifications
- own favorites
- own bookings (leader)
- accepted participant booking/inquiry access
- eligible review creation

### Organization operator access
- manage tours
- manage occurrences
- approve/reject bookings
- manage waitlist
- manage inquiries
- access private reviews
- manage announcements
- export rosters
- view reports
- manage policies
- write internal notes

### Platform admin access (optional)
- moderation only
