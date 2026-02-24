package com.amongus.core.api.events;

import java.util.function.Consumer;

public interface EventBus {

    <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> handler);

    void publish(GameEvent event);
}
