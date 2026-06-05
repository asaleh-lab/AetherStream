package com.aetherstream.infrastructure.cqrs;

import com.aetherstream.application.cqrs.Command;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.application.cqrs.CommandHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SpringCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?>> handlers;

    public SpringCommandBus(List<CommandHandler<?>> handlers) {
        this.handlers =
                handlers.stream().collect(Collectors.toMap(CommandHandler::commandType, Function.identity()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Command> void dispatch(C command) {
        CommandHandler<C> handler = (CommandHandler<C>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for command: " + command.getClass().getName());
        }
        handler.handle(command);
    }
}
