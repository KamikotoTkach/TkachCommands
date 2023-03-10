package tkachgeek.commands.command.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import tkachgeek.commands.command.arguments.executor.Executor;
import tkachgeek.commands.command.color.ColorGenerationStrategy;
import tkachgeek.commands.command.color.DefaultColorGenerationStrategy;
import tkachgeek.tkachutils.messages.MessageReturn;

public class PrintArguments extends Executor {
  @Override
  public void executeForPlayer() throws MessageReturn {
    ColorGenerationStrategy color = new DefaultColorGenerationStrategy();
    for (int i = 0; isPresent(i); i++) {
      sender().sendMessage(arg(i).toComponent(color, true).append(Component.text(": " + argS(i))).hoverEvent(HoverEvent.showText(Component.text(arg(i).getClass().getSimpleName()))));
    }
  }
}
