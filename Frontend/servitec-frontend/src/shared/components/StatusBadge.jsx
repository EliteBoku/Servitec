const ORDER_STATUS_CONFIG = {
  SOLICITADO: {
    label: 'Solicitado',
    classes: 'bg-sky-100 text-sky-800 border-sky-200',
    dot: 'bg-sky-500',
  },
  EN_DIAGNOSTICO: {
    label: 'En diagnóstico',
    classes: 'bg-amber-100 text-amber-800 border-amber-200',
    dot: 'bg-amber-500',
  },
  PRESUPUESTADO: {
    label: 'Presupuestado',
    classes: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    dot: 'bg-indigo-500',
  },
  EN_EJECUCION: {
    label: 'En Ejecución',
    classes: 'bg-blue-100 text-blue-800 border-blue-200',
    dot: 'bg-blue-600',
  },
  PAUSADA: {
    label: 'Pausada',
    classes: 'bg-orange-100 text-orange-800 border-orange-200',
    dot: 'bg-orange-500',
  },
  FINALIZADO: {
    label: 'Finalizado',
    classes: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    dot: 'bg-emerald-600',
  },
  CANCELADO: {
    label: 'Cancelado',
    classes: 'bg-rose-100 text-rose-800 border-rose-200',
    dot: 'bg-rose-600',
  },
};

const BUDGET_STATUS_CONFIG = {
  PENDIENTE_APROBACION: {
    label: 'Pendiente de aprobación',
    classes: 'bg-amber-100 text-amber-800 border-amber-200',
    dot: 'bg-amber-500',
  },
  APROBADO: {
    label: 'Aprobado',
    classes: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    dot: 'bg-emerald-600',
  },
  RECHAZADO: {
    label: 'Rechazado',
    classes: 'bg-rose-100 text-rose-800 border-rose-200',
    dot: 'bg-rose-600',
  },
};

export default function StatusBadge({ status, type = 'order' }) {
  const dictionary = type === 'budget' ? BUDGET_STATUS_CONFIG : ORDER_STATUS_CONFIG;
  const config = dictionary[status] || {
    label: status || 'Desconocido',
    classes: 'bg-slate-100 text-slate-700 border-slate-200',
    dot: 'bg-slate-400',
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${config.classes}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
}