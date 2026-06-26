# Privacy Policy

_Last updated: [DATE]_

---

## 1. Introduction

This Privacy Policy explains how Nikita Kuprins ("we", "us", or "our"), operating the [WEBSITE_URL] platform ("Baltnami"), collects, uses, stores, and protects your personal data. We are committed to complying with the General Data Protection Regulation (EU) 2016/679 ("GDPR") and the laws of the Republic of Latvia.

We collect only the data necessary to provide our service and do not use tracking technologies or third-party advertising.

---

## 2. Data Controller

The data controller for all personal data processed through this platform is:

**Nikita Kuprins**
Email: nikita.kuprins@gmail.com

You may contact us at the address above for any privacy-related requests or questions.

---

## 3. Data We Collect and Why

### 3.1 Account Registration

When you create an account, we collect:

- **Full name** — to identify your account and display on listings
- **Email address** — for account authentication, email verification, and password recovery
- **Password** — stored exclusively as a one-way cryptographic hash (BCrypt); we cannot recover your original password

_Legal basis: Article 6(1)(b) GDPR — processing is necessary to perform the contract with you (providing the platform service)._

### 3.2 Property Listings

When you create a property listing, we collect and publish:

- Property details: address, city, district, price, number of rooms, floor area, land area, floor number, year built, property features
- Geographic coordinates (latitude and longitude) for map display
- Contact phone number(s) you provide for the listing
- Listing title and description in the languages you enter
- Photos and floor plans you upload
- An optional video URL (e.g., a YouTube link)

**Important:** Property listings — including contact phone numbers and property addresses — are publicly visible to all visitors of the platform without requiring an account. Only include contact information you intend to make publicly available.

_Legal basis: Article 6(1)(b) GDPR — processing is necessary to perform the service you requested (publishing a property listing)._

### 3.3 Saved Properties

When you save a property listing, we store a record linking your account to that listing, along with the date and time it was saved.

_Legal basis: Article 6(1)(b) GDPR — contract performance._

### 3.4 Authentication Tokens

To keep you securely logged in, we store:

- A **refresh token identifier** (stored as a one-way hash) in our database, valid for 7 days
- An **access token** delivered as a short-lived (15-minute) HTTP-only browser cookie

These tokens contain or reference only your internal account identifier (a pseudonymous UUID) and carry no additional personal data.

_Legal basis: Article 6(1)(b) GDPR — contract performance._

### 3.5 Email Verification and Password Reset

When you register or request a password reset, we generate a one-time token (stored as a cryptographic hash, never in plain text) and send you a link by email. These tokens expire after 24 hours (verification) or 1 hour (password reset).

_Legal basis: Article 6(1)(b) GDPR — contract performance._

### 3.6 Transactional Emails

We send emails only for:

- Email address verification upon registration
- Password reset upon your request

We do not send marketing emails. Your email address is not used for any purpose beyond account management.

_Legal basis: Article 6(1)(b) GDPR — contract performance._

---

## 4. Cookies

We use two cookies, both of which are strictly necessary for the platform to function:

| Cookie          | Purpose                                                             | Duration   |
| --------------- | ------------------------------------------------------------------- | ---------- |
| `access_token`  | Authenticates your API requests                                     | 15 minutes |
| `refresh_token` | Allows your session to be renewed without re-entering your password | 7 days     |

Both cookies are **HTTP-only** (inaccessible to JavaScript), scoped to this platform's domain, and set with `SameSite=Lax` to mitigate cross-site request forgery risks.

Because these cookies are strictly necessary for authentication — a service explicitly requested by you — they do not require your prior consent under the ePrivacy Directive. We do not use any analytics, advertising, tracking, or third-party cookies.

---

## 5. Third-Party Service Providers

We share data with the following processors, each bound by a Data Processing Agreement and/or Standard Contractual Clauses:

### 5.1 Amazon Web Services (AWS)

