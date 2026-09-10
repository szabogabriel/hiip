# HIIP Application Backlog

## Current Product Direction

This backlog contains current priorities and intended behavior. Items remain unchecked until their implementation and tests are present in the repository.

Current priority order:

1. A useful web UI for the primary data workflow
2. Reliable persistent storage
3. Security hardening
4. Collaboration and sharing refinements

The REST API remains the backend boundary for web, mobile, and third-party clients. Users should not need to call the API directly when using the web UI.

### Product Decisions

- Users are created by administrators only. Public registration is out of scope.
- The web UI is the primary user-facing surface. The API remains important for integrations.
- Soft deletion is intentional and remains the normal delete behavior for now.
- Sharing a category works like sharing a folder: access is inherited by all descendants.
- Read access never grants permission to create or modify data.
- Global-category creation is configurable and defaults to admin-only.
- Data ownership remains with the creator even when its category is shared.

## Priority 1: Primary UI Workflow

### [ ] 1.1 Complete the data workspace

Build the UI around storing and retrieving JSON data.

Required behavior:

- Log in and maintain the authenticated session.
- Refresh an expired access token using the refresh token.
- Display the current user and accessible categories.
- Create entries with JSON content, tags, and an optional category.
- Edit and soft-delete entries owned by the current user.
- View both owned data and data shared with the user.
- Search by one or more tags and category path, including supported wildcards.
- Display useful validation and authorization errors instead of raw server responses.
- Return the user to login when tokens are expired, revoked, or invalid.

Acceptance criteria:

- A normal user can log in, create, search, edit, and soft-delete data without curl or Swagger.
- A user cannot edit or delete another user's data through the UI.
- Read-only shared data is visibly distinguishable from editable owned data.
- Loading, empty, validation, authorization, and request-failure states are represented.

### [ ] 1.2 Category management in the UI

- Display only categories accessible to the current user.
- Display descendants of shared categories.
- Allow permitted users to create categories and child categories.
- Prevent category creation below a read-only category.
- Allow owners to share and unshare categories.
- Allow owners to choose read or read/write permission.
- Display inherited access where practical.
- Separate global, owned, and shared categories in the interface.
- Show global-category controls only when policy and role permit them.

### [ ] 1.3 Admin user management in the UI

Administrators must be able to manage users from the UI because account creation is intentionally admin-only.

- List active users and optionally inactive users.
- Create users with username, email, password, role, and active status.
- Edit user details and roles.
- Activate and deactivate users.
- Display lockout status where relevant.
- Hide administrative controls from normal users and enforce the restriction server-side.
- Show duplicate username/email and weak-password validation errors.

## Priority 2: Category Permissions and Sharing

### [ ] 2.1 Inherited category access

Sharing `work` as read-only must allow the recipient to read data in `work`, `work/projects`, and `work/projects/client-a`. The recipient must not be able to create, edit, move, or delete data in that subtree, nor create child categories below it.

Implementation requirements:

- Determine access using category ancestry, not only the exact data category.
- Apply inheritance to category lists, searches, trees, ID lookups, and data searches.
- Unsharing a parent removes inherited access from descendants unless another explicit share grants access.
- Define deterministic precedence when multiple shares apply. Recommended behavior: the most specific applicable share controls the descendant, while ownership always takes precedence.

### [ ] 2.2 Enforce read/write permissions in every mutation path

Permission checks must be in the service layer and must not depend on the UI.

- Owners may use their own categories.
- Users may use categories shared with write permission.
- Global-category access follows the configured global-category policy.
- Read-only users receive a forbidden response when creating or updating data in the category.
- Assigning an existing category must perform the same checks as creating a category.
- Moving data must validate ownership and destination-category permission.
- Only owners or write-authorized collaborators may create children or modify sharing.
- Global categories cannot be modified by ordinary users unless policy explicitly allows it.

### [ ] 2.3 Secure category reads

- Require authentication for all category endpoints unless explicitly documented as public.
- Replace permissive catch-all security with authenticated-by-default behavior.
- Filter flat lists, roots, trees, searches, and ID lookups by effective user access.
- Do not expose unrelated private category names, paths, owners, or share lists.
- Show share details only to the owner or an explicitly authorized administrator.

## Priority 3: Reliable Persistence

### [ ] 3.1 Establish the production database path

The current default is in-memory H2, so data is lost on restart unless another datasource is configured. Add a documented PostgreSQL deployment path.

