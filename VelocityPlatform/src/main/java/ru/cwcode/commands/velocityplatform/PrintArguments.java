package ru.cwcode.commands.velocityplatform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import ru.cwcode.commands.velocityplatform.executor.Executor;
import ru.cwcode.cwutils.messages.MessageReturn;

public class PrintArguments extends Executor {
  @Override
  public void executeForPlayer() throws MessageReturn {
    for (int i = 0; isPresent(i); i++) {
      sender().sendMessage(arg(i).toComponent(getCommand().getRootCommand().getColorScheme(), true)
                                 .append(Component.text(": " + argS(i)))
                                 .hoverEvent(HoverEvent.showText(Component.text(arg(i).getClass().getSimpleName()))));
    }
  }
}
