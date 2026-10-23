# Remove logs
-assumenosideeffects class co.touchlab.kermit.Logger {
    public *** v(...);
    public *** d(...);
    public *** i(...);
    public *** e(...);
}

# Manifest entry points + crash capture must survive R8 untouched.
-keep class com.kgurgul.cpuinfo.CpuInfoApp { *; }
-keep class com.kgurgul.cpuinfo.HostActivity { *; }
-keep class com.kgurgul.cpuinfo.CrashActivity { *; }
-keep class com.kgurgul.cpuinfo.CrashLog { *; }
