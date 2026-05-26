import {
  AlertCircle,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Clock3,
  PlusCircle,
  RefreshCw,
  ShieldCheck,
  TicketCheck,
  UserRound,
  XCircle,
} from 'lucide-react';
import { type FormEvent, type ReactNode, useEffect, useMemo, useState } from 'react';
import {
  cancelAvailabilitySlot,
  createAvailabilitySlot,
  listAdminSlots,
  listOpenSlots,
} from './api/availability';
import { login, registerCustomer } from './api/auth';
import { ApiRequestError } from './api/client';
import { createOffer, listActiveOffers, listAdminOffers } from './api/offers';
import { cancelReservation, createReservation, listAdminReservations } from './api/reservations';
import { apiConfig } from './config';
import type {
  ApiErrorResponse,
  AvailabilitySlot,
  CreateAvailabilitySlotPayload,
  CreateOfferPayload,
  CreateReservationPayload,
  LoginCredentials,
  Offer,
  Reservation,
} from './types';

type CustomerAuthFormState = LoginCredentials & {
  displayName: string;
};

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

export function App() {
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
  const [customerAuthForm, setCustomerAuthForm] = useState<CustomerAuthFormState>({
    displayName: 'Jan Kowalski',
    email: apiConfig.customerEmail,
    password: apiConfig.customerPassword,
  });
  const [customerToken, setCustomerToken] = useState<string | null>(null);
  const [adminCredentials, setAdminCredentials] = useState<LoginCredentials>({
    email: apiConfig.adminEmail,
    password: apiConfig.adminPassword,
  });
  const [adminToken, setAdminToken] = useState<string | null>(null);
  const [isLoadingOffers, setIsLoadingOffers] = useState(false);
  const [isLoadingSlots, setIsLoadingSlots] = useState(false);
  const [isCustomerAuthLoading, setIsCustomerAuthLoading] = useState(false);
  const [isSubmittingReservation, setIsSubmittingReservation] = useState(false);
  const [isAdminAuthLoading, setIsAdminAuthLoading] = useState(false);
  const [isLoadingAdmin, setIsLoadingAdmin] = useState(false);
  const [isLoadingAdminSlots, setIsLoadingAdminSlots] = useState(false);
  const [isCreatingOffer, setIsCreatingOffer] = useState(false);
  const [isCreatingSlot, setIsCreatingSlot] = useState(false);
  const [cancellingSlotId, setCancellingSlotId] = useState<number | null>(null);
  const [error, setError] = useState<ApiErrorResponse | null>(null);

  const selectedOffer = useMemo(
    () => offers.find((offer) => offer.id === selectedOfferId) ?? null,
    [offers, selectedOfferId],
  );

  const selectedSlot = useMemo(
    () => slots.find((slot) => slot.id === selectedSlotId) ?? null,
    [slots, selectedSlotId],
  );

  const adminOfferOptions = useMemo(
    () => (adminOffers.length > 0 ? adminOffers : offers),
    [adminOffers, offers],
  );

  const selectedSlotRemaining = selectedSlot ? selectedSlot.capacity - selectedSlot.reservedCount : 0;

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

    setSlotForm((current) => ({ ...current, offerId: String(selectedOfferId) }));
  }, [selectedOfferId]);

  async function loadOffers() {
    setIsLoadingOffers(true);
    setError(null);
    try {
      const loadedOffers = await listActiveOffers();
      setOffers(loadedOffers);
      setSelectedOfferId((current) =>
        loadedOffers.some((offer) => offer.id === current) ? current : loadedOffers[0]?.id ?? null,
      );
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
      setSelectedSlotId(loadedSlots[0]?.id ?? null);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingSlots(false);
    }
  }

  async function registerCustomerAccount() {
    setIsCustomerAuthLoading(true);
    setError(null);
    try {
      await registerCustomer(customerAuthForm);
      await createCustomerSession();
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCustomerAuthLoading(false);
    }
  }

  async function loginCustomerAccount() {
    setIsCustomerAuthLoading(true);
    setError(null);
    try {
      await createCustomerSession();
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCustomerAuthLoading(false);
    }
  }

  async function createCustomerSession() {
    const token = await login({
      email: customerAuthForm.email,
      password: customerAuthForm.password,
    });
    setCustomerToken(token.token);
    setReservationForm((current) => ({
      ...current,
      customerName: current.customerName || customerAuthForm.displayName,
      customerEmail: current.customerEmail || customerAuthForm.email,
    }));
  }

  async function loginAdminAndLoad() {
    setIsAdminAuthLoading(true);
    setError(null);
    try {
      const token = await login(adminCredentials);
      setAdminToken(token.token);
      await loadAdminData(token.token);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsAdminAuthLoading(false);
    }
  }

  async function submitReservation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedSlot) {
      setError({
        code: 'SLOT_REQUIRED',
        message: 'Availability slot is required',
        details: [],
      });
      return;
    }

    const accessToken = requireAccessToken(
      customerToken,
      'CUSTOMER_LOGIN_REQUIRED',
      'Customer login is required',
    );
    if (!accessToken) {
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

    const accessToken = requireAccessToken(
      customerToken,
      'CUSTOMER_LOGIN_REQUIRED',
      'Customer login is required',
    );
    if (!accessToken) {
      return;
    }

    setError(null);
    try {
      const reservation = await cancelReservation(lastReservation.id, accessToken);
      setLastReservation(reservation);
      if (selectedOfferId !== null) {
        await loadSlots(selectedOfferId);
      }
    } catch (caught) {
      setError(toApiError(caught));
    }
  }

  async function loadAdminData(token = adminToken) {
    const accessToken = requireAccessToken(token, 'ADMIN_LOGIN_REQUIRED', 'Admin login is required');
    if (!accessToken) {
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
      setSelectedAdminOfferId((current) =>
        allOffers.some((offer) => offer.id === current) ? current : allOffers[0]?.id ?? null,
      );
      setAdminSlots([]);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingAdmin(false);
    }
  }

  async function loadAdminSlots(offerId: number, token = adminToken) {
    const accessToken = requireAccessToken(token, 'ADMIN_LOGIN_REQUIRED', 'Admin login is required');
    if (!accessToken) {
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

  async function cancelAdminSlot(slot: AvailabilitySlot) {
    const accessToken = requireAccessToken(adminToken, 'ADMIN_LOGIN_REQUIRED', 'Admin login is required');
    if (!accessToken) {
      return;
    }

    setCancellingSlotId(slot.id);
    setError(null);
    try {
      const cancelledSlot = await cancelAvailabilitySlot(slot.id, accessToken);
      setAdminSlots((current) => current.map((item) => (item.id === cancelledSlot.id ? cancelledSlot : item)));
      if (selectedOfferId === slot.offerId) {
        await loadSlots(slot.offerId);
      }
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setCancellingSlotId(null);
    }
  }

  async function submitAdminOffer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const accessToken = requireAccessToken(adminToken, 'ADMIN_LOGIN_REQUIRED', 'Admin login is required');
    if (!accessToken) {
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
      setSelectedAdminOfferId(offer.id);
      setAdminSlots([]);
      setSlotForm((current) => ({ ...current, offerId: String(offer.id) }));
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsCreatingOffer(false);
    }
  }

  async function submitAdminSlot(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const accessToken = requireAccessToken(adminToken, 'ADMIN_LOGIN_REQUIRED', 'Admin login is required');
    if (!accessToken) {
      return;
    }

    const offerId = Number(slotForm.offerId);
    if (!Number.isFinite(offerId) || offerId <= 0) {
      setError({
        code: 'OFFER_REQUIRED',
        message: 'Offer is required',
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
      setSelectedOfferId(offerId);
      setSlotForm({ ...createDefaultSlotForm(), offerId: String(offerId) });
      await loadSlots(offerId);
      if (selectedAdminOfferId === offerId) {
        await loadAdminSlots(offerId, accessToken);
      }
      setSelectedSlotId(slot.id);
    } catch (caught) {
      setError(toCreateSlotError(caught));
    } finally {
      setIsCreatingSlot(false);
    }
  }

  function requireAccessToken(accessToken: string | null, code: string, message: string): string | null {
    if (accessToken) {
      return accessToken;
    }

    setError({
      code,
      message,
      details: [],
    });
    return null;
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Reservation System</p>
          <h1>Operations Console</h1>
        </div>
        <button className="icon-button" type="button" onClick={loadOffers} disabled={isLoadingOffers}>
          <RefreshCw size={18} aria-hidden="true" />
          Refresh
        </button>
      </header>

      {error && <ErrorBanner error={error} />}

      <section className="workspace" aria-label="Reservation workspace">
        <section className="panel offers-panel" aria-labelledby="offers-heading">
          <PanelHeader
            icon={<ClipboardList size={18} aria-hidden="true" />}
            title="Offers"
            meta={isLoadingOffers ? 'Loading' : `${offers.length}`}
          />
          <div className="offer-list">
            {offers.length === 0 && !isLoadingOffers ? <EmptyState label="No active offers" /> : null}
            {offers.map((offer) => (
              <button
                className={`offer-card ${selectedOfferId === offer.id ? 'selected' : ''}`}
                type="button"
                key={offer.id}
                onClick={() => setSelectedOfferId(offer.id)}
              >
                <img src={offer.imageUrl} alt="" />
                <span className="offer-copy">
                  <strong>{offer.name}</strong>
                  <small>{formatCurrency(offer.price)}</small>
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="panel slots-panel" aria-labelledby="slots-heading">
          <PanelHeader
            icon={<CalendarDays size={18} aria-hidden="true" />}
            title={selectedOffer?.name ?? 'Availability'}
            meta={isLoadingSlots ? 'Loading' : `${slots.length}`}
          />
          <div className="slot-list">
            {slots.length === 0 && !isLoadingSlots ? <EmptyState label="No open slots" /> : null}
            {slots.map((slot) => (
              <button
                className={`slot-card ${selectedSlotId === slot.id ? 'selected' : ''}`}
                type="button"
                key={slot.id}
                onClick={() => setSelectedSlotId(slot.id)}
              >
                <span className="slot-main">
                  <Clock3 size={16} aria-hidden="true" />
                  <span>{formatDateTime(slot.startsAt)}</span>
                </span>
                <span className="slot-meta">
                  {slot.capacity - slot.reservedCount}/{slot.capacity}
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="panel booking-panel" aria-labelledby="booking-heading">
          <PanelHeader
            icon={<TicketCheck size={18} aria-hidden="true" />}
            title="Booking"
            meta={selectedSlot ? `Slot ${selectedSlot.id}` : 'No slot'}
          />
          <div className="auth-box">
            <div className="auth-status">
              <UserRound size={16} aria-hidden="true" />
              <span>{customerToken ? 'Customer token active' : 'Customer login required'}</span>
            </div>
            <label>
              Display name
              <input
                type="text"
                value={customerAuthForm.displayName}
                onChange={(event) =>
                  setCustomerAuthForm((current) => ({ ...current, displayName: event.target.value }))
                }
                autoComplete="name"
                maxLength={100}
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
                autoComplete="email"
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
                autoComplete="current-password"
              />
            </label>
            <div className="button-row">
              <button
                className="secondary-button"
                type="button"
                onClick={() => void registerCustomerAccount()}
                disabled={isCustomerAuthLoading}
              >
                <UserRound size={18} aria-hidden="true" />
                Register
              </button>
              <button
                className="secondary-button"
                type="button"
                onClick={() => void loginCustomerAccount()}
                disabled={isCustomerAuthLoading}
              >
                <TicketCheck size={18} aria-hidden="true" />
                Login
              </button>
            </div>
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
              disabled={isSubmittingReservation || !customerToken || !selectedSlot || selectedSlotRemaining <= 0}
            >
              <TicketCheck size={18} aria-hidden="true" />
              Reserve
            </button>
          </form>

          {lastReservation && (
            <div className="reservation-result">
              <div className="result-heading">
                <CheckCircle2 size={18} aria-hidden="true" />
                <strong>Reservation #{lastReservation.id}</strong>
              </div>
              <dl>
                <div>
                  <dt>Status</dt>
                  <dd>{lastReservation.status}</dd>
                </div>
                <div>
                  <dt>Email</dt>
                  <dd>{lastReservation.customerEmail}</dd>
                </div>
                <div>
                  <dt>People</dt>
                  <dd>{lastReservation.partySize}</dd>
                </div>
              </dl>
              <button
                className="secondary-button danger"
                type="button"
                onClick={cancelLastReservation}
                disabled={!customerToken || lastReservation.status === 'CANCELLED'}
              >
                <XCircle size={18} aria-hidden="true" />
                Cancel
              </button>
            </div>
          )}
        </section>

        <section className="panel admin-panel" aria-labelledby="admin-heading">
          <PanelHeader
            icon={<ShieldCheck size={18} aria-hidden="true" />}
            title="Admin"
            meta={`${adminReservations.length} reservations`}
          />
          <div className="admin-auth">
            <div className="auth-status">
              <ShieldCheck size={16} aria-hidden="true" />
              <span>{adminToken ? 'Admin token active' : 'Admin login required'}</span>
            </div>
            <label>
              Email
              <input
                type="email"
                value={adminCredentials.email}
                onChange={(event) =>
                  setAdminCredentials((current) => ({ ...current, email: event.target.value }))
                }
                autoComplete="username"
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={adminCredentials.password}
                onChange={(event) =>
                  setAdminCredentials((current) => ({ ...current, password: event.target.value }))
                }
                autoComplete="current-password"
              />
            </label>
            <button
              className="secondary-button"
              type="button"
              onClick={() => void loginAdminAndLoad()}
              disabled={isAdminAuthLoading || isLoadingAdmin}
            >
              <ShieldCheck size={18} aria-hidden="true" />
              Login & load
            </button>
          </div>

          <div className="admin-actions">
            <form className="admin-section" onSubmit={submitAdminOffer}>
              <h3>
                <PlusCircle size={16} aria-hidden="true" />
                Create offer
              </h3>
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
              <button className="primary-button" type="submit" disabled={isCreatingOffer || !adminToken}>
                <PlusCircle size={18} aria-hidden="true" />
                Create
              </button>
            </form>

            <form className="admin-section" onSubmit={submitAdminSlot}>
              <h3>
                <CalendarDays size={16} aria-hidden="true" />
                Create slot
              </h3>
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
              <button className="primary-button" type="submit" disabled={isCreatingSlot || !adminToken}>
                <PlusCircle size={18} aria-hidden="true" />
                Create
              </button>
            </form>
          </div>

          <section className="admin-section" aria-labelledby="admin-slots-heading">
            <h3 id="admin-slots-heading">
              <CalendarDays size={16} aria-hidden="true" />
              Slots
            </h3>
            <div className="admin-slot-toolbar">
              <label>
                Offer
                <select
                  value={selectedAdminOfferId ?? ''}
                  onChange={(event) => {
                    const value = event.target.value;
                    setSelectedAdminOfferId(value ? Number(value) : null);
                    setAdminSlots([]);
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
                disabled={isLoadingAdminSlots || !adminToken || selectedAdminOfferId === null}
                onClick={() => {
                  if (selectedAdminOfferId !== null) {
                    void loadAdminSlots(selectedAdminOfferId);
                  }
                }}
              >
                <RefreshCw size={18} aria-hidden="true" />
                Load slots
              </button>
            </div>
            <div className="reservation-list">
              {adminSlots.length === 0 && !isLoadingAdminSlots ? <EmptyState label="No slots loaded" /> : null}
              {adminSlots.map((slot) => (
                <article className="reservation-row admin-slot-row" key={slot.id}>
                  <div>
                    <strong>{formatDateTime(slot.startsAt)}</strong>
                    <span>
                      {formatDateTime(slot.endsAt)} · {slot.reservedCount}/{slot.capacity}
                    </span>
                  </div>
                  <span className={`status-pill ${slot.status.toLowerCase()}`}>{slot.status}</span>
                  <button
                    className="secondary-button danger compact-button"
                    type="button"
                    disabled={!adminToken || slot.status !== 'OPEN' || cancellingSlotId === slot.id}
                    onClick={() => void cancelAdminSlot(slot)}
                  >
                    <XCircle size={16} aria-hidden="true" />
                    Cancel
                  </button>
                </article>
              ))}
            </div>
          </section>

          <div className="admin-stats">
            <Metric label="Offers" value={adminOffers.length} />
            <Metric label="Confirmed" value={adminReservations.filter((item) => item.status === 'CONFIRMED').length} />
            <Metric label="Cancelled" value={adminReservations.filter((item) => item.status === 'CANCELLED').length} />
          </div>

          <div className="reservation-list">
            {adminReservations.length === 0 ? <EmptyState label="No reservations loaded" /> : null}
            {adminReservations.map((reservation) => (
              <article className="reservation-row" key={reservation.id}>
                <div>
                  <strong>#{reservation.id}</strong>
                  <span>{reservation.customerEmail}</span>
                </div>
                <span className={`status-pill ${reservation.status.toLowerCase()}`}>{reservation.status}</span>
              </article>
            ))}
          </div>
        </section>
      </section>
    </main>
  );
}

function PanelHeader(props: { icon: ReactNode; title: string; meta: string }) {
  return (
    <div className="panel-header">
      <h2>
        {props.icon}
        {props.title}
      </h2>
      <span>{props.meta}</span>
    </div>
  );
}

function Metric(props: { label: string; value: number }) {
  return (
    <div className="metric">
      <strong>{props.value}</strong>
      <span>{props.label}</span>
    </div>
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

function ErrorBanner(props: { error: ApiErrorResponse }) {
  return (
    <section className="error-banner" aria-live="polite">
      <AlertCircle size={20} aria-hidden="true" />
      <div>
        <strong>{props.error.code}</strong>
        <span>{props.error.message}</span>
        {props.error.details && props.error.details.length > 0 ? (
          <ul>
            {props.error.details.map((detail) => (
              <li key={`${detail.field}-${detail.message}`}>
                {detail.field}: {detail.message}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  );
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
    code: 'CLIENT_ERROR',
    message: 'Unexpected client error',
    details: [],
  };
}

function toCreateSlotError(caught: unknown): ApiErrorResponse {
  const apiError = toApiError(caught);

  if (apiError.code === 'AVAILABILITY_SLOT_ALREADY_EXISTS') {
    return {
      ...apiError,
      message: 'Slot for this offer and time already exists.',
      details: [],
    };
  }

  return apiError;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency: 'PLN',
  }).format(value);
}

function createDefaultSlotForm(): SlotFormState {
  const startsAt = new Date();
  startsAt.setDate(startsAt.getDate() + 1);
  startsAt.setMinutes(0, 0, 0);

  const endsAt = new Date(startsAt);
  endsAt.setHours(endsAt.getHours() + 1);

  return {
    offerId: '',
    startsAt: toDateTimeLocalValue(startsAt),
    endsAt: toDateTimeLocalValue(endsAt),
    capacity: '10',
  };
}

function toDateTimeLocalValue(value: Date): string {
  const localValue = new Date(value.getTime() - value.getTimezoneOffset() * 60_000);
  return localValue.toISOString().slice(0, 16);
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('pl-PL', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}