- PostgreSQL is the recommended production database.
- H2 remains available for local development and automated tests.
- Production startup validates required datasource configuration.
- Production schema changes use Flyway or Liquibase rather than `ddl-auto=update`.
- Transactions cover category creation, sharing, data mutation, and password-reset updates.
- Add indexes for owner, category, hidden/deleted state, tags, and sharing queries.

### [ ] 3.2 Document backup and recovery

Document and verify database backup, restore, recovery expectations, migration recovery, and administrative data export.

### [ ] 3.3 Preserve soft deletion and add future retention

Keep soft deletion as the current behavior:

- Deleted entries are hidden from normal lists and searches.
- Repeated deletion is idempotent.
- Ownership rules still apply to deletion.
- API documentation clearly states that delete means hide, not permanent removal.

Future enhancement:

- Add `deletedAt` or an equivalent lifecycle state.
- Add restore before expiry.
- Add configurable retention, with 30 days as the initial example.
- Permanently delete expired records through an admin-controlled scheduled job.
- Never remove active or restored data during cleanup.

## Priority 4: Authentication and Security Hardening

### [ ] 4.1 Harden deployment security

- Remove the usable hardcoded JWT secret default.
- Require a strong environment-provided secret in production and fail startup when it is missing or weak.
- Isolate development defaults from production configuration.
- Replace default admin credentials before production use.
- Disable the H2 console in production.
- Protect undocumented and future routes by default.

### [ ] 4.2 Complete the refresh-token lifecycle

- Revoke access and refresh tokens on logout, or use a server-side token-family model.
- Check revocation when a refresh token is used.
- Rotate refresh tokens safely and reject replayed tokens when rotation is enabled.
- Revoke active token families when an account is deactivated or its password is reset.
- Schedule and verify cleanup of expired revoked tokens.

### [ ] 4.3 Complete password reset and account recovery

- Integrate a real email provider instead of printing reset tokens to stdout.
- Configure provider credentials and reset URLs through environment configuration.
- Send the resolved user's email address, not the original username-or-email input.
- Keep reset requests indistinguishable for existing and unknown users.
- Add reset-token rate limiting and atomic token invalidation.
- Add password-reset UI after the primary data workflow.

## Priority 5: Automated Verification

### [ ] 5.1 Add automated verification

The current Maven build succeeds without compiling tests. Add real tests before considering the implementation complete.

- Service tests for inherited access and permission decisions.
- Repository tests for ownership, hidden data, categories, tags, and sharing.
- Controller tests for authentication and HTTP status behavior.
- Security tests proving category endpoints reject anonymous requests.
- Tests proving users cannot access unrelated categories or data.
- Tests proving read-only sharing blocks create and update operations.
- Tests for multiple levels of inherited access and unsharing.
- Tests proving global-category policy defaults to admin-only.
- Authentication tests for lockout, password reset, logout, refresh, and revocation.
- PostgreSQL or production-compatible persistence tests.

The Maven test command must report executed tests rather than only `No sources to compile`.

## Deferred Features

These remain lower priority until the primary UI and persistence path are reliable:

- Bulk create, update, and delete
- JSON/CSV/XML import and export
- Attachments and external file storage
- Rich text and content templates
- Notifications and notification preferences
- Favorites/bookmarks and user preferences
- Caching and Redis integration
- Full-text search and search analytics
- Webhooks and third-party event integrations
- CLI administration and backup tools
- Mobile and desktop clients
- Usage analytics and reporting
- Multi-tenancy, quotas, and billing
- GDPR and formal compliance workflows

Deferred items must not be marked complete until their implementation and tests exist in the repository.

## [ ] 4.4 Add policy configuration

The following properties describe the required policy. Exact names may change during implementation, but the defaults and behavior should remain documented:

```properties
# Only administrators may create global categories by default.
hiip.categories.global-creation-role=ADMIN

# Future soft-delete retention; disabled until restore and cleanup exist.
hiip.data.retention.enabled=false
hiip.data.retention.days=30

# Production requires an externally supplied secret.
hiip.jwt.require-external-secret=true
```

## Implementation Guidance

1. Implement and test effective category access, including inherited permissions.
2. Enforce category permissions in service-layer mutation paths.
3. Secure and filter category endpoints.
4. Complete the primary UI data workflow and admin user management.
5. Establish PostgreSQL migrations, transactions, backup, and restore documentation.
6. Add authentication hardening and password-reset delivery.
7. Add deferred features only after the preceding behavior is covered by tests.

