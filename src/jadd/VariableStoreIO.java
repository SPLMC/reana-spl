package jadd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VariableStoreIO {
    
    public static List<String> readVariableNames(String tableFileName) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(tableFileName))) {
            List<List<Object>> tokens = stream.map(line -> parseLine(line)).collect(Collectors.toList());
            List<String> variableNames = tokens.stream().map(list -> (String) list.get(1)).collect(Collectors.toList());
            return variableNames;
        } catch (IOException e) {
            throw e;
        }
    }

    private static List<Object> parseLine(String line) {
        String[] split = line.split("\\s+");
        if (split.length != 3) {
            return null;
        }

        Short index = Short.parseShort(split[0]);
        String variableName = split[1];
        String fileName = split[2];

        List<Object> tokens = new ArrayList<Object>();
        tokens.add(0, index);
        tokens.add(1, variableName);
        tokens.add(2, fileName);

        return tokens;
    }

}
