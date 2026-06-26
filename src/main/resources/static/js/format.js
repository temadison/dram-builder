export function money(value) {
  return value == null ? '—' : number(value, { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function percent(value) {
  return value == null ? '—' : `${number(value, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
}

export function decimal(value, digits = 2) {
  return value == null ? '—' : number(value, { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

export function dateTime(value) {
  if (!value) {
    return '—';
  }
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  }).format(new Date(value));
}

export function signedClass(value) {
  const numeric = Number(value || 0);
  if (numeric > 0) {
    return 'positive';
  }
  if (numeric < 0) {
    return 'negative';
  }
  return 'neutral';
}

function number(value, options) {
  return new Intl.NumberFormat(undefined, options).format(Number(value));
}
