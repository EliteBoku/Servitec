/**
 * Sección 11.3 del SRS: Formato de moneda en pesos colombianos ($ XX.XXX COP) sin decimales.
 */
export function formatCurrencyCOP(value) {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return '$ 0 COP';
  }
  const rounded = Math.round(Number(value));
  const formattedNumber = new Intl.NumberFormat('es-CO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(rounded);

  return `$ ${formattedNumber} COP`;
}

/**
 * Formatea cantidades de inventario (ej. 10.00 -> 10, 1.50 -> 1,5).
 */
export function formatQuantity(value) {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return '0';
  }
  return new Intl.NumberFormat('es-CO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(Number(value));
}

/**
 * Sección 11.3 del SRS: Fecha en convención visual DD/MM/AAAA.
 */
export function formatDate(dateString) {
  if (!dateString) return 'Sin definir';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return dateString;

  return new Intl.DateTimeFormat('es-CO', {
    timeZone: 'America/Bogota',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

/**
 * Sección 11.3 del SRS: Fecha DD/MM/AAAA y hora en formato de 12 horas con indicador AM/PM.
 */
export function formatDateTime(dateString) {
  if (!dateString) return 'Sin registro';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return dateString;

  return new Intl.DateTimeFormat('es-CO', {
    timeZone: 'America/Bogota',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  }).format(date);
}

/**
 * Convierte el código del rol a su etiqueta legible en español.
 */
export function formatRoleName(role) {
  const roles = {
    ADMINISTRADOR: 'Administrador',
    TECNICO_ESPECIALISTA: 'Técnico especialista',
    CLIENTE: 'Cliente',
  };
  return roles[role] || role;
}