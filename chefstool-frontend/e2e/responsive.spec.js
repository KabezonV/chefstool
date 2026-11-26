import { expect, test } from '@playwright/test'

/** Ninguna pantalla debe arrastrar la página en horizontal (ni en móvil ni en tablet). */
const ANCHOS = [
  { nombre: 'móvil pequeño', width: 320, height: 640 },
  { nombre: 'móvil', width: 390, height: 844 },
  { nombre: 'tablet', width: 768, height: 1024 },
]

const RUTAS = ['/dashboard', '/documentos', '/compras', '/productos', '/informes', '/locales']

test('ninguna pantalla desborda en móvil ni tablet', async ({ page }) => {
  const sufijo = `${Date.now()}`
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/auth')
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  await page.getByLabel('Tu nombre').fill('QA Responsive')
  await page.getByLabel('Nombre de la cocina u organización').fill(`Resp ${sufijo}`)
  await page.getByLabel('Correo electrónico').fill(`resp-${sufijo}@example.com`)
  await page.locator('#password').fill('Temporal123!')
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)

  for (const { nombre, width, height } of ANCHOS) {
    await page.setViewportSize({ width, height })
    for (const ruta of RUTAS) {
      await page.goto(ruta)
      const desborde = await page.evaluate(
        () => document.documentElement.scrollWidth - window.innerWidth,
      )
      expect(desborde, `${ruta} desborda en ${nombre} (${width}px)`).toBeLessThanOrEqual(1)
    }
  }
})
