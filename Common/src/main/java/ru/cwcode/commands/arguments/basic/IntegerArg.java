package ru.cwcode.commands.arguments.basic;

import ru.cwcode.commands.Argument;
import ru.cwcode.commands.CompletionStyle;
import ru.cwcode.commands.api.Sender;
import ru.cwcode.cwutils.numbers.NumbersUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.cwcode.commands.api.CommandsAPI.l10n;

public class IntegerArg extends Argument {
  int min = Integer.MIN_VALUE;
  int max = Integer.MAX_VALUE;
  CompletionStyle style = CompletionStyle.PLACEHOLDER;
  String placeholder = l10n.get("argument.integer.placeholder");
  
  public IntegerArg() {
  }
  
  public IntegerArg(String placeholder) {
    this.placeholder = placeholder;
  }
  
  public IntegerArg setMin(int min) {
    this.min = min;
    return this;
  }
  
  public IntegerArg setMax(int max) {
    this.max = max;
    return this;
  }
  
  public IntegerArg setStyle(CompletionStyle style) {
    this.style = style;
    return this;
  }
  
  public IntegerArg setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
    return this;
  }
  
  @Override
  public boolean valid(String raw) {
    try {
      if (!NumbersUtils.isInteger(raw)) return false;
      
      int parsed = Integer.parseInt(raw);
      return parsed >= min && parsed < max;
    } catch (NumberFormatException ignored) {
    }
    return false;
  }
  
  @Override
  public List<String> completions(Sender sender) {
    switch (style) {
      case PLACEHOLDER:
        return Collections.singletonList(placeholder);
      case DIAPASON:
        return Collections.singletonList(min + " -> " + max);
      case LIST:
        //todo: add cache
        return IntStream.range(min, max).limit(1000).mapToObj(Integer::toString).collect(Collectors.toList());
      default:
        return Collections.emptyList();
    }
  }
  
  @Override
  public String argumentName() {
    return placeholder;
  }
  
  @Override
  public Object map() {
    return toInt();
  }
  
  @Override
  protected String hint() {
    boolean minFlag = min > Integer.MIN_VALUE;
    boolean maxFlag = max < Integer.MAX_VALUE;
    
    if (minFlag && maxFlag) return l10n.get("argument.integer.hint.minmax", min, max);
    if (!minFlag && maxFlag) return l10n.get("argument.integer.hint.max", max);
    if (minFlag && !maxFlag) return l10n.get("argument.integer.hint.min", min);
    
    return placeholder;
  }
}
