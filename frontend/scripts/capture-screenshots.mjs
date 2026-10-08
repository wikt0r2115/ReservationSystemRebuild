import { chromium } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '../..');
const baseUrl = process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:8088';
const env = Object.fromEntries(readFileSync(resolve(root, '.env'), 'utf8')
  .split('\n')
  .filter((line) => line.includes('=') && !line.startsWith('#'))
  .map((line) => {
    const position = line.indexOf('=');
    return [line.slice(0, position), line.slice(position + 1)];
  }));

const browser = await chromium.launch();
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  await page.goto(baseUrl);
  await page.locator('.offer-card').first().waitFor();
  await page.screenshot({ path: resolve(root, 'docs/screenshots/customer-booking.png') });

  const mobile = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, isMobile: true, hasTouch: true });
  await mobile.goto(baseUrl);
  await mobile.locator('.offer-card').first().waitFor();
  await mobile.screenshot({ path: resolve(root, 'docs/screenshots/mobile-booking.png'), fullPage: true });

  if (process.env.CAPTURE_CREATE_PENDING === '1') {
    const loginResponse = await fetch(`${baseUrl}/auth-api/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'jan@example.com', password: env.DEMO_CUSTOMER_PASSWORD }),
    });
    if (!loginResponse.ok) throw new Error(`customer login: HTTP ${loginResponse.status}`);
    const { token } = await loginResponse.json();
    const reservationResponse = await fetch(`${baseUrl}/booking-api/api/v1/reservations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ availabilitySlotId: 900001, customerName: 'Demo Customer',
        customerEmail: 'jan@example.com', partySize: 2 }),
    });
    if (reservationResponse.status !== 201) throw new Error(`reservation seed: HTTP ${reservationResponse.status}`);
  }

  await page.goto(`${baseUrl}/admin`);
  await page.getByLabel('Admin email').fill('admin@example.com');
  await page.getByLabel('Admin password').fill(env.DEMO_ADMIN_PASSWORD);
  await page.getByRole('button', { name: 'Login', exact: true }).click();
  await page.getByText('Administrator signed in').waitFor();
  await page.screenshot({ path: resolve(root, 'docs/screenshots/admin-workspace.png') });

} finally {
  await browser.close();
}
