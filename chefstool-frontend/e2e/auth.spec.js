import { expect, test } from '@playwright/test'

function credenciales() {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  return {
    nombre: 'Prueba E2E',
    organizacion: 'Cocina E2E',
    email: `e2e-${sufijo}@example.com`,
    password: 'Temporal123!',
  }
}

test('rechaza credenciales invalidas y registra una cuenta', async ({ page }) => {
  await page.goto('/auth')
  await page.getByLabel('Correo electrónico').fill('no-existe@example.com')
  await page.locator('#password').fill('incorrecta')
  await page.locator('form').getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(page.locator('div').filter({ hasText: /contrase|credencial|usuario/i }).first()).toBeVisible()

  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  const cuenta = credenciales()
  await page.getByLabel('Tu nombre').fill(cuenta.nombre)
  await page.getByLabel('Nombre de la cocina u organización').fill(cuenta.organizacion)
  await page.getByLabel('Correo electrónico').fill(cuenta.email)
  await page.locator('#password').fill(cuenta.password)
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
})
