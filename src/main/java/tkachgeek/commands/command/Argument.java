package tkachgeek.commands.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tkachgeek.commands.command.arguments.ExactStringArg;
import tkachgeek.commands.command.color.ColorGenerationStrategy;
import tkachgeek.tkachutils.messages.Message;
import tkachgeek.tkachutils.messages.MessageReturn;


import java.util.List;

public abstract class Argument {
  protected String raw;
  private String tag = "";
  private boolean optional;
  
  protected Argument(String raw) {
    this.raw = raw;
  }
  
  public Argument() {
  }
  
  /**
   * Оформление аргумента в авто-хелпе
   */
  @NotNull
  public TextComponent toComponent(ColorGenerationStrategy color, boolean canPerformedBy) {
    if (isOptional()) {
      return Component.text("[" + argumentName() + "]", color.optional(canPerformedBy));
    } else if (this instanceof ExactStringArg) {
      return Component.text(argumentName(), color.subcommand(canPerformedBy));
    } else {
      TextComponent component = Component.text("<" + argumentName() + ">", color.argument(canPerformedBy));
      if (hint().isEmpty()) {
        return component;
      }
      return component.hoverEvent(HoverEvent.showText(Component.text(hint(), color.accent(canPerformedBy))));
    }
  }
  
  public String toReadableString() {
    if (isOptional()) {
      return "[" + argumentName() + "]";
    } else if (this instanceof ExactStringArg) {
      return argumentName();
    } else {
      String args = "<" + argumentName() + ">";
      if (hint().isEmpty()) {
        return args;
      }
      return args;
    }
  }
  
  public abstract boolean valid(String raw);
  
  public boolean valid(CommandSender sender, String raw, List<String> arguments) {
    return valid(raw);
  }
  
  public abstract List<String> completions(CommandSender sender);
  
  public List<String> completions(CommandSender sender, List<String> written) {
    return completions(sender);
  }
  
  /**
   * Делает аргумент опциональным. Должен быть в конце, если таких несколько - все в конце
   */
  public Argument optional() {
    optional = true;
    return this;
  }
  
  /**
   * Название аргумента в хелпе
   */
  public abstract String argumentName();
  
  public boolean notNull() {
    return raw != null;
  }
  
  public Integer toInt() {
    try {
      if (notNull()) return Integer.parseInt(raw);
    } catch (Exception exception) {
      Bukkit.getLogger().warning("Не удалось преобразовать `" + raw + "` в int в " + argumentName());
    }
    return null;
  }
  
  public Float toFloat() {
    try {
      if (notNull()) return Float.parseFloat(raw);
    } catch (Exception exception) {
      Bukkit.getLogger().warning("Не удалось преобразовать `" + raw + "` в float в " + argumentName());
    }
    
    return null;
  }
  
  public String toString() {
    if (notNull()) return raw;
    Bukkit.getLogger().warning("Не удалось преобразовать `" + (raw != null ? raw : "~null~") + "` в строку в " + argumentName());
    return null;
  }
  
  public Double toDouble() {
    try {
      if (notNull()) return Double.parseDouble(raw);
    } catch (Exception exception) {
      Bukkit.getLogger().warning("Не удалось преобразовать `" + raw + "` в double в " + argumentName());
    }
    return null;
  }
  
  public Boolean toBoolean() {
    try {
      if (notNull()) return Boolean.parseBoolean(raw);
    } catch (Exception exception) {
      Bukkit.getLogger().warning("Не удалось преобразовать `" + raw + "` в boolean в " + argumentName());
    }
    
    return null;
  }
  
  public boolean isOptional() {
    return optional;
  }
  
  /**
   * Подсказка при наведении на аргумент в авто-хелпе
   */
  protected String hint() {
    return "";
  }
  
  @Deprecated
  public Argument newInstance() throws MessageReturn {
    try {
      return this.getClass().getDeclaredConstructor().newInstance();
    } catch (Exception ignored) {
      throw new MessageReturn(
         Message.getInstance("Не удалось получить новый экземпляр аргумента " + this.argumentName()).get()
      );
    }
  }
  
  public Argument tag(String tag) {
    this.tag = tag;
    return this;
  }
  
  public String getTag() {
    return tag.isEmpty() ? argumentName() : tag;
  }
  
  public Component invalidMessage(Command command, CommandSender sender, String written) {
    ColorGenerationStrategy colorScheme = command.getColorScheme();
    
    return Component.text(written, colorScheme.accent(true))
                    .append(Component.text(" нельзя представить как ", colorScheme.main()))
                    .append(Component.text(argumentName(), colorScheme.accent(true)));
  }
}
