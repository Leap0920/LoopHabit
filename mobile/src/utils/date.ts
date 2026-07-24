import { format, parseISO, startOfMonth, endOfMonth, eachDayOfInterval, getDay } from 'date-fns';

export function todayYmd(): string {
  return format(new Date(), 'yyyy-MM-dd');
}

export function formatDisplayDate(ymd: string): string {
  try {
    return format(parseISO(ymd), 'MMMM d');
  } catch {
    return ymd;
  }
}

export function formatMonthYear(date: Date): string {
  return format(date, 'MMMM yyyy');
}

export function daysInMonthGrid(month: Date): { date: Date; ymd: string; inMonth: boolean }[] {
  const start = startOfMonth(month);
  const end = endOfMonth(month);
  const days = eachDayOfInterval({ start, end });
  // Pad start so week starts on Sunday (0)
  const pad = getDay(start);
  const leading = Array.from({ length: pad }, (_, i) => {
    const d = new Date(start);
    d.setDate(d.getDate() - (pad - i));
    return { date: d, ymd: format(d, 'yyyy-MM-dd'), inMonth: false };
  });
  const body = days.map((d) => ({ date: d, ymd: format(d, 'yyyy-MM-dd'), inMonth: true }));
  return [...leading, ...body];
}

export function formatTimer(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
}
