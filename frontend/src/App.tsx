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
import { createAvailabilitySlot, listOpenSlots } from './api/availability';
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
  Credentials,
  Offer,
  Reservation,
} from './types';

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
  const [adminCredentials, setAdminCredentials] = useState<Credentials>({
    username: 'admin',
    password: 'admin123',
  });
  const [isLoadingOffers, setIsLoadingOffers] = useState(false);
  const [isLoadingSlots, setIsLoadingSlots] = useState(false);
  const [isSubmittingReservation, setIsSubmittingReservation] = useState(false);
  const [isLoadingAdmin, setIsLoadingAdmin] = useState(false);
  const [isCreatingOffer, setIsCreatingOffer] = useState(false);
  const [isCreatingSlot, setIsCreatingSlot] = useState(false);
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

    const payload: CreateReservationPayload = {
      availabilitySlotId: selectedSlot.id,
      customerName: reservationForm.customerName,
      customerEmail: reservationForm.customerEmail,
      partySize: Number(reservationForm.partySize),
    };

    setIsSubmittingReservation(true);
    setError(null);
    try {
      const reservation = await createReservation(payload, {
        username: apiConfig.customerUsername,
        password: apiConfig.customerPassword,
      });
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

    setError(null);
    try {
      const reservation = await cancelReservation(lastReservation.id, {
        username: apiConfig.customerUsername,
        password: apiConfig.customerPassword,
      });
      setLastReservation(reservation);
      if (selectedOfferId !== null) {
        await loadSlots(selectedOfferId);
      }
    } catch (caught) {
      setError(toApiError(caught));
    }
  }

  async function loadAdminData() {
    setIsLoadingAdmin(true);
    setError(null);
    try {
      const [reservations, allOffers] = await Promise.all([
        listAdminReservations(adminCredentials),
        listAdminOffers(adminCredentials),
      ]);
      setAdminReservations(reservations);
      setAdminOffers(allOffers);
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setIsLoadingAdmin(false);
    }
  }

  async function submitAdminOffer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const payload: CreateOfferPayload = {
      name: offerForm.name,
      imageUrl: offerForm.imageUrl,
      description: offerForm.description,
      price: Number(offerForm.price),
    };

    setIsCreatingOffer(true);
    setError(null);
    try {
      const offer = await createOffer(payload, adminCredentials);
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
      const slot = await createAvailabilitySlot(offerId, payload, adminCredentials);
      setSelectedOfferId(offerId);
      setSlotForm({ ...createDefaultSlotForm(), offerId: String(offerId) });
      await loadSlots(offerId);
      setSelectedSlotId(slot.id);
    } catch (caught) {
      setError(toCreateSlotError(caught));
    } finally {
      setIsCreatingSlot(false);
    }
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
              disabled={isSubmittingReservation || !selectedSlot || selectedSlotRemaining <= 0}
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
                disabled={lastReservation.status === 'CANCELLED'}
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
            <label>
              User
              <input
                type="text"
                value={adminCredentials.username}
                onChange={(event) =>
                  setAdminCredentials((current) => ({ ...current, username: event.target.value }))
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
            <button className="secondary-button" type="button" onClick={loadAdminData} disabled={isLoadingAdmin}>
              <ShieldCheck size={18} aria-hidden="true" />
              Load
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
              <button className="primary-button" type="submit" disabled={isCreatingOffer}>
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
              <button className="primary-button" type="submit" disabled={isCreatingSlot}>
                <PlusCircle size={18} aria-hidden="true" />
                Create
              </button>
            </form>
          </div>

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
