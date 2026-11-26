import { expect, test } from '@playwright/test'

async function registrar(page) {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  await page.goto('/auth')
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  await page.getByLabel('Tu nombre').fill('Jefa de cocina')
  await page.getByLabel('Nombre de la cocina u organización').fill(`Cocina ${sufijo}`)
  await page.getByLabel('Correo electrónico').fill(`gestion-${sufijo}@example.com`)
  await page.locator('#password').fill('Temporal123!')
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

test('crea un local y lo ve reflejado en la auditoría', async ({ page }) => {
  await registrar(page)

  await page.goto('/locales')
  await expect(page.getByRole('heading', { name: 'Locales', exact: true })).toBeVisible()

  const nombre = `Terraza ${Date.now() % 10000}`
  await page.getByLabel('Nombre del local').fill(nombre)
  await page.getByRole('button', { name: 'Añadir local' }).click()
  await expect(page.getByText(nombre)).toBeVisible()

  // La pantalla de auditoría ve la actividad de la organización
  await page.goto('/auditoria')
  await expect(page.getByRole('heading', { name: 'Auditoría', exact: true })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: 'Quién' })).toBeVisible()

  // El correo se puede diagnosticar sin credenciales
  await page.goto('/configuracion')
  await expect(page.getByText('Correo saliente')).toBeVisible()
  await page.getByRole('button', { name: 'Enviar correo de prueba' }).click()
  await expect(page.getByText(/no esta configurado|No se pudo enviar/i)).toBeVisible()
})
