package com.jacopomii.googledialermod;

interface ICoreRootService {
    IBinder getFileSystemService();

    /**
    * Query the Phenotype DB to get a list of all package names that have at least one Flag set.
    *
    * @return a {@code HashMap} in the format "Phenotype package name" => "Android package name".
    */
    Map phenotypeDBGetAllPackageNames();

    /**
     * Query the Phenotype DB to get the android package name corresponding to a given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype package name for which the corresponding Android package name is to be returned.
     * @return the Android package name corresponding to the specified {@code phenotypePackageName}. An empty string if the phenotypePackageName couldn't be found.
    */
    String phenotypeDBGetAndroidPackageNameByPhenotypePackageName(in String phenotypePackageName);

    /**
     * Query the Phenotype DB, selecting all the "overridden" or "normal" boolean flags for a given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name whose flags are to be returned.
     * @return a {@code HashMap} in the format "flag name" => "flag value, overrided or not".
     */
    Map phenotypeDBGetBooleanFlagsOrOverridden(in String phenotypePackageName);

    /**
     * Query the Phenotype DB to find out if all given {@code flags} are overridden for a given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to check.
     * @param flags the flags to check.
     * @return {@code true} if all given {@code flags are overridden} for the given {@code phenotypePackageName}; {@code false} otherwise.
     */
    boolean phenotypeDBAreAllFlagsOverridden(in String phenotypePackageName, in List<String> flags);

    /**
     * Remove all flag overrides from the Phenotype DB by truncating the FlagOverrides table.
     * It also clears from the filesystem the Phenotype cache of all applications for which a suggested mod exists.
     */
    void phenotypeDBDeleteAllFlagOverrides();

    /**
     * Delete all flag overrides from the Phenotype DB for a given {@code phenotypePackageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to delete the flag overrides.
     */
    void phenotypeDBDeleteAllFlagOverridesByPhenotypePackageName(in String phenotypePackageName);

    /**
     * Delete a given list of flag overrides from the Phenotype DB for a given {@code phenotypePackageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to delete the flag overrides.
     * @param flags the list of flags to delete.
     */
    void phenotypeDBDeleteFlagOverrides(in String phenotypePackageName, in List<String> flags);

    /**
     * Override the value of a boolean flag in the Phenotype DB for a given {@code phenotypePackageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to override the flag.
     * @param flag the name of the flag to override.
     * @param value the value to override the flag with.
     */
    void phenotypeDBOverrideBooleanFlag(in String phenotypePackageName, in String flag, in boolean value);

    /**
     * Override the value of an extension flag in the Phenotype DB for a given {@code phenotypePackageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to override the flag.
     * @param flag the name of the flag to override.
     * @param value the value to override the flag with.
     */
    void phenotypeDBOverrideExtensionFlag(in String phenotypePackageName, in String flag, in byte[] value);

    /**
     * Override the value of a string flag in the Phenotype DB for a given {@code phenotypePackageName}.
     * It also clears from the filesystem the Phenotype cache of the application corresponding to the given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to override the flag.
     * @param flag the name of the flag to override.
     * @param value the value to override the flag with.
     */
    void phenotypeDBOverrideStringFlag(in String phenotypePackageName, in String flag, in String value);
}