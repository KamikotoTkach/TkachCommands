package tkachgeek.commands.command.arguments.datetime;

import org.bukkit.command.CommandSender;
import tkachgeek.commands.command.Argument;
import tkachgeek.tkachutils.datetime.StringToDuration;

import java.util.Arrays;
import java.util.List;

public class DurationArg extends Argument {
   @Override
   public boolean valid(String raw) {
      return StringToDuration.isValid(raw);
   }

   @Override
   public List<String> completions(CommandSender sender) {
      return Arrays.asList("y - год",
            "M - месяц",
            "w - неделя",
            "d - день",
            "h - час",
            "m - минута",
            "s - секунда",
            "t - тик");
   }

   @Override
   public String argumentName() {
      return "длительность";
   }

   @Override
   protected String hint() {
      return "Строка вида 1y2d100t - 1 год, 2 дня и 100 тиков. \nКомбинировать можно как угодно, повторять нельзя.";
   }
}
