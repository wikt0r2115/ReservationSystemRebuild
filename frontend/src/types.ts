export type Offer = {
  id: number;
  name: string;
  imageUrl: string;
  description: string;
  price: number;
  archived: boolean;
};

export type AvailabilityStatus = 'OPEN' | 'CLOSED' | 'CANCELLED';

export type AvailabilitySlot = {
  id: number;
  offerId: number;
  startsAt: string;
  endsAt: string;
  capacity: number;
  reservedCount: number;
  status: AvailabilityStatus;
};

export type ReservationStatus = 'CONFIRMED' | 'CANCELLED';

export type Reservation = {
  id: number;
  availabilitySlotId: number;
  offerId: number;
  customerName: string;
  customerEmail: string;
  partySize: number;
  status: ReservationStatus;
  createdAt: string;
  cancelledAt: string | null;
};

export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiErrorResponse = {
  code: string;
  message: string;
  details?: ApiFieldError[];
};

export type CreateReservationPayload = {
  availabilitySlotId: number;
  customerName: string;
  customerEmail: string;
  partySize: number;
};

export type CreateOfferPayload = {
  name: string;
  imageUrl: string;
  description: string;
  price: number;
};

export type CreateAvailabilitySlotPayload = {
  startsAt: string;
  endsAt: string;
  capacity: number;
};

export type Credentials = {
  username: string;
  password: string;
};
