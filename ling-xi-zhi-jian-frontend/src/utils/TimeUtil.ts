export default class TimeUtil {
    static getCurrentTime(
        formatStr:
            | 'YYYY-MM-DD'
            | 'YYYY-MM'
            | 'YYYY-MM-DD HH:mm' = 'YYYY-MM-DD HH:mm',
    ) {
        return TimeUtil.format(new Date(), formatStr);
    }

    static format(date: Date, formatStr: string): string {
        const year = date.getFullYear().toString();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        return formatStr
            .replace('YYYY', String(year))
            .replace('MM', month)
            .replace('DD', day)
            .replace('HH', hour)
            .replace('mm', minute);
    }

    static parse(str: string, formatStr: 'HH:mm' | 'YYYY-MM-DD'): Date {
        if (formatStr === 'HH:mm') {
            const [hour, minute] = str.split(':');
            return new Date(0, 0, 0, Number(hour), Number(minute));
        } else {
            const [year, month, day] = str.split('-');
            return new Date(Number(year), Number(month) - 1, Number(day));
        }
    }
}
