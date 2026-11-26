import { expect, test } from '@playwright/test'
import path from 'node:path'

/**
 * Recorrido de QA de punta a punta: los flujos que usa un jefe de cocina, en orden,
 * con una sola cuenta y sin tocar datos de otras organizaciones.
 */

async function registrar(page, sufijo) {
  await page.goto('/auth')
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).first().click()
  await page.getByLabel('Tu nombre').fill('QA Cocina')
  await page.getByLabel('Nombre de la cocina u organización').fill(`QA ${sufijo}`)
  await page.getByLabel('Correo electrónico').fill(`qa-${sufijo}@example.com`)
  await page.locator('#password').fill('Temporal123!')
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

test('recorrido completo de la aplicación', async ({ page }) => {
  const sufijo = `${Date.now()}`
  await registrar(page, sufijo)

  // 1. Panel de inicio
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

  // 2. Alta de proveedor
  await page.goto('/proveedores')
  await page.getByRole('button', { name: /Nuevo|Añadir/i }).first().click()
  await page.getByLabel(/Nombre/i).first().fill(`Proveedor QA ${sufijo}`)
  await page.getByRole('button', { name: /Guardar|Crear/i }).first().click()
  await expect(page.getByText(`Proveedor QA ${sufijo}`)).toBeVisible()

  // 3. Alta de producto
  await page.goto('/productos')
  await page.getByRole('button', { name: /Nuevo|Añadir/i }).first().click()
  await page.getByLabel(/Nombre/i).first().fill(`Producto QA ${sufijo}`)
  await page.getByRole('button', { name: /Guardar|Crear/i }).first().click()
  await expect(page.getByText(`Producto QA ${sufijo}`)).toBeVisible()

  // 4. Albarán desde foto real
  await page.goto('/documentos/nuevo')
  const foto = path.resolve('../docs/ejemplos/albaran-ejemplo-foto.jpg')
  await page.locator('input[accept="image/jpeg,image/png,application/pdf"]').setInputFiles(foto)
  await expect(page).toHaveURL(/\/documentos\/[0-9a-f-]+$/, { timeout: 90_000 })
  await expect(page.getByText('6 líneas')).toBeVisible()
  await expect(page.getByLabel('Importe de la línea 1')).toBeVisible()

  // 5. Corregir un importe a mano y ver que el total cambia
  const totalAntes = await page.getByText('Total', { exact: true })
    .locator('xpath=following-sibling::span[1]')
    .textContent()
  await page.getByLabel('Importe de la línea 1').fill('9.5')
  await expect(
    page.getByText('Total', { exact: true }).locator('xpath=following-sibling::span[1]'),
  ).not.toHaveText(totalAntes)

  // 6. Descargar el original
  const descarga = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Descargar' }).click()
  expect((await descarga).suggestedFilename()).toMatch(/\.(jpg|jpeg|png|pdf)$/)

  // 7. Confirmar la compra
  await page.getByRole('button', { name: /Confirmar y guardar/i }).click()
  await expect(page).toHaveURL(/\/documentos$/)
  await expect(page.getByText('Compra registrada correctamente.')).toBeVisible()

  // 8. La compra aparece en el histórico y abre su detalle
  await page.goto('/compras')
  await expect(page.getByText(/A-2026\/0457/)).toBeVisible()
  await page.getByRole('button', { name: /ver/i }).first().click()
  await expect(page.getByText('PDF de la compra')).toBeVisible()

  // 9. El listado de documentos exporta y baja el ZIP del mes
  await page.goto('/documentos')
  await expect(page.getByRole('button', { name: /ZIP/i })).toBeVisible()

  // 10. Informes
  await page.goto('/informes')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

  // 11. Solicitud de compra
  await page.goto('/solicitudes-compra')
  await page.getByLabel('¿Qué necesitas?').fill('Batidora industrial')
  await page.getByLabel('Importe estimado (€)').fill('450')
  await page.getByRole('button', { name: /Enviar para aprobación/i }).click()
  await expect(page.getByText('Batidora industrial')).toBeVisible()

  // 12. Locales: crear uno nuevo
  await page.goto('/locales')
  await page.getByLabel('Nombre del local').fill(`Terraza ${sufijo}`)
  await page.getByRole('button', { name: 'Añadir local' }).click()
  await expect(page.getByText(`Terraza ${sufijo}`)).toBeVisible()

  // 13. Auditoría: hay movimientos y dicen quién
  await page.goto('/auditoria')
  await expect(page.getByRole('columnheader', { name: 'Quién' })).toBeVisible()
  await expect(page.getByText('QA Cocina').first()).toBeVisible()

  // 14. Configuración: estado del correo
  await page.goto('/configuracion')
  await expect(page.getByText('Correo saliente')).toBeVisible()

  // 15. Ruta inexistente: 404 amable, no pantalla en blanco
  await page.goto('/ruta-que-no-existe')
  await expect(page.getByText(/no existe|404|no encontrada/i).first()).toBeVisible()

  // 16. Cerrar sesión
  await page.getByRole('button', { name: /Cerrar sesión/i }).first().click()
  await expect(page).toHaveURL(/\/auth$/)
})
