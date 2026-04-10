# Frontend Sequential Checklist

## 1) Foundation
- [x] Create isolated frontend app in `frontend/`.
- [x] Setup TypeScript + Vite + React Router.
- [x] Setup global API client and auth token handling.

## 2) Auth Flow
- [x] Register screen with required backend fields.
- [x] Verify email screen using token query param.
- [x] Login screen with token + refresh token persistence.
- [x] Logout action and session reset.

## 3) Public Browsing Flow
- [x] Home screen with trending list and movie grid.
- [x] Search/filter on movie listing.
- [x] Movie detail screen.
- [x] Public review listing on movie detail.

## 4) User Personalization Flow
- [x] Profile view/update flow.
- [x] Continue watching flow.
- [x] Recommendation flow for current user.
- [x] Watch page with protected streaming playback.

## 5) Admin Flow
- [x] Admin movie list screen.
- [x] Admin create movie form.
- [x] Admin edit movie form.
- [x] Admin soft delete action.

## 6) Error and Guard Rails
- [x] Global route guard for auth/admin.
- [x] 401 refresh strategy and fallback logout.
- [x] Error rendering based on backend `ErrorResponse`.

## 7) Build Validation
- [x] Install dependencies.
- [x] Build passes.
