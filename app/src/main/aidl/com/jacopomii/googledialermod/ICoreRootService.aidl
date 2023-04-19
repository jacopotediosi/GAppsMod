package com.jacopomii.googledialermod;

interface ICoreRootService {
    IBinder getFileSystemService();

    /**
     * Query the Phenotype DB, selecting all the "overridden" or "normal" boolean flags for a given {@code packageName}.
     *
     * @param packageName the package name whose flags are to be returned.
     * @return a {@code HashMap} in the format "flag name" => "flag value, overrided or not".
     */
    Map phenotypeDBGetBooleanFlagsOrOverridden(in String packageName);

    /**
     * Query the Phenotype DB to find out if all given {@code flags} are overridden for a given {@code packageName}.
     *
     * @param packageName the package name for which to check.
     * @param flags the flags to check.
     * @return {@code true} if all given {@code flags are overridden} for the given {@code packageName}; {@code false} otherwise.
     */
    boolean phenotypeDBAreAllFlagsOverridden(in String packageName, in List<String> flags);

    /**
     * Remove all flag overrides from the Phenotype DB by truncating the FlagOverrides table.
     * It also clears from the filesystem the Phenotype cache of all applications for which a suggested mod exists.
     */
    void phenotypeDBDeleteAllFlagOverrides();

    /**
     * Delete all flag overrides from the Phenotype DB for a given {@code packageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code packageName}.
     *
     * @param packageName the package name for which to delete the flag overrides.
     */
    void phenotypeDBDeleteAllFlagOverridesByPackageName(in String packageName);

    /**
     * Delete a given list of flag overrides from the Phenotype DB for a given {@code packageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code packageName}.
     *
     * @param packageName the package name for which to delete the flag overrides.
     * @param flags the list of flags to delete.
     */
    void phenotypeDBDeleteFlagOverrides(in String packageName, in List<String> flags);

    /**
     * Override the value of a boolean flag in the Phenotype DB for a given {@code packageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code packageName}.
     *
     * @param packageName the package name for which to override the flag.
     * @param flag the name of the flag to override.
     * @param value the value to override the flag with.
     */
    void phenotypeDBOverrideBooleanFlag(in String packageName, in String flag, in boolean value);

    /**
     * Override the value of an extension flag in the Phenotype DB for a given {@code packageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code packageName}.
     *
     * @param packageName the package name for which to override the flag.
     * @param flag the name of the flag to override.
     * @param value the value to override the flag with.
     */
    void phenotypeDBOverrideExtensionFlag(in String packageName, in String flag, in byte[] value);

    /**
     * Override the value of a string flag in the Phenotype DB for a given {@code packageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code packageName}.
     *
     * @param packageName the package name for which to override the flag.
     * @param flag the name of the flag to override.
     * @param value the value to override the flag with.
     */
    void phenotypeDBOverrideStringFlag(in String packageName, in String flag, in String value);
}