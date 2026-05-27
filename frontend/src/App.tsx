import {
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  DoorOpen,
  LayoutDashboard,
  Lock,
  LogIn,
  LogOut,
  PlusCircle,
  ShieldCheck,
  TicketCheck,
  UserRound,
  XCircle,
} from 'lucide-react';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { createAvailabilitySlot, listAdminSlots, listOpenSlots } from './api/availability';
import { login, registerCustomer } from './api/auth';
import { ApiRequestError } from './api/client';
import { createOffer, listActiveOffers, listAdminOffers } from './api/offers';
import {
  cancelReservation,
  confirmReservation,
  createReservation,
  listAdminReservations,
  rejectReservation,
} from './api/reservations';
import { apiConfig } from './config';
import type {
  ApiErrorResponse,
  AvailabilitySlot,
  CreateAvailabilitySlotPayload,
  CreateOfferPayload,
  CreateReservationPayload,
  LoginCredentials,
  Offer,
  RegisterCustomerPayload,
  Reservation,
} from './types';

type Screen = 'landing' | 'auth' | 'admin';

type ReservationFormState = {
  customerName: string;
  customerEmail: string;
  partySize: string;
};

type OfferFormState = {
  name: string;
  imageUrl: string;
  description: string;
  price: string;
};

type SlotFormState = {
  offerId: string;
  startsAt: string;
  endsAt: string;
  capacity: string;
};

const emptyReservationForm: ReservationFormState = {
  customerName: '',
  customerEmail: '',
  partySize: '1',
};

const emptyOfferForm: OfferFormState = {
  name: '',
  imageUrl: '',
  description: '',
  price: '100.00',
};

const CUSTOMER_TOKEN_KEY = 'reservation.customer.token';
const ADMIN_TOKEN_KEY = 'reservation.admin.token';

function getScreenFromPath(pathname = window.location.pathname): Screen {
  if (pathname.startsWith('/admin')) {
    return 'admin';
  }
  if (pathname.startsWith('/auth')) {
    return 'auth';
  }
  return 'landing';
}

