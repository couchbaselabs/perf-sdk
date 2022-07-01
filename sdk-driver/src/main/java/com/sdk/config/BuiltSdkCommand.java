package com.sdk.config;

import com.sdk.config.Op;

import java.util.List;

public record BuiltSdkCommand(List<Op> sdkCommand, String description) {
}
