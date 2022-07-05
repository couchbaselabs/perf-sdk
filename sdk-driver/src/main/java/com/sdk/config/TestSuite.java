package com.sdk.config;

import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.grpc.sdk.protocol.Collection;
import com.couchbase.grpc.sdk.protocol.DocLocationPool;
import com.couchbase.grpc.sdk.protocol.DocLocationSpecific;
import com.couchbase.grpc.sdk.protocol.DocLocationUuid;
import com.couchbase.grpc.sdk.protocol.PoolSelectionStategyRandom;
import com.couchbase.grpc.sdk.protocol.PoolSelectionStrategyCounter;
import com.couchbase.grpc.sdk.protocol.RandomDistribution;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sdk.constants.Defaults;

import java.util.List;
import java.util.Optional;

public record TestSuite(Implementation impl, Variables variables, Connections connections, List<Run> runs) {
//    Duration runtimeAsDuration() {
//        var trimmed = runtime.trim();
//        char suffix = trimmed.charAt(trimmed.length() - 1);
//        var rawNum = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
//        return switch (suffix) {
//            case 's' -> Duration.ofSeconds(rawNum);
//            case 'm' -> Duration.ofMinutes(rawNum);
//            case 'h' -> Duration.ofHours(rawNum);
//            default -> throw new IllegalArgumentException("Could not handle runtime " + runtime);
//        };
//    }
//
//    int runtimeAsInt(){
//        var trimmed = runtime.trim();
//        char suffix = trimmed.charAt(trimmed.length() - 1);
//        var rawNum = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
//        return switch (suffix) {
//            case 's' -> rawNum;
//            case 'm' -> rawNum * Defaults.secPerMin;
//            case 'h' -> rawNum * Defaults.secPerHour;
//            default -> throw new IllegalArgumentException("Could not handle runtime " + runtime);
//        };
//    }

    record Implementation(String language, String version) {
    }

    public record Variables(List<PredefinedVariable> predefined, List<CustomVariable> custom) {
        public Integer getCustomVarAsInt(String varName) {
            if (varName.startsWith("$")) {
                String varNameReal = varName.substring(1); // "pool_size"
                var customVar = custom.stream()
                        .filter(v -> v.name.equals(varNameReal))
                        .findFirst();

                if (customVar.isEmpty()) {
                    throw new IllegalArgumentException("Cannot find custom variable " + varName);
                }

                return (Integer) customVar.get().value();
            }

            return Integer.parseInt(varName);
        }

        public Integer horizontalScaling() {
            return (Integer) predefinedVar(PredefinedVariable.PredefinedVariableName.HORIZONTAL_SCALING);
        }

        public record PredefinedVariable(PredefinedVariableName name, Object value) {
            public enum PredefinedVariableName {
                @JsonProperty("horizontal_scaling") HORIZONTAL_SCALING,
            }
        }

        private Object predefinedVar(PredefinedVariable.PredefinedVariableName name) {
            return predefined.stream()
                    .filter(v -> v.name == name)
                    .findFirst()
                    .map(v -> v.value)
                    .orElseThrow(() -> new IllegalArgumentException("Predefined variable " + name + " not found"));
        }

        public record CustomVariable(String name, Object value) {
        }
    }

    public record Connections(Cluster cluster, PerformerConn performer, Database database) {
        public record Cluster(String hostname, String hostname_docker, String username, String password, String type) {
        }

        public record PerformerConn(String hostname, String hostname_docker, int port) {
        }

        public record Database(String hostname, String hostname_docker, int port, String username, String password,
                        String database) {
        }
    }

    public record DocLocation(Method method, String id, String idPreface, String poolSize,
                       PoolSelectionStrategy poolSelectionStrategy) {
        enum Method {
            @JsonProperty("specific") SPECIFIC,
            @JsonProperty("uuid") UUID,
            @JsonProperty("pool") POOL
        }

        enum PoolSelectionStrategy {
            @JsonProperty("random_uniform") RANDOM_UNIFORM,
            @JsonProperty("counter") COUNTER
        }

        public Optional<Long> poolSize(Variables variables) {
            if (method != Method.POOL) {
                return Optional.empty();
            }

            return Optional.of((long) variables.getCustomVarAsInt(poolSize));
        }

        public com.couchbase.grpc.sdk.protocol.DocLocation convert(Variables variables) {
            var out = com.couchbase.grpc.sdk.protocol.DocLocation.newBuilder();
            var collection = Collection.newBuilder()
                    .setBucket(Defaults.DEFAULT_BUCKET)
                    .setScope(CollectionIdentifier.DEFAULT_SCOPE)
                    .setCollection(CollectionIdentifier.DEFAULT_SCOPE)
                    .build();


            switch (method) {
                case SPECIFIC -> {
                    if (id == null) {
                        throw new IllegalArgumentException("Expect id to be set on specific doc location");
                    }

                    out.setSpecific(DocLocationSpecific.newBuilder()
                            .setCollection(collection)
                            .setId(id));
                }

                case UUID -> out.setUuid(DocLocationUuid.newBuilder()
                        .setCollection(collection)
                        .build());

                case POOL -> {
                    var idPrefaceFinal = (idPreface == null) ? Defaults.KEY_PREFACE : idPreface;
                    long poolSizeFinal = variables.getCustomVarAsInt(poolSize);

                    var builder = DocLocationPool.newBuilder()
                            .setCollection(collection)
                            .setIdPreface(idPrefaceFinal)
                            .setPoolSize(poolSizeFinal);

                    switch (poolSelectionStrategy) {
                        case RANDOM_UNIFORM -> builder.setUniform(PoolSelectionStategyRandom.newBuilder()
                                .setDistribution(RandomDistribution.RANDOM_DISTRIBUTION_UNIFORM));
                        case COUNTER -> builder.setCounter(PoolSelectionStrategyCounter.newBuilder());
                        default ->
                                throw new IllegalArgumentException("Unknown pool selection " + poolSelectionStrategy);
                    }

                    out.setPool(builder);
                }
            }

            return out.build();
        }
    }

    public record Run(String uuid, String description, List<Operation> operations) {
        public record Operation(Op op, String count, DocLocation docLocation) {
            public enum Op {
                @JsonProperty("insert") INSERT,
                @JsonProperty("get") GET,
                @JsonProperty("remove") REMOVE,
                @JsonProperty("replace") REPLACE
            }
        }
    }
}
