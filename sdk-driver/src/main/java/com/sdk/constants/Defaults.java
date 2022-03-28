package com.sdk.constants;

public class Defaults {
    public final static String DEFAULT_BUCKET = "default";
    public final static String DEFAULT_SCOPE = "_default";
    public final static String DEFAULT_COLLECTION = "_default";
    public final static String DOCPOOL_COLLECTION = "docPool";
    public final static String KEY_PREFACE = "doc_";

    // during tests the java performer seemed to be able to do about 450 inserts a second so this seemed
    // like a good arbitrary number to start off on, if remove tests keep running out of docs or the warmup takes too long this will have to be changed.
    public final static int docCreateMultiplier = 750;
    public final static int secPerMin = 60;
    public final static int secPerHour = 3600;
}