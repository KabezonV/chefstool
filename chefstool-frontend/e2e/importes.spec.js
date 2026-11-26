import { expect, test } from '@playwright/test'
import path from 'node:path'

async function registrar(page) {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  await page.goto('/auth')
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  await page.getByLabel('Tu nombre').fill('Prueba importes')
  await page.getByLabel('Nombre de la cocina u organización').fill('Cocina importes')
  await page.getByLabel('Correo electrónico').fill(`importes-${sufijo}@example.com`)
  await page.locator('#password').fill('Temporal123!')
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

async function eurosDe(page, etiqueta) {
  const texto = await page
    .getByText(etiqueta, { exact: true })
    .locator('xpath=following-sibling::span[1]')
    .textContent()
  return Number(texto.replace(/[^\d,-]/g, '').replace(',', '.'))
}

test('corrige a mano el importe de una línea y recalcula base y total', async ({ page }) => {
  await registrar(page)
  await page.goto('/documentos/nuevo')

  const foto = path.resolve('../docs/ejemplos/albaran-ejemplo-foto.jpg')
  await page.locator('input[accept="image/jpeg,image/png,application/pdf"]').setInputFiles(foto)
  await expect(page).toHaveURL(/\/documentos\/[0-9a-f-]+$/, { timeout: 60_000 })

  const importe = page.getByLabel('Importe de la línea 1')
  await expect(importe).toBeVisible()

  const baseAntes = await eurosDe(page, 'Base imponible')
  const totalAntes = await eurosDe(page, 'Total')

  await importe.fill('9.5')

  const baseDespues = await eurosDe(page, 'Base imponible')
  const totalDespues = await eurosDe(page, 'Total')

  expect(baseDespues).not.toBe(baseAntes)
  expect(totalDespues).not.toBe(totalAntes)
  // El total sigue siendo base + IVA: nunca queda un total suelto sin cuadrar
  expect(totalDespues).toBeCloseTo(Math.round(baseDespues * 1.1 * 100) / 100, 1)

  await page.getByRole('button', { name: /Confirmar y guardar/i }).click()
  await expect(page).toHaveURL(/\/documentos$/)
  await expect(page.getByText('Compra registrada correctamente.')).toBeVisible()
})
