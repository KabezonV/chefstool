import { expect, test } from '@playwright/test'

test.use({ viewport: { width: 390, height: 844 }, isMobile: true })

async function entrar(page) {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  await page.goto('/auth')
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  await page.getByLabel('Tu nombre').fill('Prueba móvil')
  await page.getByLabel('Nombre de la cocina u organización').fill('Cocina móvil')
  await page.getByLabel('Correo electrónico').fill(`movil-${sufijo}@example.com`)
  await page.locator('#password').fill('Temporal123!')
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

test('no desborda la pantalla móvil y permite abrir la carga', async ({ page }) => {
  await entrar(page)
  await page.goto('/documentos/nuevo')

  await expect(page.getByRole('heading', { name: 'Añadir documento' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Hacer foto', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Subir archivo', exact: true })).toBeVisible()

  const desborde = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(desborde).toBeLessThanOrEqual(1)
})
