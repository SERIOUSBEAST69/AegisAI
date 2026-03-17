# Role Permission E2E Live Result (2026-03-17)

## Scope
- Environment under test: running backend on `http://localhost:8080` (live listener).
- Token source: real login accounts shown in login UI and backend demo seeds.
- Roles tested: ADMIN, EXECUTIVE, SECOPS, DATA_ADMIN, AI_BUILDER, BUSINESS_OWNER, EMPLOYEE.
- Check size: 21 endpoints x 7 roles = 147 checks.

## Summary
- Total checks: 147
- Mismatches: 69
- Login failures: none
- Raw matrix CSV: docs/role-permission-e2e-results.csv

## High-Severity Findings
1. Broad authorization mismatch in live backend runtime.
- Many endpoints expected to be forbidden are currently callable by non-target roles.
- Representative examples:
  - `GET /api/user/list` callable by DATA_ADMIN.
  - `GET /api/data-asset/list` callable by EXECUTIVE/SECOPS/AI_BUILDER/BUSINESS_OWNER/EMPLOYEE.
  - `GET /api/alert/list` callable by EXECUTIVE/DATA_ADMIN/AI_BUILDER/BUSINESS_OWNER/EMPLOYEE.
  - `GET /api/security/events` callable by EXECUTIVE/DATA_ADMIN/AI_BUILDER/BUSINESS_OWNER/EMPLOYEE.

2. Employee identity was incomplete in runtime data.
- EMPLOYEE role was missing and had to be created manually for full 7-role test.
- Employee account was then registered to complete the matrix run.

## Root Cause Notes
1. Runtime/deployment mismatch.
- The live backend on `8080` appears not fully aligned with current repository changes (method security behavior and seeded role set differ from expected state).

2. Confirmed code-level privilege issue fixed in repository.
- `CurrentUserService.requireAdmin()` previously allowed roles whose role name contains "管理员".
- This would incorrectly grant admin privileges to DATA_ADMIN.
- Fixed by restricting admin check to:
  - username `admin`, or
  - role code strictly `ADMIN`.

## What Was Changed During This Run
1. Added missing EMPLOYEE role in live runtime (for test completeness).
2. Registered `employee.demo` account in live runtime (for test completeness).
3. Patched code to prevent admin escalation in:
- backend/src/main/java/com/trustai/service/CurrentUserService.java

## Recommendation
1. Redeploy backend from current repository commit and rerun the same matrix.
2. Ensure method security is active in deployed runtime (`@EnableMethodSecurity`).
3. Re-seed role catalog to include EMPLOYEE and remove deprecated SCHOOL_ADMIN.
4. Re-run checklist in docs/role-permission-e2e-checklist.md and compare mismatch count target: 0.
