import { expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const values = Object.fromEntries(
  readFileSync(resolve(import.meta.dirname, '../../.env'), 'utf8')
    .split('\n')
    .filter((line) => line.includes('=') && !line.startsWith('#'))
    .map((line) => {
      const separator = line.indexOf('=');
      return [line.slice(0, separator), line.slice(separator + 1)];
    }),
);

test('customer registers, books, admin confirms, customer cancels', async ({ page, browser }) => {
  const email = `e2e-${Date.now()}-${Math.floor(Math.random() * 100000)}@example.com`;
  const password = 'E2e-password-123';

  await page.goto('/');
  await expect(page.locator('.offer-card').getByText('City Discovery Tour')).toBeVisible();
  await page.getByRole('button', { name: 'Customer access' }).click();
  await page.getByRole('button', { name: 'Register', exact: true }).click();
  await page.getByLabel('Display name').fill('E2E Customer');
  await page.getByLabel('Email', { exact: true }).fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByText('Customer session active')).toBeVisible();
  const availableBefore = (await page.locator('.slot-card').first().innerText()).match(/\d+\/\d+ seats/)?.[0];
  expect(availableBefore).toBeTruthy();
  const reservationForm = page.locator('.reservation-form');
  await reservationForm.getByLabel('Name').fill('E2E Customer');
  await reservationForm.getByLabel('Email').fill(email);
  await reservationForm.getByLabel('Party size').fill('1');
  await reservationForm.getByRole('button', { name: 'Reserve' }).click();
  await expect(page.locator('.reservation-result')).toContainText('PENDING');

  await page.reload();
  await expect(page.locator('.reservation-result')).toContainText('PENDING');

  const adminContext = await browser.newContext();
  try {
    const adminPage = await adminContext.newPage();
    await adminPage.goto('/admin');
    await adminPage.getByLabel('Admin email').fill('admin@example.com');
    await adminPage.getByLabel('Admin password').fill(values.DEMO_ADMIN_PASSWORD);
    await adminPage.getByRole('button', { name: 'Login', exact: true }).click();
    const row = adminPage.locator('.reservation-row').filter({ hasText: email });
    await expect(row).toContainText('PENDING');
    await row.getByRole('button', { name: 'Confirm' }).click();
    await expect(row).toContainText('CONFIRMED');
  } finally {
    await adminContext.close();
  }

  await page.locator('.reservation-result').getByRole('button', { name: 'Cancel' }).click();
  await expect(page.locator('.reservation-result')).toContainText('CANCELLED');
  await expect(page.locator('.slot-card').first()).toContainText(availableBefore!);
});

test('backend denies customer access to admin operations and another reservation', async ({ request }) => {
  const password = values.DEMO_CUSTOMER_PASSWORD;
  const login = await request.post('/auth-api/api/v1/auth/login', {
    data: { email: 'jan@example.com', password },
  });
  expect(login.ok()).toBeTruthy();
  const { token } = await login.json();
  const headers = { Authorization: `Bearer ${token}` };

  const adminList = await request.get('/booking-api/api/v1/admin/reservations', { headers });
  expect(adminList.status()).toBe(403);

  const otherEmail = `other-${Date.now()}-${Math.floor(Math.random() * 100000)}@example.com`;
  const register = await request.post('/auth-api/api/v1/auth/register', {
    data: { displayName: 'Other Customer', email: otherEmail, password: 'E2e-password-123' },
  });
  expect(register.status()).toBe(201);
  const otherLogin = await request.post('/auth-api/api/v1/auth/login', {
    data: { email: otherEmail, password: 'E2e-password-123' },
  });
  const otherToken = (await otherLogin.json()).token as string;

  const created = await request.post('/booking-api/api/v1/reservations', {
    headers,
    data: {
      availabilitySlotId: 900001,
      customerName: 'Jan Kowalski',
      customerEmail: 'jan@example.com',
      partySize: 1,
    },
  });
  expect(created.status()).toBe(201);
  const reservationId = (await created.json()).id as number;
  try {
    expect((await request.get(`/booking-api/api/v1/reservations/${reservationId}`, {
      headers: { Authorization: `Bearer ${otherToken}` },
    })).status()).toBe(403);
    expect((await request.delete(`/booking-api/api/v1/reservations/${reservationId}`, {
      headers: { Authorization: `Bearer ${otherToken}` },
    })).status()).toBe(403);
  } finally {
    const cleanup = await request.delete(`/booking-api/api/v1/reservations/${reservationId}`, { headers });
    expect(cleanup.ok()).toBeTruthy();
  }
});

test('expired customer session is cleared and customer cannot enter admin panel', async ({ page, request }) => {
  const login = await request.post('/auth-api/api/v1/auth/login', {
    data: { email: 'jan@example.com', password: values.DEMO_CUSTOMER_PASSWORD },
  });
  expect(login.ok()).toBeTruthy();
  const { token } = await login.json();
  const parts = (token as string).split('.');
  parts[2] = (parts[2][0] === 'a' ? 'b' : 'a') + parts[2].slice(1);
  await page.addInitScript((invalidToken) => localStorage.setItem('reservation.customer.token', invalidToken), parts.join('.'));
  await page.goto('/');
  await expect(page.getByText('Login required to reserve')).toBeVisible();
  await expect(page.locator('.reservation-form').getByRole('button', { name: 'Reserve' })).toBeDisabled();

  await page.goto('/admin');
  await page.getByLabel('Admin email').fill('jan@example.com');
  await page.getByLabel('Admin password').fill(values.DEMO_CUSTOMER_PASSWORD);
  await page.getByRole('button', { name: 'Login', exact: true }).click();
  await expect(page.locator('.error-banner')).toContainText('FORBIDDEN');
  await expect(page.getByRole('button', { name: 'Login', exact: true })).toBeVisible();
});

