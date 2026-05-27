import type {
  AuthTokenResponse,
  AvailabilitySlot,
  CreateAvailabilitySlotPayload,
  CreateOfferPayload,
  CreateReservationPayload,
  LoginCredentials,
  Offer,
  RegisterCustomerPayload,
  Reservation,
  UserAccountResponse,
} from '../types';

const now = new Date();

let offers: Offer[] = [
  {
    id: 1,
    name: 'Weekend SPA package',
    imageUrl: createImageDataUri('#176b87', '#f4b860', 'SPA'),
    description: 'Two-hour relaxation package with limited daily availability.',
    price: 249,
    archived: false,
  },
  {
    id: 2,
    name: 'Fine dining table',
    imageUrl: createImageDataUri('#2f7d50', '#f7d488', 'DINING'),
    description: 'Evening restaurant reservation for small groups.',
    price: 159,
    archived: false,
  },
  {
    id: 3,
    name: 'City workshop',
    imageUrl: createImageDataUri('#6d5dfc', '#bde0fe', 'CLASS'),
    description: 'Hands-on workshop with confirmed seat capacity.',
    price: 99,
    archived: false,
  },
];

let slots: AvailabilitySlot[] = [
  createSlot(1, 1, 24, 26, 12, 4),
  createSlot(2, 1, 48, 50, 8, 2),
  createSlot(3, 2, 30, 32, 6, 1),
  createSlot(4, 3, 72, 75, 10, 3),
];

let reservations: Reservation[] = [
  {
    id: 1,
    availabilitySlotId: 1,
    offerId: 1,
    customerName: 'Jan Kowalski',
    customerEmail: 'jan@example.com',
    partySize: 2,
    status: 'PENDING',
    createdAt: shiftHours(-2).toISOString(),
    cancelledAt: null,
  },
  {
    id: 2,
    availabilitySlotId: 3,
    offerId: 2,
    customerName: 'Anna Nowak',
    customerEmail: 'anna@example.com',
    partySize: 4,
    status: 'CONFIRMED',
    createdAt: shiftHours(-8).toISOString(),
    cancelledAt: null,
  },
];

let nextOfferId = 4;
let nextSlotId = 5;
let nextReservationId = 3;

export async function mockRegisterCustomer(payload: RegisterCustomerPayload): Promise<UserAccountResponse> {
  return {
    id: 10,
    email: payload.email.toLowerCase(),
    displayName: payload.displayName,
    role: 'CUSTOMER',
    status: 'ACTIVE',
  };
}

export async function mockLogin(credentials: LoginCredentials): Promise<AuthTokenResponse> {
  const isAdmin = credentials.email.toLowerCase().includes('admin');

  return {
    token: isAdmin ? 'mock-admin-token' : 'mock-customer-token',
    tokenType: 'Bearer',
    expiresInSeconds: 7200,
  };
}

export async function mockListActiveOffers(): Promise<Offer[]> {
  return offers.filter((offer) => !offer.archived).map(copyOffer);
}

export async function mockListAdminOffers(): Promise<Offer[]> {
  return offers.map(copyOffer);
}

export async function mockCreateOffer(payload: CreateOfferPayload): Promise<Offer> {
  const offer: Offer = {
    id: nextOfferId++,
    name: payload.name,
    imageUrl: payload.imageUrl,
    description: payload.description,
    price: payload.price,
    archived: false,
  };
  offers = [offer, ...offers];
  return copyOffer(offer);
}

export async function mockListOpenSlots(offerId: number): Promise<AvailabilitySlot[]> {
  return slots
    .filter((slot) => slot.offerId === offerId && slot.status === 'OPEN' && slot.reservedCount < slot.capacity)
    .map(copySlot);
}

export async function mockListAdminSlots(offerId: number): Promise<AvailabilitySlot[]> {
  return slots.filter((slot) => slot.offerId === offerId).map(copySlot);
}