- **Purpose:** Storage of property photos and floor plans; content delivery via CDN
- **Data processed:** Image files you upload; content delivery logs
- **Location:** Primary storage in Stockholm, Sweden (EU/EEA); CDN edge nodes may serve content globally
- **Safeguards:** AWS GDPR Data Processing Addendum; Standard Contractual Clauses for any transfers outside the EEA

### 5.2 Resend

- **Purpose:** Delivery of transactional emails (verification and password reset)
- **Data processed:** Your email address; the content of verification and reset emails
- **Location:** United States (third country)
- **Safeguards:** Standard Contractual Clauses (Module 2: Controller to Processor); Resend Data Processing Agreement

No other third parties receive your personal data. We do not use Google Analytics, social media pixels, advertising networks, or any other tracking services.

---

## 6. Data Retention

| Data                                  | Retention period                                                                                                                   |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Account data (name, email)            | Until you delete your account, or automatically after 2 years of account inactivity with no active listings (whichever is earlier) |
| Property listings and associated data | Until you delete the listing or your account                                                                                       |
| Uploaded photos and floor plans       | Deleted from storage within hours of listing or account deletion                                                                   |
| Saved property records                | Until you remove the save or your account is deleted                                                                               |
| Access token cookie                   | 15 minutes                                                                                                                         |
| Refresh token                         | 7 days (or until logout)                                                                                                           |
| Email verification tokens             | 24 hours                                                                                                                           |
| Password reset tokens                 | 1 hour                                                                                                                             |

**Automatic inactivity deletion:** If your account has had no activity for 2 consecutive years and you have no active property listings, your account and all associated data are automatically and permanently deleted. We will send a notification email to your registered address before deletion occurs.

---

## 7. Your Rights Under GDPR

As a data subject under the GDPR, you have the following rights:

**7.1 Right of Access (Art. 15)** — You may request a copy of all personal data we hold about you.

**7.2 Right to Rectification (Art. 16)** — You may correct inaccurate data or complete incomplete data held about you.

**7.3 Right to Erasure (Art. 17)** — You may delete your account and all associated data at any time from your account settings. Deletion is immediate and permanent.

**7.4 Right to Restriction (Art. 18)** — You may request that we restrict processing of your data in certain circumstances.

**7.5 Right to Data Portability (Art. 20)** — You may request a machine-readable export of the personal data you have provided to us.

**7.6 Right to Object (Art. 21)** — You may object to processing based on our legitimate interests (Art. 6(1)(f)).

**7.7 Right to Withdraw Consent** — We do not rely on consent as a legal basis for any processing; all processing is based on contract performance or legitimate interest.

To exercise any of these rights, contact us at nikita.kuprins@gmail.com. We will respond within 30 days.

---

## 8. Data Security

We implement the following technical measures pursuant to Article 32 GDPR:

- Passwords are hashed using BCrypt with a high computational cost factor before storage and are never stored in recoverable form
- Authentication uses short-lived cryptographic tokens (15-minute access tokens) delivered exclusively via HTTP-only cookies inaccessible to browser scripts
- Tokens used for password reset and email verification are stored as one-way cryptographic hashes — even in the event of a database breach, these tokens cannot be extracted
- Authentication endpoints are rate-limited to prevent brute-force and credential-stuffing attacks
- All data in transit is encrypted using TLS
- A minimum password length of 15 characters is enforced

---

## 9. Supervisory Authority

If you believe we have violated your data protection rights, you have the right to lodge a complaint with the Latvian supervisory authority:

**Datu valsts inspekcija (Data State Inspectorate)**
Blaumaņa iela 11/13-11, Riga, LV-1011, Latvia
Phone: +371 67 223 131
Web: www.dvi.gov.lv

---

## 10. Children

This platform is not directed at persons under 18 years of age. We do not knowingly collect personal data from minors. If you believe a minor has submitted data, contact us for immediate deletion.

---

## 11. Changes to This Policy

We may update this policy as our platform evolves or legal requirements change. Material changes will be communicated via email to registered users at least 14 days before taking effect. The "Last updated" date at the top of this page reflects the current version.

---

## 12. Contact

For all privacy-related inquiries: nikita.kuprins@gmail.com
