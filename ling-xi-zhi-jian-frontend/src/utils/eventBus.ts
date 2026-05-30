class EventBus {
    private listeners: {
        [eventName: string]: ((...args: string[]) => void)[];
    } = {};

    public on(eventName: string, listener: (...args: string[]) => void) {
        if (!this.listeners[eventName]) {
            this.listeners[eventName] = [];
        }
        this.listeners[eventName].push(listener);
    }

    public off(eventName: string, listener: (...args: string[]) => void) {
        if (!this.listeners[eventName]) {
            return;
        }
        this.listeners[eventName] = this.listeners[eventName].filter(
            (l) => l !== listener,
        );
    }

    public emit(eventName: string, ...args: string[]) {
        if (!this.listeners[eventName]) {
            return;
        }
        this.listeners[eventName].forEach((listener) => listener(...args));
    }
}

export const eventBus = new EventBus(); // Singleton
