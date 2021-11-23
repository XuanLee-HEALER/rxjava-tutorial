package others;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.List;

/**
 * 解析YML文件
 */
public class YAMLParser {

    public static void main(String[] args) {
        var yaml = new Yaml(new Constructor(Config.class));
        var yamlStream = YAMLParser.class
                .getClassLoader()
                .getResourceAsStream("my.yaml");
        SystemConfig config = yaml.load(yamlStream);
        System.out.println(config);

//        var yaml = new Yaml();
//        var yamlStream = YAMLParser.class
//                .getClassLoader()
//                .getResourceAsStream("test.yaml");
//        Map<String, Object> result = yaml.load(yamlStream);
//        System.out.println(result);
    }

}

class Config {
    public SystemConfig autoCache;
    public Service service;
    public SystemItem system;
    public Pty pty;
    public ResourcesItem resources;
    public AccountItem account;

}

class Service {
    public String forwardedHost;
    public Publish publish;
}

class Publish {
    public boolean all;
    public List<String> include;
}

class SystemConfig {
    public List<Cache> Caches;
}

class Cache {
    public String Name;
    public String ServiceId;
    public boolean Eternal;
    public int TimeToLive;
    public int TimeToIdle;
    public String EvictionPolicy;
    public String MaxElement;
}

class SystemItem {
    public boolean debugMode;
}

/**
 * 调试模式
 */
class Pty {
    public boolean debug;
}

class ResourcesItem {
    public String base;
    public String custom;
}

class AccountItem {
    public boolean autoCreateUser;
    public String defaultRoleId;
    public String defaultGroupId;
    public String phoneCodeLimit;
    public Kaptcha kaptcha;
}

class Kaptcha {
    public boolean enable;
    public int length;
    public String complexity;
}