package info.repy.tools.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
    public static <K,V> Map<K, V> toSingleMap(Map<K, V[]> map) {
        return map.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> e.getValue()[0]
        ));
    }

    public static <K,V> Map<K, V>[] toListMap(List<Map<K, V>> list) {
        Map<K, V>[] arr = (Map<K, V>[])new Map[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
