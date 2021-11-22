package others;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * 解析YML文件
 */
public class YAMLParser {

    public static void main(String[] args) {
        var yaml = new Yaml();
        var yamlStream = YAMLParser.class
                .getClassLoader()
                .getResourceAsStream("my.yaml");
        Map<String, Object> result = yaml.load(yamlStream);
        for (var entry: result.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }
}
