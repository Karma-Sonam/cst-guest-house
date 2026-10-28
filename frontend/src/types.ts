export type Role = 'USER' | 'ADMIN'
export interface User { id: string; fullName: string; email: string; phone: string | null; role: Role }
export interface Token { token: string; expiresIn: number; user: User }
export interface Page<T> { content: T[]; totalElements: number; page: number; size: number }
export interface Room { id: string; roomNumber: string; category: 'STANDARD' | 'SPECIAL'; beds: number | null; normalCapacity: number | null; maxCapacity: number | null; supportsExtraBed: boolean; serviceStatus: string; extraBedRequired: boolean; location?: string | null; description?: string | null; amenities?: string | null }
export interface Booking { id: string; reference: string; userId: string; fullName: string; email: string; phone: string | null; organization: string | null; designation: string | null; guestCategory: string; purpose: string; guests: number; checkIn: string; checkOut: string; preferredCategory: string; assignedRoomId: string | null; extraBedRequested: boolean; extraBedDecision: string; extraBedAdded: boolean; status: string; userRemark: string | null; adminRemark: string | null; actualCheckIn: string | null; actualCheckOut: string | null; createdAt: string }
export interface Event { previousStatus: string | null; newStatus: string; changedBy: string; remark: string; changedAt: string }
export interface BookingDetail { booking: Booking; timeline: Event[] }
export interface FacilityRequest { id: string; facilityType: 'CONVENTION' | 'DINING'; userId: string; bookingId: string | null; title: string; purpose: string; usageDate: string; startTime: string; endTime: string; mealPeriod: string | null; participants: number; status: string; userRemark: string | null; adminRemark: string | null; schedulingConflict: boolean; createdAt: string }
export interface FacilityDetail { request: FacilityRequest; timeline: Event[] }
export interface Config { registrationEnabled: boolean; totalRooms?: number; standardRooms?: number; specialRooms?: number; totalFacilities?: number; contact?: { phone: string; email: string; address: string } }
export interface Dashboard { totalRooms: number; standardRooms: number; specialRooms: number; availableRooms: number; occupiedRooms: number; maintenanceRooms: number; pendingBookings: number; approvedBookings: number; currentGuests: number; todayArrivals: number; todayDepartures: number; extraBedRequests: number; conventionHallPending: number; diningHallPending: number }
export interface BookingCounts { total: number; pending: number; approved: number; completed: number }
