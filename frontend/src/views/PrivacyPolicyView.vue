<script setup lang="ts">
import { useI18n } from 'vue-i18n';
const { t } = useI18n();
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-12 sm:py-16">
    <p class="micro-label text-ink-3 mb-2">{{ t('legal.legal') }}</p>
    <h1 class="display-headline text-3xl text-ink mb-2">
      {{ t('legal.privacyTitle') }}
    </h1>
    <p class="text-sm text-ink-3 mb-10">{{ t('legal.lastUpdated') }}</p>

    <div class="flex flex-col gap-8 text-sm text-ink-2 leading-relaxed">
      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">1. Who we are</h2>
        <p>
          Baltnami is a real estate listings platform operated by [Your Name],
          located in Latvia. For questions about this policy or your data,
          contact us at
          <a
            href="mailto:privacy@baltnami.lv"
            class="text-ink underline underline-offset-2"
          >
            privacy@baltnami.lv
          </a>
        </p>
        <p>
          We are the data controller for personal data collected through this
          platform, as defined under Regulation (EU) 2016/679 (GDPR).
        </p>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">
          2. What data we collect
        </h2>
        <p>We collect only what is necessary to provide the service:</p>
        <ul class="flex flex-col gap-2 pl-4">
          <li class="list-disc">
            <span class="text-ink font-medium">Account data</span> — your full
            name, email address, and hashed password when you register.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Listing data</span> — property
            details you submit including title, description, price, address,
            geographic coordinates, and photos.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Contact data</span> — phone
            number, if you choose to provide it on your listing.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Usage data</span> — properties
            you save, your session tokens (stored as HttpOnly cookies, invisible
            to JavaScript).
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Server logs</span> — IP address
            and request metadata retained for security purposes.
          </li>
        </ul>
        <p>
          We do not use third-party analytics, advertising trackers, or sell
          your data to anyone.
        </p>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">
          3. Legal basis for processing
        </h2>
        <ul class="flex flex-col gap-2 pl-4">
          <li class="list-disc">
            <span class="text-ink font-medium"
              >Contract performance (Art. 6(1)(b))</span
            >
            — account creation, authentication, and listing management are
            necessary to provide the service you signed up for.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium"
              >Legitimate interest (Art. 6(1)(f))</span
            >
            — server logs and security measures to protect the platform and its
            users.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium"
              >Legal obligation (Art. 6(1)(c))</span
            >
            — we may retain certain data if required by Latvian or EU law.
          </li>
        </ul>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">
          4. How long we keep your data
        </h2>
        <ul class="flex flex-col gap-2 pl-4">
          <li class="list-disc">
            Account data is kept for as long as your account is active. Accounts
            inactive for 2 years with no active listings are deleted.
          </li>
          <li class="list-disc">
            Active listings remain published until you remove them or delete
            your account.
          </li>
          <li class="list-disc">
            Password reset and email verification tokens are deleted after use
            or expiry.
          </li>
          <li class="list-disc">Server logs are retained for up to 90 days.</li>
        </ul>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">
          5. Who we share data with
        </h2>
        <p>We do not sell your data. We share it only with:</p>
        <ul class="flex flex-col gap-2 pl-4">
          <li class="list-disc">
            <span class="text-ink font-medium"
              >Cloud infrastructure providers</span
            >
            — our hosting provider and object storage (for photos) process data
            on our behalf under data processing agreements.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Other users</span> — your listing
            content (address, photos, contact details you choose to provide) is
            visible to all visitors of the platform.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Authorities</span> — if required
            by law or to prevent fraud.
          </li>
        </ul>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">
          6. Your rights under GDPR
        </h2>
        <p>As an EU resident you have the right to:</p>
        <ul class="flex flex-col gap-2 pl-4">
          <li class="list-disc">
            <span class="text-ink font-medium">Access</span> — request a copy of
            all personal data we hold about you.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Rectification</span> — correct
            inaccurate data.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Erasure</span> — delete your
            account and all associated data via Settings → Delete account, or by
            contacting us.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Portability</span> — receive your
            data in a machine-readable format.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Object</span> — object to
            processing based on legitimate interest.
          </li>
          <li class="list-disc">
            <span class="text-ink font-medium">Lodge a complaint</span> — with
            the Latvian data protection authority, Datu valsts inspekcija (DVI),
            at
            <a
              href="https://www.dvi.gov.lv"
              target="_blank"
              class="text-ink underline underline-offset-2"
              >dvi.gov.lv</a
            >.
          </li>
        </ul>
        <p>
          To exercise any right, email us at
          <a
            href="mailto:privacy@baltnami.lv"
            class="text-ink underline underline-offset-2"
            >privacy@baltnami.lv</a
          >. We respond within 30 days.
        </p>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">7. Cookies</h2>
        <p>
          We use only strictly necessary cookies — session tokens stored as
          HttpOnly cookies to keep you signed in. These are exempt from consent
          requirements under ePrivacy rules as they are essential to the
          service. We do not use tracking or advertising cookies.
        </p>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">8. Data security</h2>
        <p>
          Passwords are stored as bcrypt hashes and never in plaintext. All data
          is transmitted over HTTPS. Session tokens are HttpOnly and not
          accessible to JavaScript. We apply reasonable technical and
          organisational measures to protect your data against unauthorised
          access.
        </p>
      </section>

      <section class="flex flex-col gap-3">
        <h2 class="text-base font-semibold text-ink">
          9. Changes to this policy
        </h2>
        <p>
          We may update this policy. If changes are material, we will notify you
          by email or a notice on the platform. The date at the top of this page
          reflects the last update.
        </p>
      </section>
    </div>
  </div>
</template>
