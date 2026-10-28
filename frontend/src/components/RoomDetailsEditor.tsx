import { useState, type FormEvent } from 'react'
import type { Room } from '../types'
import { api, errorMessage } from '../services/api'
import { Field, ErrorBox, Submit } from './ui'

export function RoomDetailsEditor({ room, onSaved, onClose }: { room: Room; onSaved: () => void; onClose: () => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget); setBusy(true); setError('')
    try {
      await api.put(`/admin/rooms/${room.id}`, { roomNumber: String(form.get('roomNumber')).trim(), beds: room.category === 'STANDARD' ? 2 : form.get('beds') ? Number(form.get('beds')) : null, location: form.get('location'), description: form.get('description'), amenities: form.get('amenities') })
      onSaved()
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }
  return <section className="panel room-edit-panel"><form className="stack-form" onSubmit={save}><h2>Edit room {room.roomNumber}</h2><p>Enter property-confirmed details only. Guest-facing details are visible on the room information page. Active room allocations must be resolved first.</p><Field label="Room number"><input name="roomNumber" autoFocus required maxLength={20} pattern="[A-Za-z0-9][A-Za-z0-9 \-]*" defaultValue={room.roomNumber} /></Field><Field label="Regular beds" hint={room.category === 'STANDARD' ? 'Standard rooms retain two regular beds, with one optional approved extra bed.' : 'Leave blank if the bed count is unverified. Bed count does not set guest capacity.'}><input name="beds" type="number" min={1} max={3} defaultValue={room.beds ?? ''} disabled={room.category === 'STANDARD'} /></Field><Field label="Location" hint="For example, a property-confirmed floor or wing."><input name="location" maxLength={120} defaultValue={room.location ?? ''} /></Field><Field label="Description"><textarea name="description" maxLength={1000} rows={4} defaultValue={room.description ?? ''} /></Field><Field label="Amenities" hint="List only verified amenities; leave blank when unknown."><textarea name="amenities" maxLength={500} rows={3} defaultValue={room.amenities ?? ''} /></Field>{error && <ErrorBox message={error} />}<div className="button-row"><Submit busy={busy}>Save room details</Submit><button type="button" className="button secondary" disabled={busy} onClick={onClose}>Go back</button></div></form></section>
}
