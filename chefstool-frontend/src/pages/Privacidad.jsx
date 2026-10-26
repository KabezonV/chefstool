import { Link } from 'react-router-dom'
import { ArrowLeft, ChefHat } from 'lucide-react'

const SECCIONES = [
  {
    titulo: 'Quién trata tus datos',
    texto:
      'Chefstool es una aplicación de control de costes de cocina. El responsable del ' +
      'tratamiento es la empresa usuaria de la aplicación (tu organización), que decide qué ' +
      'datos introduce. El soporte técnico de Chefstool solo accede a ellos para mantener el ' +
      'servicio.',
  },
  {
    titulo: 'Qué datos guardamos',
    texto:
      'Datos de cuenta (nombre, correo y contraseña cifrada), datos de tu organización ' +
      '(proveedores, productos, compras, albaranes y fichas técnicas) y los archivos que subes ' +
      '(fotos o PDF de albaranes).',
  },
  {
    titulo: 'Para qué y con qué base jurídica',
    texto:
      'Los datos se tratan para ejecutar el contrato de servicio que tienes con tu organización ' +
      '(art. 6.1.b RGPD): calcular costes, generar informes y avisarte de subidas de precio. No ' +
      'se usan para publicidad ni se venden a terceros.',
  },
  {
    titulo: 'Cuánto tiempo',
    texto:
      'Mientras tu cuenta esté activa. Si pides la baja, se eliminan tu cuenta y, si eras el ' +
      'único miembro, todos los datos de la organización. Las copias de seguridad se rotan y se ' +
      'sobrescriben en un plazo máximo de 30 días.',
  },
  {
    titulo: 'Quién más los ve',
    texto:
      'Solo los proveedores técnicos necesarios para prestar el servicio (alojamiento, base de ' +
      'datos y almacenamiento de archivos), que actúan como encargados del tratamiento. Si el OCR ' +
      'con IA está activado, la imagen del albarán se envía al proveedor de IA indicado únicamente ' +
      'para extraer sus líneas. Los documentos se guardan en un almacenamiento privado accesible ' +
      'solo con sesión iniciada.',
  },
  {
    titulo: 'Tus derechos',
    texto:
      'Puedes ejercer acceso, rectificación, supresión, oposición, limitación y portabilidad desde ' +
      'Configuración: ahí puedes descargar tus datos en JSON o darte de baja. También puedes ' +
      'reclamar ante la Agencia Española de Protección de Datos (www.aepd.es).',
  },
]

export default function Privacidad() {
  return (
    <div className="min-h-screen bg-background px-4 py-10">
      <div className="mx-auto max-w-2xl space-y-6">
        <div className="flex items-center gap-2">
          <span className="grid size-10 place-items-center rounded-xl bg-primary text-primary-foreground">
            <ChefHat className="size-5" />
          </span>
          <div>
            <p className="text-lg font-semibold tracking-tight">Chefstool</p>
            <p className="text-xs text-muted">Aviso de privacidad</p>
          </div>
        </div>

        <div className="space-y-4 rounded-xl border border-border bg-card p-5">
          <h1 className="text-xl font-semibold tracking-tight">Protección de datos</h1>
          <p className="text-sm text-muted">
            Resumen en lenguaje claro de cómo tratamos los datos personales, conforme al
            Reglamento (UE) 2016/679 (RGPD) y a la LOPDGDD.
          </p>

          {SECCIONES.map(({ titulo, texto }) => (
            <section key={titulo} className="space-y-1">
              <h2 className="text-sm font-semibold">{titulo}</h2>
              <p className="text-sm text-muted">{texto}</p>
            </section>
          ))}

          <p className="border-t border-border pt-3 text-xs text-muted">
            Última actualización: pendiente de revisión legal antes de la puesta en producción.
          </p>
        </div>

        <Link
          to="/auth"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Volver al acceso
        </Link>
      </div>
    </div>
  )
}
