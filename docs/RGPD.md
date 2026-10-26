# RGPD: estado y pendientes

Checklist de lo que la aplicación ya cumple **por sí sola** y lo que depende de decisiones
de la empresa (no de código). No sustituye a la revisión de un abogado.

## 1. Ya implementado en la aplicación

| Requisito (RGPD) | Cómo se cumple | Dónde |
| --- | --- | --- |
| Información al interesado (art. 13) | Aviso de privacidad en lenguaje claro, enlazado desde el acceso | `/privacidad` (`Privacidad.jsx`) |
| Derecho de acceso y portabilidad (arts. 15 y 20) | Descarga en JSON de la cuenta y sus organizaciones | `GET /api/auth/datos`, botón en *Configuración* |
| Derecho de supresión (art. 17) | Baja con confirmación de contraseña; borra usuario, invitaciones y, si es el único miembro, la organización completa (cascade en base de datos) | `DELETE /api/auth/cuenta`, `BajaCuentaService` |
| Integridad de la baja | No se puede dejar una organización con equipo y sin propietario (responde 409 y pide nombrar otro antes) | `BajaCuentaService.comprobarPropiedad` |
| Minimización (art. 5.1.c) | Solo se guardan datos de cuenta, catálogo, compras y documentos; el OCR no guarda imágenes de terceros | Modelo de datos |
| Confidencialidad (art. 32) | Contraseñas con BCrypt, JWT firmado, aislamiento por organización en todas las consultas, almacenamiento de archivos privado (URLs firmadas), límite de intentos de login | `SeguridadConfig`, `Permisos`, `AlmacenamientoS3` |
| Cifrado en tránsito | HTTPS en el proxy y TLS hacia la base de datos (`sslmode`) | `docker-compose.yml`, despliegue |
| Registro de actividad de seguridad | Logs de aplicación con usuario y organización; alertas de login fallido por IP | `LimitadorIntentos` |

## 2. Pendiente de decisión de empresa (no es código)

1. **Encargados del tratamiento (art. 28)**: firmar el contrato de encargo con el proveedor
   de alojamiento, con el de base de datos (si es gestionada) y con el proveedor de OCR con
   IA si se activa. Sin ese contrato no se puede usar el servicio en producción.
2. **Registro de actividades de tratamiento (art. 30)**: documento de una página con
   finalidades, categorías de datos, plazos y medidas. Plantilla lista para rellenar.
3. **Transferencias internacionales (arts. 44-49)**: si el alojamiento o el OCR están fuera
   del EEE, hacen falta cláusulas contractuales tipo y evaluación de impacto del proveedor.
4. **Plazo de conservación**: definir cuántos años se guardan albaranes y compras (fiscal:
   4 años para documentación contable; 6 para libros). Hoy no hay purga automática.
5. **Copias de seguridad**: política de retención (hoy 30 días propuesto) y prueba de
   restauración documentada.
6. **Revisión legal del aviso de privacidad**: identidad del responsable, NIF y correo de
   contacto reales (hoy el texto está redactado pero sin datos de la empresa).
7. **Procedimiento de brechas (art. 33)**: quién avisa a la AEPD en 72 h y cómo se
   documenta el incidente.

## 3. Mejoras opcionales (baratas, cuando haya tiempo)

- Purga automática de documentos y compras anteriores al plazo de conservación.
- Segundo factor (TOTP) para propietarios y administradores.
- Exportación completa de la organización (no solo datos personales) en un ZIP, útil para
  el derecho de portabilidad de la empresa cliente.
- Borrado de los archivos del almacenamiento S3 al eliminar una organización (hoy quedan
  objetos huérfanos en el bucket, sin metadatos que los enlacen a nadie).

## 4. Cómo probarlo en dos minutos

```bash
# 1. datos personales
curl -s localhost:8081/api/auth/datos -H "Authorization: Bearer $TOKEN"

# 2. baja con contraseña incorrecta -> 401
curl -o /dev/null -w '%{http_code}\n' -X DELETE localhost:8081/api/auth/cuenta \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"password":"mal"}'

# 3. baja correcta -> 204 y el login posterior devuelve 401
curl -o /dev/null -w '%{http_code}\n' -X DELETE localhost:8081/api/auth/cuenta \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"password":"chefstool2026"}'
```
