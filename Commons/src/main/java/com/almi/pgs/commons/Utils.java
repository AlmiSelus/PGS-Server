package com.almi.pgs.commons;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * Created by Almi on 2016-12-10.
 */
public class Utils {
    public static Optional<String> getArgFor(String[] args, String arg) {
        OptionalInt valueForArgument = IntStream.range(0, args.length).filter(i->args[i].equals(arg)).findFirst();
        if(valueForArgument.isPresent()) {
            return Optional.of(args[valueForArgument.getAsInt()+1]);
        }
        return Optional.empty();
    }
}
