package others;

import org.yaml.snakeyaml.Yaml;

import java.util.List;
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
        Map<String, SystemConfig> result = yaml.load(yamlStream);
        for (var entry: result.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }

    static class Config {
        private SystemConfig autoCache;
        private Service service;
    }

    static class SystemConfig {
        List<Cache> Caches;

        public List<Cache> getCaches() {
            return Caches;
        }

        public void setCaches(List<Cache> caches) {
            Caches = caches;
        }

        class Cache {
            private String Name;
            private String ServiceId;
            private boolean Eternal;
            private int TimeToLive;
            private int TimeToIdle;
            private String EvictionPolicy;
            private String MaxElement;

            public String getName() {
                return Name;
            }

            public void setName(String name) {
                Name = name;
            }

            public String getServiceId() {
                return ServiceId;
            }

            public void setServiceId(String serviceId) {
                ServiceId = serviceId;
            }

            public boolean isEternal() {
                return Eternal;
            }

            public void setEternal(boolean eternal) {
                Eternal = eternal;
            }

            public int getTimeToLive() {
                return TimeToLive;
            }

            public void setTimeToLive(int timeToLive) {
                TimeToLive = timeToLive;
            }

            public int getTimeToIdle() {
                return TimeToIdle;
            }

            public void setTimeToIdle(int timeToIdle) {
                TimeToIdle = timeToIdle;
            }

            public String getEvictionPolicy() {
                return EvictionPolicy;
            }

            public void setEvictionPolicy(String evictionPolicy) {
                EvictionPolicy = evictionPolicy;
            }

            public String getMaxElement() {
                return MaxElement;
            }

            public void setMaxElement(String maxElement) {
                MaxElement = maxElement;
            }
        }
    }
    static class Service {
        private String forwardedHost;
        private Publish publish;

        public String getForwardedHost() {
            return forwardedHost;
        }

        public void setForwardedHost(String forwardedHost) {
            this.forwardedHost = forwardedHost;
        }

        public Publish getPublish() {
            return publish;
        }

        public void setPublish(Publish publish) {
            this.publish = publish;
        }

        static class Publish {
            private boolean all;
            private List<Map<String, String>> include;

            public boolean isAll() {
                return all;
            }

            public void setAll(boolean all) {
                this.all = all;
            }

            public List<Map<String, String>> getInclude() {
                return include;
            }

            public void setInclude(List<Map<String, String>> include) {
                this.include = include;
            }
        }
    }
}
