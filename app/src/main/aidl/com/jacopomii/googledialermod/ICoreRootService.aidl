package com.jacopomii.googledialermod;

interface ICoreRootService {
    IBinder getFileSystemService();

    Map phenotypeDBGetBooleanFlags(in String packageName);

    boolean phenotypeDBAreAllBooleanFlagsTrue(in String packageName, in String[] flags);
    boolean phenotypeDBAreAllFlagsOverridden(in String packageName, in String[] flags);
    boolean phenotypeDBAreAllStringFlagsEmpty(in String packageName, in String[] flags);

    void phenotypeDBDeleteAllFlagOverrides();
    void phenotypeDBDeleteAllFlagOverridesByPackageName(in String packageName);
    void phenotypeDBDeleteFlagOverrides(in String packageName, in String[] flags);

    void phenotypeDBUpdateBooleanFlag(in String packageName, in String flag, in boolean value);
    void phenotypeDBUpdateExtensionFlag(in String packageName, in String flag, in byte[] value);
    void phenotypeDBUpdateStringFlag(in String packageName, in String flag, in String value);
}