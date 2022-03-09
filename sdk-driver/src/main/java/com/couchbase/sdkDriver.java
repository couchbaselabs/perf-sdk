package com.couchbase;

//import com.couchbase.grpc.protocol.TransactionAttemptRequest;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;
import com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest;
import com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse;
import com.couchbase.sdk.util.Performer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.couchbase.grpc.sdk.protocol.Durability;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

record PerfConfig(Variables variables, Connections connections, Map<String, Object> db, List<Workload> runs) {
    record Variables(String runtime) {
        Duration runtimeAsDuration() {
            var trimmed = runtime.trim();
            char suffix = trimmed.charAt(trimmed.length() - 1);
            var rawNum = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
            return switch (suffix) {
                case 's' -> Duration.ofSeconds(rawNum);
                case 'm' -> Duration.ofMinutes(rawNum);
                case 'h' -> Duration.ofHours(rawNum);
                default -> throw new IllegalArgumentException("Could not handle runtime " + runtime);
            };
        }
    }

    record Connections(Cluster cluster, Database database, Performer performer) {
        record Database(String hostname, int port, String username, String password, String database) {
        }

        record Cluster(String hostname, String username, String password) {
        }

        record Performer(String hostname, int port) {
        }
    }

    record Workload(String uuid, String description, Variables variables, SdkOp sdkOp) {
        record Variables(List<PredefinedVariable> predefined, List<CustomVariable> custom) {
            Integer getCustomVarAsInt(String varName) {
                if (varName.startsWith("$")) {
                    return getCustomVarAsInt(varName.substring(1));
                }

                var match = custom.stream().filter(v -> v.name.equals(varName)).findFirst();
                return match
                        .map(v -> (Integer) v.value)
                        .orElseThrow(() -> new IllegalArgumentException("Custom variable " + varName + " not found"));
            }

            Integer horizontalScaling() {
                return (Integer) predefinedVar(PredefinedVariable.PredefinedVariableName.HORIZONTAL_SCALING);
            }

            Integer docPoolSize() {
                return (Integer) predefinedVar(PredefinedVariable.PredefinedVariableName.DOC_POOL_SIZE);
            }

            Durability durability() {
                var raw = (String) predefinedVar(PredefinedVariable.PredefinedVariableName.DURABILITY);
                return Durability.valueOf(raw);
            }

            private Object predefinedVar(PredefinedVariable.PredefinedVariableName name) {
                return predefined.stream()
                        .filter(v -> v.name == name)
                        .findFirst()
                        .map(v -> v.value)
                        .orElseThrow(() -> new IllegalArgumentException("Predefined variable " + name + " not found"));
            }
        }

        record PredefinedVariable(PredefinedVariableName name, Object value) {
            enum PredefinedVariableName {
                @JsonProperty("horizontal_scaling") HORIZONTAL_SCALING,
                @JsonProperty("doc_pool_size") DOC_POOL_SIZE,
                @JsonProperty("durability") DURABILITY
            }
        }

        record CustomVariable(String name, Object value) {
        }

        record SdkOp(List<Operation> operations) {
            record Operation(Op op, Doc doc, Operation repeat, String count) {
                enum Op {
                    INSERT,
                    REPLACE,
                    REMOVE
                }
            }

            record Doc(From from, Dist dist) {
                enum From {
                    UUID,
                    POOL
                }

                enum Dist {
                    @JsonProperty("uniform") UNIFORM
                }

                @Override
                public String toString() {
                    return "doc{" +
                            "from=" + from.name().toLowerCase() +
                            (dist == null ? "" : ", dist=" + dist.name().toLowerCase()) +
                            '}';
                }
            }
        }
    }
}

interface Op {
    void applyTo(TransactionAttemptRequest.Builder builder);
}

record BuiltSdkOp(List<Op> sdkOp, String description) {
}

public class SdkDriver {

    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    private final static ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //private static final Logger logger = LogUtil.getLogger(PerfRunnerTest.class);

    public static void main(String[] args)  throws SQLException, IOException, InterruptedException {
        if (args.length != 1) {
            //logger.info("Must provide config.yaml");
            System.exit(-1);
        }
    }

    static PerfConfig readPerfConfig(String configFilename) {
        try {
            return yamlMapper.readValue(new File(configFilename), PerfConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void run(String configFilename) throws IOException, SQLException, InterruptedException {
        var perfConfig = readPerfConfig(configFilename);

        var dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                perfConfig.connections().database().hostname(),
                perfConfig.connections().database().port(),
                perfConfig.connections().database().database());

        var props = new Properties();
        props.setProperty("user", perfConfig.connections().database().username());
        props.setProperty("password", perfConfig.connections().database().password());

        //logger.info("Connecting to database " + url);

        try (var conn = DriverManager.getConnection(dbUrl, props)) {

            // Make sure that the timescaledb database is created

            CreateConnectionRequest createConnection =
                    CreateConnectionRequest.newBuilder()
                            .setClusterHostname(perfConfig.connections().cluster().hostname())
                            .setBucketName("default")
                            .setClusterUsername(perfConfig.connections().cluster().username())
                            .setClusterPassword(perfConfig.connections().cluster().password())
                            .setUseAsDefaultConnection(true)
                            .build();

            //logger.info("Connecting to performer on {}:{}", perfConfig.connections().performer().hostname(), perfConfig.connections().performer().port());

            var performer = new Performer(0,
                    perfConfig.connections().performer().hostname(),
                    perfConfig.connections().performer().port(),
                    createConnection);

            SdkFactoryCreateResponse factory = performer.stubBlock()
                    .sdkFactoryCreate(SdkFactoryCreateRequest.newBuilder()
                            .setCleanupLostAttempts(true)
                            .setCleanupClientAttempts(true)
                            .setCleanupWindowMillis(60000)
                            .setExpirationMillis(15000)
                            .setDurability(Durability.MAJORITY)
                            .build());

            for (var workload : perfConfig.runs()) {
                //logger.info("Running workload " + workload);

                BuiltTransaction txn = createTransaction(workload);


            }

        }
    }
}
