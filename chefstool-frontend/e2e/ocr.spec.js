import { expect, test } from '@playwright/test'
import path from 'node:path'

async function registrar(page) {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  await page.goto('/auth')
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  await page.getByLabel('Tu nombre').fill('Prueba OCR')
  await page.getByLabel('Nombre de la cocina u organización').fill('Cocina OCR')
  await page.getByLabel('Correo electrónico').fill(`ocr-${sufijo}@example.com`)
  await page.locator('#password').fill('Temporal123!')
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

test('reconoce una foto pobre y confirma sus seis líneas', async ({ page }) => {
  await registrar(page)
  await page.goto('/documentos/nuevo')

  const foto = path.resolve('../docs/ejemplos/albaran-ejemplo-foto.jpg')
  await page.locator('input[accept="image/jpeg,image/png,application/pdf"]').setInputFiles(foto)

  await expect(page).toHaveURL(/\/documentos\/[0-9a-f-]+$/, { timeout: 60_000 })
  await expect(page.getByLabel('Nombre del proveedor nuevo')).toHaveValue(/DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO/i)
  await expect(page.getByLabel('Fecha')).toHaveValue('2026-11-08')
  await expect(page.getByText('6 líneas')).toBeVisible()

  await page.getByRole('button', { name: 'Confirmar y guardar' }).click()
  await expect(page).toHaveURL(/\/documentos$/)
  await expect(page.getByText('Compra registrada correctamente.')).toBeVisible()
})