export async function mockCreateAvailabilitySlot(
  offerId: number,
  payload: CreateAvailabilitySlotPayload,
): Promise<AvailabilitySlot> {
  const slot: AvailabilitySlot = {
    id: nextSlotId++,
    offerId,
    startsAt: payload.startsAt,
    endsAt: payload.endsAt,
    capacity: payload.capacity,
    reservedCount: 0,
    status: 'OPEN',
  };
  slots = [slot, ...slots];
  return copySlot(slot);
}

export async function mockCreateReservation(payload: CreateReservationPayload): Promise<Reservation> {
  const slot = slots.find((item) => item.id === payload.availabilitySlotId);
  if (!slot) {
    throw new Error('Selected slot does not exist in mock data');
  }

  slot.reservedCount += payload.partySize;
  const reservation: Reservation = {
    id: nextReservationId++,
    availabilitySlotId: payload.availabilitySlotId,
    offerId: slot.offerId,
    customerName: payload.customerName,
    customerEmail: payload.customerEmail,
    partySize: payload.partySize,
    status: 'PENDING',
    createdAt: new Date().toISOString(),
    cancelledAt: null,
  };
  reservations = [reservation, ...reservations];
  return copyReservation(reservation);
}

export async function mockCancelReservation(reservationId: number): Promise<Reservation> {
  return updateReservationStatus(reservationId, 'CANCELLED', true);
}

export async function mockListAdminReservations(): Promise<Reservation[]> {
  return reservations.map(copyReservation);
}

export async function mockConfirmReservation(reservationId: number): Promise<Reservation> {
  return updateReservationStatus(reservationId, 'CONFIRMED', false);
}

export async function mockRejectReservation(reservationId: number): Promise<Reservation> {
  return updateReservationStatus(reservationId, 'REJECTED', true);
}

function updateReservationStatus(
  reservationId: number,
  status: Reservation['status'],
  releaseCapacity: boolean,
): Reservation {
  const reservation = reservations.find((item) => item.id === reservationId);
  if (!reservation) {
    throw new Error('Reservation does not exist in mock data');
  }

  if (releaseCapacity && (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED')) {
    const slot = slots.find((item) => item.id === reservation.availabilitySlotId);
    if (slot) {
      slot.reservedCount = Math.max(0, slot.reservedCount - reservation.partySize);
    }
  }

  reservation.status = status;
  reservation.cancelledAt = status === 'CANCELLED' ? new Date().toISOString() : reservation.cancelledAt;
  return copyReservation(reservation);
}

function createSlot(
  id: number,
  offerId: number,
  startHourOffset: number,
  endHourOffset: number,
  capacity: number,
  reservedCount: number,
): AvailabilitySlot {
  return {
    id,
    offerId,
    startsAt: shiftHours(startHourOffset).toISOString(),
    endsAt: shiftHours(endHourOffset).toISOString(),
    capacity,
    reservedCount,
    status: 'OPEN',
  };
}

function shiftHours(hours: number) {
  return new Date(now.getTime() + hours * 60 * 60 * 1000);
}

function copyOffer(offer: Offer): Offer {
  return { ...offer };
}

function copySlot(slot: AvailabilitySlot): AvailabilitySlot {
  return { ...slot };
}

function copyReservation(reservation: Reservation): Reservation {
  return { ...reservation };
}

function createImageDataUri(background: string, accent: string, label: string) {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="640" height="420" viewBox="0 0 640 420">
      <rect width="640" height="420" fill="${background}"/>
      <circle cx="520" cy="80" r="130" fill="${accent}" opacity="0.9"/>
      <rect x="54" y="240" width="270" height="34" rx="8" fill="#ffffff" opacity="0.86"/>
      <rect x="54" y="292" width="190" height="22" rx="7" fill="#ffffff" opacity="0.55"/>
      <text x="54" y="190" fill="#ffffff" font-family="Arial, sans-serif" font-size="58" font-weight="700">${label}</text>
    </svg>
  `;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}
