package tkachgeek.commands.command.arguments.basic;

import org.bukkit.command.CommandSender;
import tkachgeek.commands.command.Argument;
import tkachgeek.commands.command.CompletionStyle;
import tkachgeek.tkachutils.numbers.NumbersUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class DoubleArg extends Argument {
  double min = -Double.MAX_VALUE;
  double max = Double.MAX_VALUE;
  CompletionStyle style = CompletionStyle.PLACEHOLDER;
  double step = 1.0;
  String placeholder = "дробное число";
  
  public DoubleArg(String placeholder) {
    this.placeholder = placeholder;
  }
  
  public DoubleArg() {
  }
  
  public DoubleArg setMin(double min) {
    this.min = min;
    return this;
  }
  
  public DoubleArg setMax(double max) {
    this.max = max;
    return this;
  }
  
  public DoubleArg setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
    return this;
  }
  
  public DoubleArg setStyle(CompletionStyle style) {
    this.style = style;
    return this;
  }
  
  public DoubleArg setStep(double step) {
    this.step = step;
    return this;
  }
  
  @Override
  public boolean valid(String raw) {
    try {
      if (!NumbersUtils.isNumber(raw)) return false;
      
      double parsed = Double.parseDouble(raw);
      return parsed >= min && parsed < max;
    } catch (NumberFormatException ignored) {
    }
    return false;
  }
  
  @Override
  public List<String> completions(CommandSender sender) {
    switch (style) {
      case PLACEHOLDER:
        return Collections.singletonList(placeholder);
      case DIAPASON:
        return Collections.singletonList(min + " -> " + max);
    }
    return Collections.emptyList();
  }
  
  @Override
  public String argumentName() {
    return placeholder;
  }
  
  @Override
  protected String hint() {
    StringBuilder builder = new StringBuilder();
    
    boolean minFlag = min != Double.MIN_VALUE;
    if (minFlag) {
      builder.append("От ").append(min);
    }
    if (max != Double.MAX_VALUE) builder.append(minFlag ? " до " : "До ").append(max);
    
    return builder.toString();
  }
}
