package com.amongus.core.impl.event;

import com.amongus.core.api.events.GameEvent;
import com.amongus.core.api.events.EventBus;

import java.util.*;
import java.util.function.Consumer;

public class EventBusImpl implements EventBus {

    /**  Mapa que almacena las suscripciones a eventos. *
     *  - La clave (Class<?>):
     *  Representa el tipo de evento. Ejemplo: KillAttemptedEvent.class.
     *  Se usa Class<?> porque puede ser cualquier subclase de GameEvent.
     *
     *  - El valor (List<Consumer<?>>):
     *   Es una lista de funciones (handlers) que deben ejecutarse cuando
     *   se publique un evento del tipo asociado.
     *
     *  En resumen:
        subscribers asocia "tipo de evento" -> "lista de funciones que reaccionan a ese evento".

     */

    private final Map<Class<?>, List<Consumer<?>>> subscribers = new HashMap<>();

    /**
     *
     * Registra un handler(manejador) para un tipo específico de evento.
     *
     * @Param eventType Clase del evento al que se quiere suscribir. Ejm: KillAttemptedEvent
     *
     * @Param handler Función que se ejecutará cuando se publique un evento del tipo indicado.
     *                Recibe una instancia del evento.
     * <p>
     *  computeIfAbsent(eventType, k -> new ArrayList<>()):
     *      - Si no existe una lista para ese tipo de evento, la crea.
     *      - Si ya existe, simplemente la reutiliza.
     * <p>
     *  add(handler):
     *      Agrega el handler a la lista de funciones asociadas al evento.
     *
     * */
    @Override
    public <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> handler){
        subscribers.computeIfAbsent(eventType, k-> new ArrayList<>()).add(handler);
    }


    /// Publica un evento, ejecutando todos los handlers asociados a su tipo.
    ///
    /// @Param event Instancia del evento que ocurrió
    /// subcribers.get(event.getClass()):
    ///              Obtiene la lista de handlers registrados para el tipo exacto del evento.
    ///              si no hay handlers, devuelve null.
    ///<P>
    /// if(handlers == null) return:
    ///              Sí nadie está suscrito a este evento, no hay nada que ejecutar.
    ///
    /// for (Consumer<?> handler: handlers):
    ///      Itera sobre cada función registrada para este tipo de evento.
    ///<P>
    ///(Consumer<GameEvent>) handler:
    ///           Se hace un cast forzado porque la lista usa Consumer<?>.
    ///            Es seguro porque subscribe garantiza que el tipo coincide.
    ///<P>
    /// handler.accept(event):
    ///     Ejecuta la función pasando el evento como parámetro.

    @Override
    @SuppressWarnings("unchecked")


    public void publish (GameEvent evet){
        var handlers = subscribers.get(evet.getClass());
        if(handlers == null) return;

        for (Consumer<?> handler : handlers){
            ((Consumer<GameEvent>) handler).accept(evet);
        }
    }

}
