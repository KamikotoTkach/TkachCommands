package ru.cwcode.commands.arguments;

import ru.cwcode.commands.Argument;
import ru.cwcode.commands.api.Sender;

import java.util.Collections;
import java.util.List;

public class ExactStringArg extends Argument {
  final String exactString;
  
  public ExactStringArg(String exactString) {
    this.exactString = exactString;
  }
  
  public String getExactString() {
    return exactString;
  }
  
  @Override
  public boolean valid(String raw) {
    return exactString.equalsIgnoreCase(raw);
  }
  
  @Override
  public List<String> completions(Sender sender) {
    return Collections.singletonList(exactString);
  }
  
  @Override
  public String argumentName() {
    return exactString;
  }
}

