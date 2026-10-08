# Bank-Grade Secure Auth & Core Banking Sync System

A highly secure, full-stack banking application demonstrating enterprise-grade security protocols, robust authentication mechanisms, and an automated Core Banking synchronization architecture. 

## 🏗 Architecture Overview
This project is divided into two primary phases:
1. **The Pre-Login Phase**: A zero-trust authentication layer enforcing maximum security standards.
2. **The Post-Login Phase**: A secure, IDOR-proof data retrieval system that syncs customer accounts from a simulated Core Banking API.

---

## 🛡️ Phase 1: Authentication & Security (Max Level)
The authentication layer is built with a zero-trust mindset, assuming that tokens can be stolen and APIs will be probed.

* **Argon2id Password Hashing**: Utilizes the industry standard for password storage, protecting against brute-force and GPU cracking attacks.
* **Asymmetric JWT Signing (RSA-512)**: Unlike standard HS256, tokens are signed with a private key and verified with a public key. If a downstream microservice is compromised, the attacker cannot forge new tokens.
* **Refresh Token Rotation & Theft Detection**: Access tokens are heavily restricted (5-minute expiry). Refresh tokens are rotated upon use. If a revoked refresh token is re-used, the system assumes token theft and aggressively invalidates the entire token family.
* **Multi-Factor Authentication (MFA)**: Time-based One-Time Password (TOTP) implementation using HMAC-SHA1 (RFC 6238). Users must set up an authenticator app via QR code.
* **Brute-Force & Account Lockout Strategy**: Tracks failed login attempts and implements progressive lockouts to prevent credential stuffing.
* **Device Fingerprinting**: Tracks the user's `User-Agent` and IP. Logs anomalies if the user logs in from an unrecognized device.
* **JWT Password Versioning**: If a user changes their password, a `passwordVersion` integer is incremented in the DB. The `JwtAuthenticationFilter` validates this, instantly revoking all active sessions across all devices.
* **Information Leakage Prevention**: Global Exception Handlers ensure stack traces or internal DB errors are never exposed to the client (e.g., returning generic `401`/`403`/`428` status codes).

---

## 🏦 Phase 2: Core Banking Data Sync (Post-Login)
Fetching banking data (CASA Accounts) safely requires strict mapping and concurrency controls.

* **IDOR Prevention (Insecure Direct Object Reference)**: The API does NOT accept a `customerId` or `accountId` in the request parameters. Instead, the `CIF` (Customer Information File number) is securely injected as a claim inside the JWT. The backend extracts the `CIF` from the `SecurityContext`, making it mathematically impossible for User A to view User B's accounts.
* **Core Banking Stub Service**: Simulates an external gRPC/HTTP call to a legacy Core Banking system to fetch live account data.
* **The "Diff & Sync" Engine**: 
  1. The API fetches the user's accounts from the local DB.
  2. The API fetches the latest account data from the Core Banking Stub.
  3. A strict `Comparator` runs field-by-field diffs (checking for Balance changes, Status changes to `DORMANT`/`INACTIVE`, etc.).
  4. If a mismatch is detected, the Local DB is synced and updated to match the Core Banking system.
* **Optimistic Locking (`@Version`)**: The JPA `Account` entity implements `@Version`. If two concurrent sync requests attempt to update the account database at the same exact millisecond, Hibernate will reject the conflicting transaction, preventing race conditions.

---

## 💻 Frontend UI (React + Vite)
The presentation layer is designed with a premium, dynamic "Glassmorphism" aesthetic.

* **Technology**: React, Vite, Vanilla CSS.
* **MFA Onboarding Flow**: Dynamically generates QR codes using external APIs for Google Authenticator/Authy setup.
* **Dashboard Logic**: Seamlessly handles loading states and automatically formats currency (e.g., USD, BDT) based on the synced account data. Badges dynamically render status changes (e.g., Red for `INACTIVE`/`DORMANT`, Green for `ACTIVE`).

---

## 🚀 Tech Stack
* **Backend**: Java 17, Spring Boot 3, Spring Security, Spring Data JPA
* **Database**: H2 (In-Memory Development) / Ready for PostgreSQL (Production)
* **Frontend**: Node, React, Vite
* **Cryptography**: JJWT (RSA-512), BouncyCastle (Argon2), TOTP/HMAC-SHA1

## ⚙️ How to Run
1. **Backend**: Open terminal in the root directory and run `mvn spring-boot:run`. The H2 database will initialize automatically.
2. **Frontend**: Open terminal in the `frontend` directory, run `npm install`, then `npm run dev`.
3. Navigate to `http://localhost:5173`.
4. Log in using the default seeded user:
   * **Username**: `bankuser`
   * **Password**: `UserSecure#2026!`
  


## Screen shot of project 
<img width="1470" height="956" alt="Screenshot 2026-10-08 at 4 25 32 PM" src="https://github.com/user-attachments/assets/f449773d-fb28-4735-a74a-59263ec63725" />

<img width="1470" height="956" alt="Screenshot 2026-10-08 at 4 26 09 PM" src="https://github.com/user-attachments/assets/9e23af18-498c-4e05-8415-7e0f68ddf27c" />

<img width="1470" height="956" alt="Screenshot 2026-10-08 at 4 26 38 PM" src="https://github.com/user-attachments/assets/38c87454-8bd2-45c4-83af-c45cb744acb3" />
<img width="1470" height="956" alt="Screenshot 2026-10-08 at 4 28 04 PM" src="https://github.com/user-attachments/assets/daa08e67-d97c-44a6-9d3d-b8f9d3919ba6" />
<img width="1470" height="956" alt="Screenshot 2026-10-08 at 4 29 18 PM" src="https://github.com/user-attachments/assets/360a9ff8-ce89-4c38-8963-a5f9688c1221" />
<img width="1470" height="956" alt="Screenshot 2026-10-08 at 4 30 24 PM" src="https://github.com/user-attachments/assets/bf324327-7025-4185-be5d-8864506de405" />


