import { expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Field } from './components/ui'
import { PublicLayout } from './components/Layout'
vi.mock('./context/AuthContext', () => ({ useAuth: () => ({ user: null }) }))

it('keeps labels separate from help text and preserves existing descriptions', () => {
  render(<><p id="existing-help">Existing guidance</p><Field label="Location" hint="Use a verified floor or wing."><input id="location" aria-describedby="existing-help" /></Field></>)
  const input = screen.getByRole('textbox', { name: 'Location' })
  expect(input).toHaveAccessibleDescription('Existing guidance Use a verified floor or wing.')
  expect(screen.getByLabelText('Location')).toBe(input)
})

it('labels a nested password input without including its toggle button', () => {
  render(<Field label="Password" hint="At least 12 characters."><span><input type="password" /><button type="button">Show password</button></span></Field>)
  expect(screen.getByLabelText('Password', { exact: true })).toHaveAccessibleName('Password')
  expect(screen.getByLabelText('Password', { exact: true })).toHaveAccessibleDescription('At least 12 characters.')
  expect(screen.getByRole('button', { name: 'Show password' })).toHaveAttribute('type', 'button')
})

it('closes the public mobile menu with Escape and restores trigger focus', () => {
  render(<MemoryRouter><PublicLayout /></MemoryRouter>)
  const trigger = screen.getByRole('button', { name: 'Open menu' })
  fireEvent.click(trigger)
  expect(trigger).toHaveAttribute('aria-expanded', 'true')
  screen.getByRole('link', { name: 'Rooms' }).focus()
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(trigger).toHaveAttribute('aria-expanded', 'false')
  expect(trigger).toHaveFocus()
})