export function App() {
  const [screen, setScreen] = useState<Screen>(() => getScreenFromPath());
  const [offers, setOffers] = useState<Offer[]>([]);
  const [selectedOfferId, setSelectedOfferId] = useState<number | null>(null);
  const [slots, setSlots] = useState<AvailabilitySlot[]>([]);
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);
  const [reservationForm, setReservationForm] = useState<ReservationFormState>(emptyReservationForm);
  const [offerForm, setOfferForm] = useState<OfferFormState>(emptyOfferForm);
  const [slotForm, setSlotForm] = useState<SlotFormState>(() => createDefaultSlotForm());
  const [lastReservation, setLastReservation] = useState<Reservation | null>(null);
  const [adminReservations, setAdminReservations] = useState<Reservation[]>([]);
  const [adminOffers, setAdminOffers] = useState<Offer[]>([]);
  const [adminSlots, setAdminSlots] = useState<AvailabilitySlot[]>([]);
  const [selectedAdminOfferId, setSelectedAdminOfferId] = useState<number | null>(null);
  const [customerAuthForm, setCustomerAuthForm] = useState<RegisterCustomerPayload>({
    displayName: '',
    email: apiConfig.customerEmail,
    password: apiConfig.customerPassword,
  });
  const [customerLoginForm, setCustomerLoginForm] = useState<LoginCredentials>({
    email: apiConfig.customerEmail,
    password: apiConfig.customerPassword,
  });
  const [adminLoginForm, setAdminLoginForm] = useState<LoginCredentials>({
    email: apiConfig.adminEmail,
    password: apiConfig.adminPassword,
  });
  const [customerToken, setCustomerToken] = useState<string | null>(
    () => localStorage.getItem(CUSTOMER_TOKEN_KEY),
  );
  const [adminToken, setAdminToken] = useState<string | null>(() => localStorage.getItem(ADMIN_TOKEN_KEY));
  const [isLoadingOffers, setIsLoadingOffers] = useState(false);
  const [isLoadingSlots, setIsLoadingSlots] = useState(false);
  const [isSubmittingReservation, setIsSubmittingReservation] = useState(false);
  const [isLoadingAdmin, setIsLoadingAdmin] = useState(false);
  const [isLoadingAdminSlots, setIsLoadingAdminSlots] = useState(false);
  const [isCreatingOffer, setIsCreatingOffer] = useState(false);
  const [isCreatingSlot, setIsCreatingSlot] = useState(false);
  const [isCustomerAuthLoading, setIsCustomerAuthLoading] = useState(false);
  const [isAdminAuthLoading, setIsAdminAuthLoading] = useState(false);
  const [reservationActionId, setReservationActionId] = useState<number | null>(null);
  const [error, setError] = useState<ApiErrorResponse | null>(null);
  const [authTab, setAuthTab] = useState<'login' | 'register'>('login');

  const selectedOffer = useMemo(
    () => offers.find((offer) => offer.id === selectedOfferId) ?? null,
    [offers, selectedOfferId],
  );

  const selectedSlot = useMemo(
    () => slots.find((slot) => slot.id === selectedSlotId) ?? null,
    [slots, selectedSlotId],
  );

  const selectedSlotRemaining = selectedSlot ? selectedSlot.capacity - selectedSlot.reservedCount : 0;

  const adminOfferOptions = useMemo(
    () => (adminOffers.length > 0 ? adminOffers : offers),
    [adminOffers, offers],
  );

  useEffect(() => {
    void loadOffers();
  }, []);

  useEffect(() => {
    if (selectedOfferId === null) {
      setSlots([]);
      setSelectedSlotId(null);
      return;
    }

    void loadSlots(selectedOfferId);
  }, [selectedOfferId]);

  useEffect(() => {
    if (selectedOfferId === null) {
      return;
    }

    setSlotForm((current) => ({
      ...current,
      offerId: String(selectedOfferId),
    }));
  }, [selectedOfferId]);

  useEffect(() => {
    if (customerToken) {
      localStorage.setItem(CUSTOMER_TOKEN_KEY, customerToken);
    } else {
      localStorage.removeItem(CUSTOMER_TOKEN_KEY);
    }
  }, [customerToken]);

  useEffect(() => {
    if (adminToken) {
      localStorage.setItem(ADMIN_TOKEN_KEY, adminToken);
    } else {
      localStorage.removeItem(ADMIN_TOKEN_KEY);
    }
  }, [adminToken]);

  useEffect(() => {
    const handlePop = () => setScreen(getScreenFromPath());
    window.addEventListener('popstate', handlePop);
    return () => window.removeEventListener('popstate', handlePop);
  }, []);

  function navigate(nextScreen: Screen) {
    const nextPath = nextScreen === 'landing' ? '/' : `/${nextScreen}`;
    if (window.location.pathname !== nextPath) {
      window.history.pushState({ screen: nextScreen }, '', nextPath);
    }
    setScreen(nextScreen);
  }

  async function loadOffers() {
    setIsLoadingOffers(true);
    setError(null);
    try {
      const loadedOffers = await listActiveOffers();
      setOffers(loadedOffers);
      setSelectedOfferId((current) => current ?? loadedOffers[0]?.id ?? null);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingOffers(false);
    }
  }

  async function loadSlots(offerId: number) {
    setIsLoadingSlots(true);
    setError(null);
    try {
      const loadedSlots = await listOpenSlots(offerId);
      setSlots(loadedSlots);
      setSelectedSlotId((current) =>
        loadedSlots.some((slot) => slot.id === current) ? current : loadedSlots[0]?.id ?? null,
      );
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingSlots(false);
    }
  }

  async function submitReservation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const accessToken = requireAccessToken(customerToken);
    if (!accessToken) {
      setError({
        code: 'CUSTOMER_LOGIN_REQUIRED',
        message: 'Customer login is required',
        details: [],
      });
      return;
    }

    if (!selectedSlot) {
      return;
    }

    if (selectedSlotRemaining <= 0) {
      setError({
        code: 'NO_CAPACITY',
        message: 'Selected slot has no capacity left',
        details: [],
      });
      return;
    }

    const payload: CreateReservationPayload = {
      availabilitySlotId: selectedSlot.id,
      customerName: reservationForm.customerName,
      customerEmail: reservationForm.customerEmail,
      partySize: Number(reservationForm.partySize),
    };

    setIsSubmittingReservation(true);
    setError(null);
    try {
      const reservation = await createReservation(payload, accessToken);
      setLastReservation(reservation);
      setReservationForm(emptyReservationForm);
      if (selectedOfferId !== null) {
        await loadSlots(selectedOfferId);
      }
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsSubmittingReservation(false);
    }
  }

  async function cancelLastReservation() {
    if (!lastReservation) {
      return;
    }

    const accessToken = requireAccessToken(customerToken);
    if (!accessToken) {
      setError({
        code: 'CUSTOMER_LOGIN_REQUIRED',
        message: 'Customer login is required',
        details: [],
      });
      return;
    }

    setIsSubmittingReservation(true);
    setError(null);
    try {
      const reservation = await cancelReservation(lastReservation.id, accessToken);
      setLastReservation(reservation);
      if (selectedOfferId !== null) {
        await loadSlots(selectedOfferId);
      }
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsSubmittingReservation(false);
    }
  }

  async function loadAdminData(token = adminToken) {
    const accessToken = requireAccessToken(token);
    if (!accessToken) {
      setError({
        code: 'ADMIN_LOGIN_REQUIRED',
        message: 'Admin login is required',
        details: [],
      });
      return;
    }

    setIsLoadingAdmin(true);
    setError(null);
    try {
      const [reservations, allOffers] = await Promise.all([
        listAdminReservations(accessToken),
        listAdminOffers(accessToken),
      ]);
      setAdminReservations(reservations);
      setAdminOffers(allOffers);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingAdmin(false);
    }
  }

  async function loadAdminSlots(offerId: number, token = adminToken) {
    const accessToken = requireAccessToken(token);
    if (!accessToken) {
      setError({
        code: 'ADMIN_LOGIN_REQUIRED',
        message: 'Admin login is required',
        details: [],
      });
      return;
    }

    setIsLoadingAdminSlots(true);
    setError(null);
    try {
      const loadedSlots = await listAdminSlots(offerId, accessToken);
      setAdminSlots(loadedSlots);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingAdminSlots(false);
    }
  }

  async function submitAdminOffer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const accessToken = requireAccessToken(adminToken);
    if (!accessToken) {
      setError({
        code: 'ADMIN_LOGIN_REQUIRED',
        message: 'Admin login is required',
        details: [],
      });
      return;
    }

    const payload: CreateOfferPayload = {
      name: offerForm.name,
      imageUrl: offerForm.imageUrl,
      description: offerForm.description,
      price: Number(offerForm.price),
    };

    setIsCreatingOffer(true);
    setError(null);
    try {
      const offer = await createOffer(payload, accessToken);
      setOfferForm(emptyOfferForm);
      setAdminOffers((current) => [offer, ...current.filter((item) => item.id !== offer.id)]);
      setOffers((current) => [offer, ...current.filter((item) => item.id !== offer.id)]);
      setSelectedOfferId(offer.id);
      setSlotForm((current) => ({ ...current, offerId: String(offer.id) }));
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCreatingOffer(false);
    }
  }

  async function submitAdminSlot(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const accessToken = requireAccessToken(adminToken);
    if (!accessToken) {
      setError({
        code: 'ADMIN_LOGIN_REQUIRED',
        message: 'Admin login is required',
        details: [],
      });
      return;
    }

    const offerId = Number(slotForm.offerId);
    if (!Number.isFinite(offerId) || offerId <= 0) {
      setError({
        code: 'INVALID_OFFER',
        message: 'Select an offer for availability slot',
        details: [],
      });
      return;
    }

    const payload: CreateAvailabilitySlotPayload = {
      startsAt: slotForm.startsAt,
      endsAt: slotForm.endsAt,
      capacity: Number(slotForm.capacity),
    };

    setIsCreatingSlot(true);
    setError(null);
    try {
      const slot = await createAvailabilitySlot(offerId, payload, accessToken);
      setSlotForm(createDefaultSlotForm(offerId));
      setAdminSlots((current) => [slot, ...current.filter((item) => item.id !== slot.id)]);
      setSelectedAdminOfferId(offerId);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCreatingSlot(false);
    }
  }

  async function confirmAdminReservation(reservationId: number) {
    const accessToken = requireAccessToken(adminToken);
    if (!accessToken) {
      setError({
        code: 'ADMIN_LOGIN_REQUIRED',
        message: 'Admin login is required',
        details: [],
      });
      return;
    }

    setReservationActionId(reservationId);
    setError(null);
    try {
      const updated = await confirmReservation(reservationId, accessToken);
      setAdminReservations((current) =>
        current.map((reservation) => (reservation.id === updated.id ? updated : reservation)),
      );
      if (selectedOfferId !== null && updated.offerId === selectedOfferId) {
        await loadSlots(selectedOfferId);
      }
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setReservationActionId(null);
    }
  }

  async function rejectAdminReservation(reservationId: number) {
    const accessToken = requireAccessToken(adminToken);
    if (!accessToken) {
      setError({
        code: 'ADMIN_LOGIN_REQUIRED',
        message: 'Admin login is required',
        details: [],
      });
      return;
    }

    setReservationActionId(reservationId);
    setError(null);
    try {
      const updated = await rejectReservation(reservationId, accessToken);
      setAdminReservations((current) =>
        current.map((reservation) => (reservation.id === updated.id ? updated : reservation)),
      );
      if (selectedOfferId !== null && updated.offerId === selectedOfferId) {
        await loadSlots(selectedOfferId);
      }
      if (selectedAdminOfferId !== null && updated.offerId === selectedAdminOfferId) {
        await loadAdminSlots(updated.offerId, accessToken);
      }
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setReservationActionId(null);
    }
  }

  async function submitCustomerRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsCustomerAuthLoading(true);
    setError(null);
    try {
      await registerCustomer(customerAuthForm);
      const token = await login({ email: customerAuthForm.email, password: customerAuthForm.password });
      setCustomerToken(token.token);
      setReservationForm((current) => ({
        ...current,
        customerName: current.customerName || customerAuthForm.displayName,
        customerEmail: current.customerEmail || customerAuthForm.email,
      }));
      navigate('landing');
      setAuthTab('login');
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCustomerAuthLoading(false);
    }
  }

  async function submitCustomerLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsCustomerAuthLoading(true);
    setError(null);
    try {
      const token = await login(customerLoginForm);
      setCustomerToken(token.token);
      setReservationForm((current) => ({
        ...current,
        customerEmail: current.customerEmail || customerLoginForm.email,
      }));
      navigate('landing');
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCustomerAuthLoading(false);
    }
  }

  async function submitAdminLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsAdminAuthLoading(true);
    setError(null);
    try {
      const token = await login(adminLoginForm);
      setAdminToken(token.token);
      await loadAdminData(token.token);
      navigate('admin');
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsAdminAuthLoading(false);
    }
  }

  function handleLogout() {
    setCustomerToken(null);
    setLastReservation(null);
  }

  function handleAdminLogout() {
    setAdminToken(null);
    setAdminReservations([]);
    setAdminOffers([]);
    setAdminSlots([]);
  }

  return (
    <main className="app-shell">
      <header className="nav">
        <div className="logo">
          <span className="logo-mark">
            <ClipboardList size={18} aria-hidden="true" />
          </span>
          <div>
            <span className="logo-title">Reservation System</span>
            <span className="logo-subtitle">Simple booking for curated offers</span>
          </div>
        </div>
        <div className="nav-actions">
          <button className="ghost-button" type="button" onClick={() => navigate('landing')}>
            <DoorOpen size={16} aria-hidden="true" />
            Landing
          </button>
          <button className="ghost-button" type="button" onClick={() => navigate('auth')}>
            <LogIn size={16} aria-hidden="true" />
            Login / Register
          </button>
          <button className="ghost-button" type="button" onClick={() => navigate('admin')}>
            <LayoutDashboard size={16} aria-hidden="true" />
            Admin
          </button>
        </div>
      </header>

      {error && <ErrorBanner error={error} />}

      {screen === 'auth' && (
        <section className="auth-screen">
          <div className="auth-card">
            <div className="auth-header">
              <div>
                <p className="eyebrow">Access</p>
                <h1>Login or register</h1>
                <p className="helper">Authenticate to create and manage reservations.</p>
              </div>
              <span className="tag">Auth module: {apiConfig.authBaseUrl}</span>
            </div>

            <div className="auth-tabs">
              <button
                className={`tab-button ${authTab === 'login' ? 'active' : ''}`}
                type="button"
                onClick={() => setAuthTab('login')}
              >
                Login
              </button>
              <button
                className={`tab-button ${authTab === 'register' ? 'active' : ''}`}
                type="button"
                onClick={() => setAuthTab('register')}
              >
                Register
              </button>
            </div>

            <div className="auth-grid">
              {authTab === 'login' ? (
                <form className="auth-form" onSubmit={submitCustomerLogin}>
                  <label>
                    Email
                    <input
                      type="email"
                      value={customerLoginForm.email}
                      onChange={(event) =>
                        setCustomerLoginForm((current) => ({ ...current, email: event.target.value }))
                      }
                      required
                    />
                  </label>
                  <label>
                    Password
                    <input
                      type="password"
                      value={customerLoginForm.password}
                      onChange={(event) =>
                        setCustomerLoginForm((current) => ({ ...current, password: event.target.value }))
                      }
                      required
                    />
                  </label>
                  <button className="primary-button" type="submit" disabled={isCustomerAuthLoading}>
                    <LogIn size={16} aria-hidden="true" />
                    Login as customer
                  </button>
                </form>
              ) : (
                <form className="auth-form" onSubmit={submitCustomerRegister}>
                  <label>
                    Display name
                    <input
                      type="text"
                      value={customerAuthForm.displayName}
                      onChange={(event) =>
                        setCustomerAuthForm((current) => ({ ...current, displayName: event.target.value }))
                      }
                      minLength={2}
                      maxLength={100}
                      required
                    />
                  </label>
                  <label>
                    Email
                    <input
                      type="email"
                      value={customerAuthForm.email}
                      onChange={(event) =>
                        setCustomerAuthForm((current) => ({ ...current, email: event.target.value }))
                      }
                      required
                    />
                  </label>
                  <label>
                    Password
                    <input
                      type="password"
                      value={customerAuthForm.password}
                      onChange={(event) =>
                        setCustomerAuthForm((current) => ({ ...current, password: event.target.value }))
                      }
                      minLength={8}
                      maxLength={72}
                      required
                    />
                  </label>
                  <button className="primary-button" type="submit" disabled={isCustomerAuthLoading}>
                    <UserRound size={16} aria-hidden="true" />
                    Create account
                  </button>
                </form>
              )}

            </div>
          </div>
        </section>
      )}

      {screen === 'admin' && (
        <section className="admin-screen">
          <div className="admin-grid">
            <div className="panel admin-card">
              <div className="panel-header">
                <h2>
                  <ShieldCheck size={16} aria-hidden="true" />
                  Admin access
                </h2>
                {adminToken ? (
                  <span className="tag success">Admin token active</span>
                ) : (
                  <span className="tag warning">Admin login required</span>
                )}
              </div>
              <form className="auth-form" onSubmit={submitAdminLogin}>
                <label>
                  Admin email
                  <input
                    type="email"
                    value={adminLoginForm.email}
                    onChange={(event) => setAdminLoginForm((current) => ({ ...current, email: event.target.value }))}
                    required
                  />
                </label>
                <label>
                  Admin password
                  <input
                    type="password"
                    value={adminLoginForm.password}
                    onChange={(event) => setAdminLoginForm((current) => ({ ...current, password: event.target.value }))}
                    required
                  />
                </label>
                <div className="button-row">
                  <button className="primary-button" type="submit" disabled={isAdminAuthLoading}>
                    <ShieldCheck size={16} aria-hidden="true" />
                    Login
                  </button>
                  <button className="secondary-button" type="button" onClick={handleAdminLogout}>
                    <LogOut size={16} aria-hidden="true" />
                    Logout
                  </button>
                </div>
              </form>
            </div>

            {!adminToken ? (
              <div className="panel admin-card">
                <h2>Admin panel</h2>
                <p className="helper">Login as admin to access offers, slots, and reservation actions.</p>
              </div>
            ) : (
              <>
                <div className="panel admin-card">
                  <div className="panel-header">
                    <h2>
                      <ClipboardList size={16} aria-hidden="true" />
                      Offers
                    </h2>
                    <span className="tag">{adminOffers.length} total</span>
                  </div>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => loadAdminData()}
                    disabled={isLoadingAdmin}
                  >
                    Refresh admin data
                  </button>
                  <form className="admin-form" onSubmit={submitAdminOffer}>
                    <label>
                      Name
                      <input
                        type="text"
                        value={offerForm.name}
                        onChange={(event) => setOfferForm((current) => ({ ...current, name: event.target.value }))}
                        maxLength={255}
                        required
                      />
                    </label>
                    <label>
                      Image URL
                      <input
                        type="url"
                        value={offerForm.imageUrl}
                        onChange={(event) => setOfferForm((current) => ({ ...current, imageUrl: event.target.value }))}
                        maxLength={2048}
                        required
                      />
                    </label>
                    <label>
                      Description
                      <textarea
                        value={offerForm.description}
                        onChange={(event) =>
                          setOfferForm((current) => ({ ...current, description: event.target.value }))
                        }
                        maxLength={2048}
                        required
                      />
                    </label>
                    <label>
                      Price
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={offerForm.price}
                        onChange={(event) => setOfferForm((current) => ({ ...current, price: event.target.value }))}
                        required
                      />
                    </label>
                    <button className="primary-button" type="submit" disabled={isCreatingOffer}>
                      <PlusCircle size={16} aria-hidden="true" />
                      Create offer
                    </button>
                  </form>
                </div>

                <div className="panel admin-card">
                  <div className="panel-header">
                    <h2>
                      <CalendarDays size={16} aria-hidden="true" />
                      Availability slots
                    </h2>
                    <span className="tag">{adminSlots.length} slots</span>
                  </div>
                  <form className="admin-form" onSubmit={submitAdminSlot}>
                    <label>
                      Offer
                      <select
                        value={slotForm.offerId}
                        onChange={(event) => setSlotForm((current) => ({ ...current, offerId: event.target.value }))}
                        required
                      >
                        <option value="">Select offer</option>
                        {adminOfferOptions.map((offer) => (
                          <option value={offer.id} key={offer.id}>
                            {offer.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Starts
                      <input
                        type="datetime-local"
                        value={slotForm.startsAt}
                        onChange={(event) => setSlotForm((current) => ({ ...current, startsAt: event.target.value }))}
                        required
                      />
                    </label>
                    <label>
                      Ends
                      <input
                        type="datetime-local"
                        value={slotForm.endsAt}
                        onChange={(event) => setSlotForm((current) => ({ ...current, endsAt: event.target.value }))}
                        required
                      />
                    </label>
                    <label>
                      Capacity
                      <input
                        type="number"
                        min="1"
                        value={slotForm.capacity}
                        onChange={(event) => setSlotForm((current) => ({ ...current, capacity: event.target.value }))}
                        required
                      />
                    </label>
                    <button className="primary-button" type="submit" disabled={isCreatingSlot}>
                      <PlusCircle size={16} aria-hidden="true" />
                      Create slot
                    </button>
                  </form>
                  <div className="admin-actions">
                    <label>
                      Inspect offer slots
                      <select
                        value={selectedAdminOfferId ?? ''}
                        onChange={(event) => {
                          const nextId = Number(event.target.value);
                          setSelectedAdminOfferId(Number.isFinite(nextId) ? nextId : null);
                        }}
                      >
                        <option value="">Select offer</option>
                        {adminOfferOptions.map((offer) => (
                          <option value={offer.id} key={offer.id}>
                            {offer.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => selectedAdminOfferId && loadAdminSlots(selectedAdminOfferId)}
                      disabled={!selectedAdminOfferId || isLoadingAdminSlots}
                    >
                      Load slots
                    </button>
                  </div>
                  <div className="slots-list">
                    {adminSlots.length === 0 ? <EmptyState label="No slots loaded" /> : null}
                    {adminSlots.map((slot) => (
                      <article className="slot-card" key={slot.id}>
                        <div>
                          <strong>#{slot.id}</strong>
                          <span>{formatDateTime(slot.startsAt)}</span>
                        </div>
                        <span className={`status-pill ${slot.status.toLowerCase()}`}>{slot.status}</span>
                      </article>
                    ))}
                  </div>
                </div>

                <div className="panel admin-card">
                  <div className="panel-header">
                    <h2>
                      <TicketCheck size={16} aria-hidden="true" />
                      Reservations
                    </h2>
                    <span className="tag">{adminReservations.length} total</span>
                  </div>
                  <div className="slots-list">
                    {adminReservations.length === 0 ? <EmptyState label="No reservations loaded" /> : null}
                    {adminReservations.map((reservation) => (
                      <article className="slot-card reservation-row" key={reservation.id}>
                        <div>
                          <strong>#{reservation.id}</strong>
                          <span>{reservation.customerEmail}</span>
                        </div>
                        <div className="reservation-actions">
                          <span className={`status-pill ${reservation.status.toLowerCase()}`}>
                            {reservation.status}
                          </span>
                          {reservation.status === 'PENDING' ? (
                            <div className="button-row">
                              <button
                                className="primary-button"
                                type="button"
                                onClick={() => confirmAdminReservation(reservation.id)}
                                disabled={reservationActionId === reservation.id}
                              >
                                Confirm
                              </button>
                              <button
                                className="secondary-button"
                                type="button"
                                onClick={() => rejectAdminReservation(reservation.id)}
                                disabled={reservationActionId === reservation.id}
                              >
                                Reject
                              </button>
                            </div>
                          ) : null}
                        </div>
                      </article>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </section>
      )}

      {screen === 'landing' && (
        <>
          <section className="hero">
            <div className="hero-copy">
              <p className="eyebrow">Landing</p>
              <h1>Pick an offer and reserve your spot.</h1>
              <p className="helper">
                Browse curated offers, choose a time slot, and book in a few clicks. Login is required for
                reservations.
              </p>
              <div className="hero-actions">
                <button className="primary-button" type="button" onClick={() => navigate('auth')}>
                  <LogIn size={16} aria-hidden="true" />
                  Login / Register
                </button>
                <button className="secondary-button" type="button" onClick={loadOffers} disabled={isLoadingOffers}>
                  Refresh offers
                </button>
              </div>
            </div>
            <div className="hero-card">
              <div className="hero-badge">
                <Lock size={16} aria-hidden="true" />
                <span>{customerToken ? 'Customer session active' : 'Login required to reserve'}</span>
              </div>
              <div className="hero-badge">
                <ShieldCheck size={16} aria-hidden="true" />
                <span>Admin panel: /admin</span>
              </div>
              {customerToken ? (
                <button className="secondary-button" type="button" onClick={handleLogout}>
                  <LogOut size={16} aria-hidden="true" />
                  Logout
                </button>
              ) : null}
            </div>
          </section>

          <section className="landing-grid">
            <section className="panel">
              <div className="panel-header">
                <h2>
                  <ClipboardList size={16} aria-hidden="true" />
                  Offers
                </h2>
                <span className="tag">{offers.length} active</span>
              </div>
              <div className="offer-grid">
                {offers.length === 0 && !isLoadingOffers ? <EmptyState label="No active offers" /> : null}
                {offers.map((offer) => (
                  <button
                    className={`offer-card ${selectedOfferId === offer.id ? 'selected' : ''}`}
                    type="button"
                    key={offer.id}
                    onClick={() => setSelectedOfferId(offer.id)}
                  >
                    <img className="offer-image" src={offer.imageUrl} alt={offer.name} />
                    <div className="offer-body">
                      <div>
                        <strong>{offer.name}</strong>
                        <p>{offer.description}</p>
                      </div>
                      <span className="price-tag">{formatCurrency(offer.price)}</span>
                    </div>
                  </button>
                ))}
              </div>
            </section>

            <section className="panel">
              <div className="panel-header">
                <h2>
                  <CalendarDays size={16} aria-hidden="true" />
                  Availability
                </h2>
                <span className="tag">{slots.length} open</span>
              </div>
              <div className="slots-list">
                {slots.length === 0 && !isLoadingSlots ? <EmptyState label="No open slots" /> : null}
                {slots.map((slot) => (
                  <button
                    className={`slot-card ${selectedSlotId === slot.id ? 'selected' : ''}`}
                    type="button"
                    key={slot.id}
                    onClick={() => setSelectedSlotId(slot.id)}
                  >
                    <div>
                      <strong>{formatDateTime(slot.startsAt)}</strong>
                      <span>
                        {slot.capacity - slot.reservedCount}/{slot.capacity} seats
                      </span>
                    </div>
                    <span className={`status-pill ${slot.status.toLowerCase()}`}>{slot.status}</span>
                  </button>
                ))}
              </div>
            </section>

            <section className="panel reservation-panel">
              <div className="panel-header">
                <h2>
                  <TicketCheck size={16} aria-hidden="true" />
                  Reservation
                </h2>
                <span className="tag">{selectedSlot ? `Slot #${selectedSlot.id}` : 'Select slot'}</span>
              </div>
              <form className="reservation-form" onSubmit={submitReservation}>
                <label>
                  Name
                  <input
                    type="text"
                    value={reservationForm.customerName}
                    onChange={(event) =>
                      setReservationForm((current) => ({ ...current, customerName: event.target.value }))
                    }
                    autoComplete="name"
                    maxLength={255}
                    required
                  />
                </label>
                <label>
                  Email
                  <input
                    type="email"
                    value={reservationForm.customerEmail}
                    onChange={(event) =>
                      setReservationForm((current) => ({ ...current, customerEmail: event.target.value }))
                    }
                    autoComplete="email"
                    maxLength={255}
                    required
                  />
                </label>
                <label>
                  Party size
                  <input
                    type="number"
                    min="1"
                    value={reservationForm.partySize}
                    onChange={(event) =>
                      setReservationForm((current) => ({ ...current, partySize: event.target.value }))
                    }
                    max={selectedSlot ? selectedSlot.capacity - selectedSlot.reservedCount : undefined}
                    required
                  />
                </label>
                <button
                  className="primary-button"
                  type="submit"
                  disabled={
                    isSubmittingReservation ||
                    !customerToken ||
                    !selectedSlot ||
                    selectedSlotRemaining <= 0
                  }
                >
                  <TicketCheck size={16} aria-hidden="true" />
                  Reserve
                </button>
                {!customerToken && (
                  <p className="helper">
                    Please login first. Go to{' '}
                    <button className="link-button" type="button" onClick={() => navigate('auth')}>
                      Login / Register
                    </button>
                    .
                  </p>
                )}
              </form>

              {lastReservation && (
                <div className="reservation-result">
                  <div className="result-heading">
                    <CheckCircle2 size={18} aria-hidden="true" />
                    <strong>Reservation #{lastReservation.id}</strong>
                  </div>
                  <div className="result-row">
                    <span>Status</span>
                    <span>{lastReservation.status}</span>
                  </div>
                  <div className="result-row">
                    <span>Email</span>
                    <span>{lastReservation.customerEmail}</span>
                  </div>
                  <div className="result-row">
                    <span>Party size</span>
                    <span>{lastReservation.partySize}</span>
                  </div>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={cancelLastReservation}
                    disabled={
                      isSubmittingReservation ||
                      (lastReservation.status !== 'PENDING' && lastReservation.status !== 'CONFIRMED')
                    }
                  >
                    <XCircle size={18} aria-hidden="true" />
                    Cancel
                  </button>
                </div>
              )}
            </section>
          </section>
        </>
      )}
    </main>
  );
}

function EmptyState(props: { label: string }) {
  return (
    <div className="empty-state">
      <UserRound size={18} aria-hidden="true" />
      <span>{props.label}</span>
    </div>
  );
}

function ErrorBanner({ error }: { error: ApiErrorResponse }) {
  return (
    <div className="error-banner">
      <span className="error-icon">
        <XCircle size={18} aria-hidden="true" />
      </span>
      <div>
        <strong>{error.code}</strong>
        <span>{error.message}</span>
        {error.details && error.details.length > 0 ? (
          <ul>
            {error.details.map((detail) => (
              <li key={`${detail.field}-${detail.message}`}>
                {detail.field}: {detail.message}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </div>
  );
}

function createDefaultSlotForm(offerId?: number): SlotFormState {
  const now = new Date();
  const startsAt = new Date(now.getTime() + 30 * 60 * 1000);
  const endsAt = new Date(now.getTime() + 2 * 60 * 60 * 1000);

  return {
    offerId: offerId ? String(offerId) : '',
    startsAt: toLocalInput(startsAt),
    endsAt: toLocalInput(endsAt),
    capacity: '10',
  };
}

function toApiError(caught: unknown): ApiErrorResponse {
  if (caught instanceof ApiRequestError) {
    return caught.response;
  }

  if (caught instanceof Error) {
    return {
      code: 'CLIENT_ERROR',
      message: caught.message,
      details: [],
    };
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: 'Something went wrong',
    details: [],
  };
}

function requireAccessToken(accessToken: string | null): string | null {
  if (accessToken) {
    return accessToken;
  }

  return null;
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return date.toLocaleString('pl-PL', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function toLocalInput(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`;
}

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency: 'PLN',
  }).format(amount);
}