test('stale availability returns a conflict and refreshes slot list', async ({ page, request }) => {
  const login = await request.post('/auth-api/api/v1/auth/login', {
    data: { email: 'jan@example.com', password: values.DEMO_CUSTOMER_PASSWORD },
  });
  const { token } = await login.json();
  await page.goto('/auth');
  await page.getByLabel('Email', { exact: true }).fill('jan@example.com');
  await page.getByLabel('Password', { exact: true }).fill(values.DEMO_CUSTOMER_PASSWORD);
  await page.getByRole('button', { name: 'Login as customer' }).click();
  await expect(page.getByText('Customer session active')).toBeVisible();
  await expect(page.locator('.slot-card').first()).toBeVisible();

  const slotResponse = await request.get('/availability-api/api/v1/offers/900001/availability');
  const slot = (await slotResponse.json()).find((item: { id: number }) => item.id === 900001);
  expect(slot).toBeTruthy();
  const fill = await request.post('/booking-api/api/v1/reservations', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      availabilitySlotId: slot.id,
      customerName: 'Jan Kowalski',
      customerEmail: 'jan@example.com',
      partySize: slot.capacity - slot.reservedCount,
    },
  });
  expect(fill.status()).toBe(201);
  const reservationId = (await fill.json()).id as number;
  try {
    const form = page.locator('.reservation-form');
    await form.getByLabel('Name').fill('Jan Kowalski');
    await form.getByLabel('Email').fill('jan@example.com');
    await form.getByLabel('Party size').fill('1');
    await form.getByRole('button', { name: 'Reserve' }).click();
    await expect(page.locator('.error-banner')).toContainText('CAPACITY_EXCEEDED');
    await expect(page.locator('.slot-card')).toContainText('0/8 seats');
    await expect(form.getByRole('button', { name: 'Reserve' })).toBeDisabled();
  } finally {
    const cleanup = await request.delete(`/booking-api/api/v1/reservations/${reservationId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(cleanup.ok()).toBeTruthy();
  }
});

test('lost write response is reconciled before customer retries', async ({ page }) => {
  await page.goto('/auth');
  await page.getByLabel('Email', { exact: true }).fill('jan@example.com');
  await page.getByLabel('Password', { exact: true }).fill(values.DEMO_CUSTOMER_PASSWORD);
  await page.getByRole('button', { name: 'Login as customer' }).click();
  await expect(page.getByText('Customer session active')).toBeVisible();
  await expect(page.locator('.slot-card').first()).toBeVisible();

  await page.route('**/booking-api/api/v1/reservations', async (route) => {
    if (route.request().method() === 'POST') {
      const backendResponse = await route.fetch();
      expect(backendResponse.status()).toBe(201);
      await route.abort('failed');
    } else {
      await route.continue();
    }
  });

  const form = page.locator('.reservation-form');
  await form.getByLabel('Name').fill('Jan Kowalski');
  await form.getByLabel('Email').fill('jan@example.com');
  await form.getByLabel('Party size').fill('1');
  await form.getByRole('button', { name: 'Reserve' }).click();
  await expect(page.locator('.error-banner')).toContainText('Latest reservation status was refreshed');
  await expect(page.locator('.reservation-result')).toContainText('PENDING');
  await page.locator('.reservation-result').getByRole('button', { name: 'Cancel' }).click();
  await expect(page.locator('.reservation-result')).toContainText('CANCELLED');
});

test('API outage and invalid registration show recoverable states', async ({ page }) => {
  await page.route('**/offer-api/api/v1/offers', (route) => route.abort('failed'));
  await page.goto('/');
  await expect(page.locator('.error-banner')).toContainText('NETWORK_ERROR');
  await expect(page.getByText('No active offers')).toBeVisible();
  await expect(page.locator('.reservation-form').getByRole('button', { name: 'Reserve' })).toBeDisabled();

  await page.unrouteAll();
  await page.getByRole('button', { name: 'Refresh offers' }).click();
  await expect(page.locator('.offer-card').first()).toBeVisible();
  await expect(page.locator('.error-banner')).toHaveCount(0);

  await page.getByRole('button', { name: 'Customer access' }).click();
  await page.getByRole('button', { name: 'Register', exact: true }).click();
  await page.getByLabel('Display name').fill('Test Customer');
  await page.getByLabel('Email', { exact: true }).fill('invalid-email');
  await page.getByLabel('Password', { exact: true }).fill('E2e-password-123');
  await page.getByRole('button', { name: 'Create account' }).click();
  expect(await page.getByLabel('Email', { exact: true }).evaluate((input: HTMLInputElement) => input.validity.valid)).toBe(false);
  await expect(page.getByRole('heading', { name: 'Customer login' })).toBeVisible();
});
